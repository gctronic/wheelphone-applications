/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "BooksViewController.h"
#import "BooksManager.h"
#import "BookWebDetailViewController.h"
#import "TargetOverlayView.h"

#import <QCAR/QCAR.h>
#import <QCAR/TrackerManager.h>
#import <QCAR/ImageTracker.h>
#import <QCAR/ImageTarget.h>
#import <QCAR/DataSet.h>
#import <QCAR/CameraDevice.h>
#import <QCAR/TargetFinder.h>
#import <QCAR/TargetSearchResult.h>

// ----------------------------------------------------------------------------
// Credentials for authenticating with the Books service
// These are read-only access keys for accessing the image database
// specific to this sample application - the keys should be replaced
// by your own access keys. You should be very careful how you share
// your credentials, especially with untrusted third parties, and should
// take the appropriate steps to protect them within your application code
// ----------------------------------------------------------------------------
static const char* const kAccessKey = "65c7d92e768c3aa5ec8f43c74dc95774d2577973";
static const char* const kSecretKey = "541c65156986fce31e650c976826f9b86cd7346d";



@interface BooksViewController ()

@end

@implementation BooksViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        vapp = [[SampleApplicationSession alloc] initWithDelegate:self];
        
        // Custom initialization
        self.title = @"Cloud Recognition";
        // Create the EAGLView with the screen dimensions
        CGRect screenBounds = [[UIScreen mainScreen] bounds];
        viewFrame = screenBounds;
        
        
        arViewRect.size = [[UIScreen mainScreen] bounds].size;
        arViewRect.origin.x = arViewRect.origin.y = 0;
        

        
        // If this device has a retina display, scale the view bounds that will
        // be passed to QCAR; this allows it to calculate the size and position of
        // the viewport correctly when rendering the video background
        if (YES == vapp.isRetinaDisplay) {
            viewFrame.size.width *= 2.0;
            viewFrame.size.height *= 2.0;
        }
        
        scanningMode = YES;
        isVisualSearchOn = NO;
        lastScannedBook = nil;
        lastTargetIDScanned = nil;
        isShowingAnAlertView = NO;
        
        tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
        tapGestureRecognizer.delegate = self;
    }
    return self;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch {
    return ([touch.view.superview isKindOfClass:[BooksEAGLView class]] || [touch.view.superview isKindOfClass:[TargetOverlayView class]]);
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:backgroundObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:activeObserver];
    [tapGestureRecognizer release];
    
    [vapp release];
    [eaglView release];
    
    if (lastTargetIDScanned != nil) {
        [lastTargetIDScanned release];
        lastTargetIDScanned = nil;
    }
    if (lastScannedBook != nil) {
        [lastScannedBook release];
        lastScannedBook = nil;
    }

    
    [super dealloc];
}

- (NSString *) lastTargetIDScanned {
    return lastTargetIDScanned;
}

- (void) setLastTargetIDScanned:(NSString *) targetId {
    if (lastTargetIDScanned != nil) {
        [lastTargetIDScanned release];
        lastTargetIDScanned = nil;
    }
    if (targetId != nil) {
        lastTargetIDScanned = [[NSString stringWithString:targetId ] retain];
    }
}


- (void) setLastScannedBook: (Book *) book {
    if (lastScannedBook != nil) {
        [lastScannedBook release];
        lastScannedBook = nil;
    }
    if (book != nil) {
        lastScannedBook = [book retain];
    }
}


- (BOOL) isVisualSearchOn {
    return isVisualSearchOn;
}

- (void) setVisualSearchOn:(BOOL) isOn {
    isVisualSearchOn = isOn;
}



- (void)loadView
{
    bookOverlayController = [[BooksOverlayViewController alloc]initWithDelegate:self];

    // Create the EAGLView
    eaglView = [[BooksEAGLView alloc] initWithFrame:viewFrame delegate:self appSession:vapp];
    [eaglView addSubview:bookOverlayController.view];
    [self setView:eaglView];
    
    CGRect indicatorBounds;
    
    CGRect mainBounds = [[UIScreen mainScreen] bounds];
    if (self.interfaceOrientation ==UIInterfaceOrientationPortrait || self.interfaceOrientation == UIInterfaceOrientationPortraitUpsideDown) {
        indicatorBounds = CGRectMake(mainBounds.size.width / 2 - 12,
                                        mainBounds.size.height / 2 - 12, 24, 24);
    } else {
        indicatorBounds = CGRectMake(mainBounds.size.height / 2 - 12,
                                     mainBounds.size.width / 2 - 12, 24, 24);
    }
    UIActivityIndicatorView *loadingIndicator = [[[UIActivityIndicatorView alloc]
                                          initWithFrame:indicatorBounds]autorelease];
    
    loadingIndicator.tag  = 1;
    loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhiteLarge;
    [eaglView addSubview:loadingIndicator];
    [loadingIndicator startAnimating];
    
    [vapp initAR:QCAR::GL_20 ARViewBoundsSize:viewFrame.size orientation:self.interfaceOrientation];//UIInterfaceOrientationPortrait];

    backgroundObserver = [[NSNotificationCenter defaultCenter]
                          addObserverForName:UIApplicationWillResignActiveNotification
                          object:nil
                          queue:nil
                          usingBlock:^(NSNotification *note) {
                              NSError * error = nil;
                              if(! [vapp pauseAR:&error]) {
                                  NSLog(@"Error pausing AR:%@", [error description]);
                              }
                          } ];
    
    activeObserver = [[NSNotificationCenter defaultCenter]
                      addObserverForName:UIApplicationDidBecomeActiveNotification
                      object:nil
                      queue:nil
                      usingBlock:^(NSNotification *note) {
                          NSError * error = nil;
                          if(! [vapp resumeAR:&error]) {
                              NSLog(@"Error resuming AR:%@", [error description]);
                          }
                          // on resume, we reset the flash and the associated menu item
                          QCAR::CameraDevice::getInstance().setFlashTorchMode(false);
                          SampleAppMenu * menu = [SampleAppMenu instance];
                          [menu setSelectionValueForCommand:C_FLASH value:false];
                          
                          [self handleRotation:self.interfaceOrientation];
                      } ];
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    [self prepareMenu];

    // Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    [self.view addGestureRecognizer:tapGestureRecognizer];
    
    // last error seen - used to avoid seeing twice the same error in the error dialog box
    lastErrorCode = 99;
}

- (void)viewWillDisappear:(BOOL)animated {
    if (!self.presentedViewController) {
        [vapp stopAR:nil];
        // Be a good OpenGL ES citizen: now that QCAR is paused and the render
        // thread is not executing, inform the root view controller that the
        // EAGLView should finish any OpenGL ES commands
        [eaglView finishOpenGLESCommands];
    }

}

- (void)finishOpenGLESCommands
{
    // Called in response to applicationWillResignActive.  Inform the EAGLView
    [eaglView finishOpenGLESCommands];
}


- (void)freeOpenGLESResources
{
    // Called in response to applicationDidEnterBackground.  Inform the EAGLView
    [eaglView freeOpenGLESResources];
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Support all orientations
    return YES;
}


// Not using iOS6 specific enums in order to compile on iOS5 and lower versions
-(NSUInteger)supportedInterfaceOrientations
{
    return ((1 << UIInterfaceOrientationPortrait) | (1 << UIInterfaceOrientationLandscapeLeft) | (1 << UIInterfaceOrientationLandscapeRight) | (1 << UIInterfaceOrientationPortraitUpsideDown));
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    // make sure we're oriented/sized properly before reappearing/restarting
    [self handleARViewRotation:self.interfaceOrientation];
}

// This is called on iOS 4 devices (when built with SDK 5.1 or 6.0) and iOS 6
// devices (when built with SDK 5.1)
- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation duration:(NSTimeInterval)duration
{
    [self handleRotation:interfaceOrientation];
}

- (void) handleRotation:(UIInterfaceOrientation)interfaceOrientation {
    // ensure overlay size and AR orientation is correct for screen orientation
    [self handleARViewRotation:self.interfaceOrientation];
    [bookOverlayController handleViewRotation:self.interfaceOrientation];
    [vapp changeOrientation:self.interfaceOrientation];
}

- (void) handleARViewRotation:(UIInterfaceOrientation)interfaceOrientation
{
    CGPoint centre, pos;
    NSInteger rot;
    
    // Set the EAGLView's position (its centre) to be the centre of the window, based on orientation
    centre.x = arViewRect.size.width / 2;
    centre.y = arViewRect.size.height / 2;
    
    if (interfaceOrientation == UIInterfaceOrientationPortrait)
    {
        NSLog(@"ARVC: Rotating to Portrait");
        pos = centre;
        rot = 90;
        
        CGRect viewBounds;
        viewBounds.origin.x = 0;
        viewBounds.origin.y = 0;
        viewBounds.size.width = arViewRect.size.width;
        viewBounds.size.height = arViewRect.size.height;
        
        [eaglView setFrame:viewBounds];
    }
    else if (interfaceOrientation == UIInterfaceOrientationPortraitUpsideDown)
    {
        NSLog(@"ARVC: Rotating to Upside Down");
        pos = centre;
        rot = 270;
        
        CGRect viewBounds;
        viewBounds.origin.x = 0;
        viewBounds.origin.y = 0;
        viewBounds.size.width = arViewRect.size.width;
        viewBounds.size.height = arViewRect.size.height;
        
        [eaglView setFrame:viewBounds];
    }
    else if (interfaceOrientation == UIInterfaceOrientationLandscapeLeft)
    {
        NSLog(@"ARVC: Rotating to Landscape Left");
        pos.x = centre.y;
        pos.y = centre.x;
        rot = 180;
        
        CGRect viewBounds;
        viewBounds.origin.x = 0;
        viewBounds.origin.y = 0;
        viewBounds.size.width = arViewRect.size.height;
        viewBounds.size.height = arViewRect.size.width;
        
        [eaglView setFrame:viewBounds];
    }
    else if (interfaceOrientation == UIInterfaceOrientationLandscapeRight)
    {
        NSLog(@"ARVC: Rotating to Landscape Right");
        pos.x = centre.y;
        pos.y = centre.x;
        rot = 0;
        
        CGRect viewBounds;
        viewBounds.origin.x = 0;
        viewBounds.origin.y = 0;
        viewBounds.size.width = arViewRect.size.height;
        viewBounds.size.height = arViewRect.size.width;
        
        [eaglView setFrame:viewBounds];
    }
}


-(void)showUIAlertFromErrorCode:(int)code
{
    
    if (!isShowingAnAlertView)
    {
        if (lastErrorCode == code)
        {
            // we don't want to show twice the same error
            return;
        }
        lastErrorCode = code;
        
        NSString *title = nil;
        NSString *message = nil;
        
        if (code == QCAR::TargetFinder::UPDATE_ERROR_NO_NETWORK_CONNECTION)
        {
            title = @"Network Unavailable";
            message = @"Please check your internet connection and try again.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_REQUEST_TIMEOUT)
        {
            title = @"Request Timeout";
            message = @"The network request has timed out, please check your internet connection and try again.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_SERVICE_NOT_AVAILABLE)
        {
            title = @"Service Unavailable";
            message = @"The cloud recognition service is unavailable, please try again later.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_UPDATE_SDK)
        {
            title = @"Unsupported Version";
            message = @"The application is using an unsupported version of Vuforia.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE)
        {
            title = @"Clock Sync Error";
            message = @"Please update the date and time and try again.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_AUTHORIZATION_FAILED)
        {
            title = @"Authorization Error";
            message = @"The cloud recognition service access keys are incorrect or have expired.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_PROJECT_SUSPENDED)
        {
            title = @"Authorization Error";
            message = @"The cloud recognition service has been suspended.";
        }
        else if (code == QCAR::TargetFinder::UPDATE_ERROR_BAD_FRAME_QUALITY)
        {
            title = @"Poor Camera Image";
            message = @"The camera does not have enough detail, please try again later";
        }
        else
        {
            title = @"Unknown error";
            message = [NSString stringWithFormat:@"An unknown error has occurred (Code %d)", code];
        }
        
        //  Call the UIAlert on the main thread to avoid undesired behaviors
        dispatch_async( dispatch_get_main_queue(), ^{
            if (title && message)
            {
                UIAlertView *anAlertView = [[[UIAlertView alloc] initWithTitle:title
                                                                       message:message
                                                                      delegate:self
                                                             cancelButtonTitle:@"OK"
                                                             otherButtonTitles:nil] autorelease];
                anAlertView.tag = 42;
                [anAlertView show];
                isShowingAnAlertView = YES;
            }
        });
    }
}

#pragma mark - UIAlertViewDelegate

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    // we go back to the about page
    isShowingAnAlertView = NO;
    // Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:NO animated:NO];
    [self.navigationController popViewControllerAnimated:YES];
}



#pragma mark - SampleApplicationControl

// Initialize the application trackers        
- (bool) doInitTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* trackerBase = trackerManager.initTracker(QCAR::ImageTracker::getClassType());
    // Set the visual search credentials:
    QCAR::TargetFinder* targetFinder = static_cast<QCAR::ImageTracker*>(trackerBase)->getTargetFinder();
    if (targetFinder == NULL)
    {
        NSLog(@"Failed to get target finder.");
        return false;
    }
    
    return true;
}

// load the data associated to the trackers
- (bool) doLoadTrackersData {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    if (imageTracker == NULL)
    {
        NSLog(@"Failed to load tracking data set because the ImageTracker has not been initialized.");
        return false;
        
    }
    
    // Initialize visual search:
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    if (targetFinder == NULL)
    {
        NSLog(@"Failed to get target finder.");
        return false;
    }
    
    NSDate *start = [NSDate date];
    
    // Start initialization:
    if (targetFinder->startInit(kAccessKey, kSecretKey))
    {
        targetFinder->waitUntilInitFinished();
        
        NSDate *methodFinish = [NSDate date];
        NSTimeInterval executionTime = [methodFinish timeIntervalSinceDate:start];
        
        NSLog(@"waitUntilInitFinished Execution Time: %f", executionTime);
        
        
    }
    
    int resultCode = targetFinder->getInitState();
    if ( resultCode != QCAR::TargetFinder::INIT_SUCCESS)
    {
        int initErrorCode;
        if(resultCode == QCAR::TargetFinder::INIT_ERROR_NO_NETWORK_CONNECTION)
        {
            initErrorCode = QCAR::TargetFinder::UPDATE_ERROR_NO_NETWORK_CONNECTION;
        }
        else
        {
            initErrorCode = QCAR::TargetFinder::UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
        }
        [self showUIAlertFromErrorCode: initErrorCode];
        return false;
    } else {
        NSLog(@"target finder initialized");
    }
    return true;
}

// start the application trackers
- (bool) doStartTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
                                                                        trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    assert(imageTracker != 0);
    imageTracker->start();
    
    
    
    // Start cloud based recognition if we are in scanning mode:
    if (scanningMode)
    {
        QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
        assert (targetFinder != 0);
        isVisualSearchOn = targetFinder->startRecognition();
    }
    return true;
}

// callback called when the initailization of the AR is done
- (void) onInitARDone:(NSError *)initError {
    UIActivityIndicatorView *loadingIndicator = (UIActivityIndicatorView *)[eaglView viewWithTag:1];
    [loadingIndicator removeFromSuperview];

    if (initError == nil) {
        NSError * error = nil;
        [vapp startAR:QCAR::CameraDevice::CAMERA_BACK error:&error];
        
        // by default, we try to set the continuous auto focus mode
        // and we update menu to reflect the state of continuous auto-focus
        bool isContinuousAutofocus = QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO);
        SampleAppMenu * menu = [SampleAppMenu instance];
        [menu setSelectionValueForCommand:C_AUTOFOCUS value:isContinuousAutofocus];

        //  the camera is initialized, this call will reset the screen configuration
        [self handleRotation:self.interfaceOrientation];
    } else {
        NSLog(@"Error initializing AR:%@", [initError description]);
        if ([initError code] == E_INITIALIZING_QCAR) {
            // for this sample, we hijack an existing error code. A real app would have its own error code for that situation
            [self showUIAlertFromErrorCode: QCAR::TargetFinder::UPDATE_ERROR_NO_NETWORK_CONNECTION];
        }
    }
}

// update from the QCAR loop
- (void) onQCARUpdate: (QCAR::State *) state {
    // Get the tracker manager:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    
    // Get the image tracker:
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    
    // Get the target finder:
    QCAR::TargetFinder* finder = imageTracker->getTargetFinder();
    
    // Check if there are new results available:
    const int statusCode = finder->updateSearchResults();
    if (statusCode < 0)
    {
        // Show a message if we encountered an error:
        NSLog(@"update search result failed:%d", statusCode);
        if (statusCode == QCAR::TargetFinder::UPDATE_ERROR_NO_NETWORK_CONNECTION) {
            [self showUIAlertFromErrorCode:statusCode];
        }
    }
    else if (statusCode == QCAR::TargetFinder::UPDATE_RESULTS_AVAILABLE)
    {
        
        // Iterate through the new results:
        for (int i = 0; i < finder->getResultCount(); ++i)
        {
            const QCAR::TargetSearchResult* result = finder->getResult(i);
            
            // Check if this target is suitable for tracking:
            if (result->getTrackingRating() > 0)
            {
                // Create a new Trackable from the result:
                QCAR::Trackable* newTrackable = finder->enableTracking(*result);
                if (newTrackable != 0)
                {
                    QCAR::ImageTarget* imageTargetTrackable = (QCAR::ImageTarget*)newTrackable;
                    
                    //  Avoid entering on ContentMode when a bad target is found
                    //  (Bad Targets are targets that are exists on the Books database but not on our
                    //  own book database)
                    if (![[BooksManager sharedInstance] isBadTarget:imageTargetTrackable->getUniqueTargetId()])
                    {
                        NSLog(@"Successfully created new trackable '%s' with rating '%d'.",
                              newTrackable->getName(), result->getTrackingRating());
                    }
                }
                else
                {
                    NSLog(@"Failed to create new trackable.");
                }
            }
        }
    }

}

// stop your trackerts
- (bool) doStopTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
                                                                        trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    assert(imageTracker != 0);
    imageTracker->stop();
    
    // Stop cloud based recognition:
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    assert (targetFinder != 0);
    isVisualSearchOn = !targetFinder->stop();
    return true;
}

// unload the data associated to your trackers
- (bool) doUnloadTrackersData {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    
    if (imageTracker == NULL)
    {
        NSLog(@"Failed to unload tracking data set because the ImageTracker has not been initialized.");
        return false;
    }
    
    // Deinitialize visual search:
    QCAR::TargetFinder* finder = imageTracker->getTargetFinder();
    finder->deinit();
    return true;
}

// deinitialize your trackers
- (bool) doDeinitTrackers {
    return true;
}

- (void)handleTap:(UITapGestureRecognizer *)sender
{
    if (sender.state == UIGestureRecognizerStateEnded) {
        // handling code
        CGPoint touchPoint = [sender locationInView:eaglView];
        if ([eaglView isTouchOnTarget:touchPoint] ) {
            if (lastScannedBook)
            {
                //  Show Book WebView Detail
                BookWebDetailViewController *bookWebDetailViewController = [[[BookWebDetailViewController alloc] initWithBook:lastScannedBook] autorelease];
                [self presentModalViewController:bookWebDetailViewController animated:YES];
            }
        }
    }
    [self performSelector:@selector(cameraPerformAutoFocus) withObject:nil afterDelay:.4];
}

- (void)cameraPerformAutoFocus
{
    QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO);
}

- (void) toggleVisualSearch:(BOOL)visualSearchOn
{
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
                                                                        trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    assert(imageTracker != 0);
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    assert (targetFinder != 0);
    if (visualSearchOn == NO)
    {
        targetFinder->startRecognition();
        isVisualSearchOn = YES;
    }
    else
    {
        targetFinder->stop();
        isVisualSearchOn = NO;
    }
}


- (void)setOverlayLayer:(CALayer *)overlayLayer
{
    [eaglView setOverlayLayer:overlayLayer];
}

- (void)enterScanningMode
{
    [eaglView enterScanningMode];
}


#pragma mark - left menu

typedef enum {
    C_AUTOFOCUS,
    C_FLASH
} MENU_CMDE;

- (void) prepareMenu {
    
    SampleAppMenu * menu = [SampleAppMenu prepareWithCommandProtocol:self title:@"Cloud Reco"];
    SampleAppMenuGroup * group;
    
    group = [menu addGroup:@""];
    [group addTextItem:@"About" command:-1];

    group = [menu addGroup:@""];
    [group addSelectionItem:@"Autofocus" command:C_AUTOFOCUS isSelected:true];
    [group addSelectionItem:@"Flash" command:C_FLASH isSelected:false];
}

- (bool) menuProcess:(SampleAppMenu *) menu command:(int) command value:(bool) value{
    bool result = true;

    switch(command) {
        case C_FLASH:
            if (!QCAR::CameraDevice::getInstance().setFlashTorchMode(value)) {
                result = false;
            }
            break;
            
        case C_AUTOFOCUS: {
            int focusMode = value ? QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO : QCAR::CameraDevice::FOCUS_MODE_NORMAL;
            result = QCAR::CameraDevice::getInstance().setFocusMode(focusMode);
        }
            break;
            
        default:
            result = false;
            break;
    }
    return result;
}

@end

