/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <QuartzCore/QuartzCore.h>
#import "CloudRecoViewController.h"
#import <QCAR/QCAR.h>
#import <QCAR/TrackerManager.h>
#import <QCAR/ImageTracker.h>
#import <QCAR/ImageTarget.h>
#import <QCAR/DataSet.h>
#import <QCAR/Trackable.h>
#import <QCAR/TargetFinder.h>
#import <QCAR/TargetSearchResult.h>

// ----------------------------------------------------------------------------
// Credentials for authenticating with the CloudReco service
// These are read-only access keys for accessing the image database
// specific to this sample application - the keys should be replaced
// by your own access keys. You should be very careful how you share
// your credentials, especially with untrusted third parties, and should
// take the appropriate steps to protect them within your application code
// ----------------------------------------------------------------------------
static const char* const kAccessKey = "869a299f9911cd84f189d69fe8d5f79f35304372";
static const char* const kSecretKey = "ad4a7110ad50100b22474f166d7ef4f5b3887a30";

@interface CloudRecoViewController ()

@end

@implementation CloudRecoViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        vapp = [[SampleApplicationSession alloc]initWithDelegate:self];
        // Custom initialization
        self.title = @"Cloud Reco";
        // Create the EAGLView with the screen dimensions
        CGRect screenBounds = [[UIScreen mainScreen] bounds];
        viewFrame = screenBounds;
        
        // If this device has a retina display, scale the view bounds that will
        // be passed to QCAR; this allows it to calculate the size and position of
        // the viewport correctly when rendering the video background
        if (YES == vapp.isRetinaDisplay) {
            viewFrame.size.width *= 2.0;
            viewFrame.size.height *= 2.0;
        }
        
        scanningMode = YES;
        isVisualSearchOn = NO;
        
        // single tap will trigger focus
        tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(autofocus:)];
        
        // handle iOS notifications to properly pause/resume AR
        backgroundObserver = [[NSNotificationCenter defaultCenter]
                              addObserverForName:UIApplicationWillResignActiveNotification
                              object:nil
                              queue:nil
                              usingBlock:^(NSNotification *note) {
                                  NSError * error = nil;
                                  if (![vapp pauseAR:&error]) {
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

                          } ];
        offTargetTrackingEnabled = NO;
        isShowingAnAlertView = NO;
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:backgroundObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:activeObserver];

    [vapp release];
    [eaglView release];
    [super dealloc];
}

- (BOOL) isVisualSearchOn {
    return isVisualSearchOn;
}

- (void) setVisualSearchOn:(BOOL) isOn {
    isVisualSearchOn = isOn;
}

- (void)loadView
{
    // Create the EAGLView
    eaglView = [[CloudRecoEAGLView alloc] initWithFrame:viewFrame  appSession:vapp];
    [self setView:eaglView];
    
    
    CGRect mainBounds = [[UIScreen mainScreen] bounds];
    CGRect indicatorBounds = CGRectMake(mainBounds.size.width / 2 - 12,
                                        mainBounds.size.height / 2 - 12, 24, 24);
    UIActivityIndicatorView *loadingIndicator = [[[UIActivityIndicatorView alloc]
                                                  initWithFrame:indicatorBounds]autorelease];
    
    loadingIndicator.tag  = 1;
    loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhiteLarge;
    [eaglView addSubview:loadingIndicator];
    [loadingIndicator startAnimating];
    
    // initialize the AR session 
    [vapp initAR:QCAR::GL_20 ARViewBoundsSize:viewFrame.size orientation:UIInterfaceOrientationPortrait];
    
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    [self prepareMenu];
    self.navigationController.navigationBar.translucent = YES;

	// Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:YES animated:YES];

    // last error seen - used to avoid seeing twice the same error in the error dialog box
    lastErrorCode = 99;
}

- (void)viewWillDisappear:(BOOL)animated {
    self.navigationController.navigationBar.translucent = NO;
    [vapp stopAR:nil];
    // Be a good OpenGL ES citizen: now that QCAR is paused and the render
    // thread is not executing, inform the root view controller that the
    // EAGLView should finish any OpenGL ES commands
    [eaglView finishOpenGLESCommands];

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
    [[NSNotificationCenter defaultCenter] postNotificationName:@"kMenuDismissViewController" object:nil];
}



#pragma mark - loading animation

- (void) showLoadingAnimation {
    CGRect mainBounds = [[UIScreen mainScreen] bounds];
    CGRect indicatorBounds = CGRectMake(mainBounds.size.width / 2 - 12,
                                        mainBounds.size.height / 2 - 12, 24, 24);
    UIActivityIndicatorView *loadingIndicator = [[[UIActivityIndicatorView alloc]
                                                  initWithFrame:indicatorBounds]autorelease];
    
    loadingIndicator.tag  = 1;
    loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhiteLarge;
    [eaglView addSubview:loadingIndicator];
    [loadingIndicator startAnimating];
}

- (void) hideLoadingAnimation {
    UIActivityIndicatorView *loadingIndicator = (UIActivityIndicatorView *)[eaglView viewWithTag:1];
    [loadingIndicator removeFromSuperview];
}

#pragma mark - SampleApplicationControl

- (bool) doInitTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* trackerBase = trackerManager.initTracker(QCAR::ImageTracker::getClassType());
    // Set the visual search credentials:
    QCAR::TargetFinder* targetFinder = static_cast<QCAR::ImageTracker*>(trackerBase)->getTargetFinder();
    if (targetFinder == NULL)
    {
        NSLog(@"Failed to get target finder.");
        return NO;
    }
    
    NSLog(@"Successfully initialized ImageTracker.");
    return YES;
}

- (bool) doLoadTrackersData {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    if (imageTracker == NULL)
    {
        NSLog(@">doLoadTrackersData>Failed to load tracking data set because the ImageTracker has not been initialized.");
        return NO;
        
    }
    
    // Initialize visual search:
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    if (targetFinder == NULL)
    {
        NSLog(@">doLoadTrackersData>Failed to get target finder.");
        return NO;
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
        NSLog(@">doLoadTrackersData>Failed to initialize target finder.");
        if (resultCode == QCAR::TargetFinder::INIT_ERROR_NO_NETWORK_CONNECTION) {
            NSLog(@"CloudReco error:QCAR::TargetFinder::INIT_ERROR_NO_NETWORK_CONNECTION");
        } else if (resultCode == QCAR::TargetFinder::INIT_ERROR_SERVICE_NOT_AVAILABLE) {
            NSLog(@"CloudReco error:QCAR::TargetFinder::INIT_ERROR_SERVICE_NOT_AVAILABLE");
        } else {
            NSLog(@"CloudReco error:%d", resultCode);
        }
        
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
        return NO;
    } else {
        NSLog(@">doLoadTrackersData>target finder initialized");
    }
    
    return YES;
}

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
    return YES;
}

- (void) onInitARDone:(NSError *)initError {
    // remove loading animation
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
        

    } else {
        NSLog(@"Error initializing AR:%@", [initError description]);
    }
}

- (bool) doStopTrackers {
    // Stop the tracker
    // Stop the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
                                                                        trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    assert(imageTracker != 0);
    imageTracker->stop();
    
    // Stop cloud based recognition:
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    assert (targetFinder != 0);
    isVisualSearchOn = !targetFinder->stop();
    return YES;
}

- (bool) doUnloadTrackersData {
    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    
    if (imageTracker == NULL)
    {
        NSLog(@"Failed to unload tracking data set because the ImageTracker has not been initialized.");
        return NO;
    }
    
    // Deinitialize visual search:
    QCAR::TargetFinder* finder = imageTracker->getTargetFinder();
    finder->deinit();
    return YES;
}

- (bool) doDeinitTrackers {
    return YES;
}

// enable auto-focus mode
- (void)autofocus:(UITapGestureRecognizer *)sender
{
    [self performSelector:@selector(cameraPerformAutoFocus) withObject:nil afterDelay:.4];
}

- (void)cameraPerformAutoFocus
{
    QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO);
}


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
                    //  Avoid entering on ContentMode when a bad target is found
                    //  (Bad Targets are targets that are exists on the CloudReco database but not on our
                    //  own book database)
                    NSLog(@"Successfully created new trackable '%s' with rating '%d'.",
                          newTrackable->getName(), result->getTrackingRating());
                    if (offTargetTrackingEnabled) {
                        newTrackable->startExtendedTracking();
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


- (void) toggleVisualSearch:(BOOL)visualSearchOn
{
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
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

- (void) setOffTargetTracking:(BOOL) isActive {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    assert(imageTracker != 0);
    QCAR::TargetFinder* targetFinder = imageTracker->getTargetFinder();
    int nbTargets = targetFinder->getNumImageTargets();
    for(int idx = 0; idx < nbTargets ; idx++) {
        QCAR::ImageTarget * it = targetFinder->getImageTarget(idx);
        if (it != NULL) {
            if (isActive) {
                it->startExtendedTracking();
            } else {
                it->stopExtendedTracking();
            }
        }
    }
}


#pragma mark - left menu

typedef enum {
    C_EXTENDED_TRACKING,
    C_AUTOFOCUS,
    C_FLASH,
    C_CAMERA_FRONT,
    C_CAMERA_REAR
} I_COMAMND;

- (void) prepareMenu {
    
    SampleAppMenu * menu = [SampleAppMenu prepareWithCommandProtocol:self title:@"Cloud Reco"];
    SampleAppMenuGroup * group;
    
    group = [menu addGroup:@""];
    [group addTextItem:@"Vuforia Samples" command:-1];
    
    group = [menu addGroup:@""];
    [group addSelectionItem:@"Extended Tracking" command:C_EXTENDED_TRACKING isSelected:NO];
    [group addSelectionItem:@"Autofocus" command:C_AUTOFOCUS isSelected:NO];
    [group addSelectionItem:@"Flash" command:C_FLASH isSelected:NO];
    
    group = [menu addSelectionGroup:@"CAMERA"];
    [group addSelectionItem:@"Front" command:C_CAMERA_FRONT isSelected:NO];
    [group addSelectionItem:@"Rear" command:C_CAMERA_REAR isSelected:YES];
}

- (bool) menuProcess:(SampleAppMenu *) menu command:(int) command value:(bool) value{
    bool result = YES;
    NSError * error = nil;

    switch(command) {
        case C_FLASH:
            if (!QCAR::CameraDevice::getInstance().setFlashTorchMode(value)) {
                result = NO;
            }
            break;
            
        case C_EXTENDED_TRACKING:
            offTargetTrackingEnabled = value;
            [self setOffTargetTracking:offTargetTrackingEnabled];
            break;
        
        case C_CAMERA_FRONT:
        case C_CAMERA_REAR: {
            if ([vapp stopCamera:&error]) {
                result = [vapp startAR:(command == C_CAMERA_FRONT) ? QCAR::CameraDevice::CAMERA_FRONT:QCAR::CameraDevice::CAMERA_BACK error:&error];
            } else {
                result = NO;
            }
            if (result) {
                // if the camera switch worked, the flash will be off
                [menu setSelectionValueForCommand:C_FLASH value:false];
            }

        }
            break;
            
        case C_AUTOFOCUS: {
            int focusMode = (YES == value) ? QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO : QCAR::CameraDevice::FOCUS_MODE_NORMAL;
            result = QCAR::CameraDevice::getInstance().setFocusMode(focusMode);
        }
            break;
            
        default:
            result = NO;
            break;
    }
    return result;
}



@end
