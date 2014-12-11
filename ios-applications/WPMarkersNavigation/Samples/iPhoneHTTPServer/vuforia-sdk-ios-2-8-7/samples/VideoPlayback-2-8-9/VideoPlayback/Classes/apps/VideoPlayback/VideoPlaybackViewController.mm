/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "VideoPlaybackViewController.h"
#import <QCAR/QCAR.h>
#import <QCAR/TrackerManager.h>
#import <QCAR/ImageTracker.h>
#import <QCAR/DataSet.h>
#import <QCAR/Trackable.h>
#import <QCAR/CameraDevice.h>

@interface VideoPlaybackViewController ()

@end

@implementation VideoPlaybackViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        vapp = [[SampleApplicationSession alloc] initWithDelegate:self];
        
        // Custom initialization
        self.title = @"Video Playback";
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

- (void)loadView
{
    // Create the EAGLView
    eaglView = [[[VideoPlaybackEAGLView alloc] initWithFrame:viewFrame  rootViewController:self appSession:vapp] autorelease];
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
    
    [vapp initAR:QCAR::GL_20 ARViewBoundsSize:viewFrame.size orientation:UIInterfaceOrientationPortrait];
    
    backgroundObserver = [[NSNotificationCenter defaultCenter]
                          addObserverForName:UIApplicationWillResignActiveNotification
                          object:nil
                          queue:nil
                          usingBlock:^(NSNotification *note) {
                              [eaglView dismissPlayers];
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
                          [eaglView preparePlayers];
                          NSError * error = nil;
                          if(! [vapp resumeAR:&error]) {
                              NSLog(@"Error resuming AR:%@", [error description]);
                          }
                          // on resume, we reset the flash and the associated menu item
                          QCAR::CameraDevice::getInstance().setFlashTorchMode(false);
                          SampleAppMenu * menu = [SampleAppMenu instance];
                          [menu setSelectionValueForCommand:C_FLASH value:false];
                      } ];
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    [eaglView prepare];
    [self prepareMenu];
    
    
    UITapGestureRecognizer *doubleTap = [[[UITapGestureRecognizer alloc] initWithTarget: self action:@selector(handleDoubleTap:)] autorelease];
    doubleTap.numberOfTapsRequired = 2;
    [self.view addGestureRecognizer:doubleTap];
    
    UITapGestureRecognizer *tap = [[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)]autorelease];
    tap.delegate = (id<UIGestureRecognizerDelegate>)self;
    [self.view addGestureRecognizer:tap];
    [tap requireGestureRecognizerToFail:doubleTap];


  // Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}
    
- (void)viewWillDisappear:(BOOL)animated {
    if (!self.presentedViewController) {

        [eaglView dismiss];
        
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

// double tap handler
- (void)handleDoubleTap:(UITapGestureRecognizer *)sender {
    CGPoint touchPoint = [sender locationInView:eaglView];
    [eaglView handleDoubleTouchPoint:touchPoint];
    
//    [[NSNotificationCenter defaultCenter] postNotificationName:@"double_tap" object:self];
}

// tap handler
- (void)handleTap:(UITapGestureRecognizer *)sender {
    if (sender.state == UIGestureRecognizerStateEnded) {
        // handling code
        CGPoint touchPoint = [sender locationInView:eaglView];
        [eaglView handleTouchPoint:touchPoint];
    }
}

- (void) dimissController:(id) sender {
    self.navigationController.navigationBar.translucent = NO;
    [vapp stopAR:nil];
    // Be a good OpenGL ES citizen: now that QCAR is paused and the render
    // thread is not executing, inform the root view controller that the
    // EAGLView should finish any OpenGL ES commands
    [eaglView finishOpenGLESCommands];
    [self.navigationController popViewControllerAnimated:YES];
}


// Present a view controller using the root view controller (eaglViewController)
- (void)rootViewControllerPresentViewController:(UIViewController*)viewController inContext:(BOOL)currentContext
{
    if (YES == currentContext) {
        // Use UIModalPresentationCurrentContext so the root view is not hidden
        // when presenting another view controller
        [self setModalPresentationStyle:UIModalPresentationCurrentContext];
    }
    else {
        // Use UIModalPresentationFullScreen so the presented view controller
        // covers the screen
        [self setModalPresentationStyle:UIModalPresentationFullScreen];
    }
    
    if ([self respondsToSelector:@selector(presentViewController:animated:completion:)]) {
        // iOS > 4
        [self presentViewController:viewController animated:NO completion:nil];
    }
    else {
        // iOS 4
        [self presentModalViewController:viewController animated:NO];
    }
}

// Dismiss a view controller presented by the root view controller
// (eaglViewController)
- (void)rootViewControllerDismissPresentedViewController
{
    // Dismiss the presented view controller (return to the root view
    // controller)
    if ([self respondsToSelector:@selector(dismissViewControllerAnimated:completion:)]) {
        // iOS > 4
        [self dismissViewControllerAnimated:NO completion:nil];
    }
    else {
        // iOS 4
        [self dismissModalViewControllerAnimated:NO];
    }
}



#pragma mark - SampleApplicationControl

// Initialize the application trackers        
- (bool) doInitTrackers {
    // Initialize the image or marker tracker
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    
    // Image Tracker...
    QCAR::Tracker* trackerBase = trackerManager.initTracker(QCAR::ImageTracker::getClassType());
    if (trackerBase == NULL)
    {
        NSLog(@"Failed to initialize ImageTracker.");
        return false;
    }
    return true;
}

// load the data associated to the trackers
- (bool) doLoadTrackersData {
    return [self loadAndActivateImageTrackerDataSet:@"StonesAndChips.xml"];
}

// start the application trackers
- (bool) doStartTrackers {
    // Set the number of simultaneous trackables to two
    QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, NUM_VIDEO_TARGETS);
    
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* tracker = trackerManager.getTracker(QCAR::ImageTracker::getClassType());
    if(tracker == 0) {
        return false;
    }
    tracker->start();
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

    } else {
        NSLog(@"Error initializing AR:%@", [initError description]);
    }
}

// update from the QCAR loop
- (void) onQCARUpdate: (QCAR::State *) state {
}

// stop your trackerts
- (bool) doStopTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* tracker = trackerManager.getTracker(QCAR::ImageTracker::getClassType());
    
    if (NULL == tracker) {
        NSLog(@"ERROR: failed to get the tracker from the tracker manager");
        return false;
    }
    
    tracker->stop();
    return true;
}

// unload the data associated to your trackers
- (bool) doUnloadTrackersData {
    if (dataSet != NULL) {
        // Get the image tracker:
        QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
        QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
        
        if (imageTracker == NULL)
        {
            NSLog(@"Failed to unload tracking data set because the ImageTracker has not been initialized.");
            return false;
        }
        // Deactivate the data set:
        if (!imageTracker->deactivateDataSet(dataSet))
        {
            NSLog(@"Failed to deactivate data set.");
            return false;
        }
        dataSet = NULL;
    }
    return true;
}

// deinitialize your trackers
- (bool) doDeinitTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    trackerManager.deinitTracker(QCAR::ImageTracker::getClassType());
    return true;
}

- (void)autofocus:(UITapGestureRecognizer *)sender
{
    [self performSelector:@selector(cameraPerformAutoFocus) withObject:nil afterDelay:.4];
}

- (void)cameraPerformAutoFocus
{
    QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO);
}

// Load the image tracker data set
- (BOOL)loadAndActivateImageTrackerDataSet:(NSString*)dataFile
{
    NSLog(@"loadAndActivateImageTrackerDataSet (%@)", dataFile);
    BOOL ret = YES;
    dataSet = NULL;
    
    // Get the QCAR tracker manager image tracker
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    
    if (NULL == imageTracker) {
        NSLog(@"ERROR: failed to get the ImageTracker from the tracker manager");
        ret = NO;
    } else {
        dataSet = imageTracker->createDataSet();
        
        if (NULL != dataSet) {
            NSLog(@"INFO: successfully loaded data set");
            
            // Load the data set from the app's resources location
            if (!dataSet->load([dataFile cStringUsingEncoding:NSASCIIStringEncoding], QCAR::DataSet::STORAGE_APPRESOURCE)) {
                NSLog(@"ERROR: failed to load data set");
                imageTracker->destroyDataSet(dataSet);
                dataSet = NULL;
                ret = NO;
            } else {
                // Activate the data set
                if (imageTracker->activateDataSet(dataSet)) {
                    NSLog(@"INFO: successfully activated data set");
                }
                else {
                    NSLog(@"ERROR: failed to activate data set");
                    ret = NO;
                }
            }
        }
        else {
            NSLog(@"ERROR: failed to create data set");
            ret = NO;
        }
        
    }
    
    return ret;
}

- (BOOL) setExtendedTrackingForDataSet:(QCAR::DataSet *)theDataSet start:(BOOL) start {
    BOOL result = YES;
    for (int tIdx = 0; tIdx < theDataSet->getNumTrackables(); tIdx++) {
        QCAR::Trackable* trackable = theDataSet->getTrackable(tIdx);
        if (start) {
            if (!trackable->startExtendedTracking())
            {
                NSLog(@"Failed to start extended tracking on: %s", trackable->getName());
                result = false;
            }
        } else {
            if (!trackable->stopExtendedTracking())
            {
                NSLog(@"Failed to stop extended tracking on: %s", trackable->getName());
                result = false;
            }
        }
    }
    return result;
}




#pragma mark - left menu

typedef enum {
    C_EXTENDED_TRACKING,
    C_AUTOFOCUS,
    C_FLASH,
    C_CAMERA_FRONT,
    C_CAMERA_REAR
} MENU_COMMAND;

- (void) prepareMenu {
    
    SampleAppMenu * menu = [SampleAppMenu prepareWithCommandProtocol:self title:@"Video Playback"];
    SampleAppMenuGroup * group;
    
    group = [menu addGroup:@""];
    [group addTextItem:@"About" command:-1];

    group = [menu addGroup:@""];
    [group addSelectionItem:@"Extended Tracking" command:C_EXTENDED_TRACKING isSelected:NO];
    [group addSelectionItem:@"Autofocus" command:C_AUTOFOCUS isSelected:true];
    [group addSelectionItem:@"Flash" command:C_FLASH isSelected:false];

    group = [menu addSelectionGroup:@"CAMERA"];
    [group addSelectionItem:@"Front" command:C_CAMERA_FRONT isSelected:false];
    [group addSelectionItem:@"Rear" command:C_CAMERA_REAR isSelected:true];
}

- (bool) menuProcess:(SampleAppMenu *) menu command:(int) command value:(bool) value{
    bool result = true;
    NSError * error = nil;

    switch(command) {
        case C_FLASH:
            if (!QCAR::CameraDevice::getInstance().setFlashTorchMode(value)) {
                result = false;
            }
            break;
            
        case C_EXTENDED_TRACKING:
            result = [self setExtendedTrackingForDataSet:dataSet start:value];
            break;
            
        case C_AUTOFOCUS: {
            int focusMode = value ? QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO : QCAR::CameraDevice::FOCUS_MODE_NORMAL;
            result = QCAR::CameraDevice::getInstance().setFocusMode(focusMode);
        }
            break;
            
        case C_CAMERA_FRONT:
        case C_CAMERA_REAR: {
            if ([vapp stopCamera:&error]) {
                result = [vapp startAR:(command == C_CAMERA_FRONT) ? QCAR::CameraDevice::CAMERA_FRONT:QCAR::CameraDevice::CAMERA_BACK error:&error];
            } else {
                result = false;
            }
        }
            break;
            
        default:
            result = false;
            break;
    }
    return result;
}

@end

