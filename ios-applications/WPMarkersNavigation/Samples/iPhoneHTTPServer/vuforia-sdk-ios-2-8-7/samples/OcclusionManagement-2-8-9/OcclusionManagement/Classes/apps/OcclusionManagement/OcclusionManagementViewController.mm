/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "OcclusionManagementViewController.h"
#import <QCAR/QCAR.h>
#import <QCAR/TrackerManager.h>
#import <QCAR/ImageTracker.h>
#import <QCAR/DataSet.h>
#import <QCAR/CameraDevice.h>

@interface OcclusionManagementViewController ()

@end

@implementation OcclusionManagementViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        vapp = [[SampleApplicationSession alloc] initWithDelegate:self];
        
        // Custom initialization
        self.title = @"OcclusionManagement";
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
        
        tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(autofocus:)];
    }
    return self;
}
- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:backgroundObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:activeObserver];
    [tapGestureRecognizer release];
    
    [vapp release];
    [eaglView release];
    
    [super dealloc];
}

- (void)loadView
{
    // Create the EAGLView
    eaglView = [[OcclusionManagementEAGLView alloc] initWithFrame:viewFrame appSession:vapp];
    [self setView:eaglView];
    
    // as the view is in landscape, we need to adjust the position of the spinner accordingly
    CGRect mainBounds = [[UIScreen mainScreen] bounds];
    CGRect indicatorBounds = CGRectMake(mainBounds.size.height / 2 - 12,
                                        mainBounds.size.width / 2 - 12, 24, 24);
    UIActivityIndicatorView *loadingIndicator = [[[UIActivityIndicatorView alloc]
                                          initWithFrame:indicatorBounds]autorelease];
    
    loadingIndicator.tag  = 1;
    loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyleWhiteLarge;
    [eaglView addSubview:loadingIndicator];
    [loadingIndicator startAnimating];
    
    [vapp initAR:QCAR::GL_20 ARViewBoundsSize:viewFrame.size orientation:UIInterfaceOrientationLandscapeRight];
    
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
                          
                      } ];
    

}


- (void)viewDidLoad
{
    [super viewDidLoad];
    [self handleARViewRotation:self.interfaceOrientation];

    [self prepareMenu];

  // Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    [self.view addGestureRecognizer:tapGestureRecognizer];
    
    NSLog(@"self.navigationController.navigationBarHidden:%d",self.navigationController.navigationBarHidden);
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
    // ensure overlay size and AR orientation is correct for screen orientation
    [self handleARViewRotation:self.interfaceOrientation];
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
    }
    else if (interfaceOrientation == UIInterfaceOrientationPortraitUpsideDown)
    {
        NSLog(@"ARVC: Rotating to Upside Down");
        pos = centre;
        rot = 270;
    }
    else if (interfaceOrientation == UIInterfaceOrientationLandscapeLeft)
    {
        NSLog(@"ARVC: Rotating to Landscape Left");
        pos.x = centre.y;
        pos.y = centre.x;
        rot = 180;
    }
    else if (interfaceOrientation == UIInterfaceOrientationLandscapeRight)
    {
        NSLog(@"ARParent: Rotating to Landscape Right");
        pos.x = centre.y;
        pos.y = centre.x;
        rot = 0;
    } else {
        pos = centre;
        rot = 90;
    }
    
    CGAffineTransform rotate = CGAffineTransformMakeRotation(rot * M_PI  / 180);
    
    [eaglView setOrientationTransform:rotate withLayerPosition:pos];
}


- (void)viewWillDisappear:(BOOL)animated {
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

#pragma mark - SampleApplicationControl

// Initialize the application trackers        
- (bool) doInitTrackers {
    // Initialize the image or marker tracker
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
    return [self loadAndActivateImageTrackerDataSet:@"FlakesBox.xml"];;
}

// start the application trackers
- (bool) doStartTrackers {
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
        // Activate the data set:
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


#pragma mark - left menu

typedef enum {
    C_AUTOFOCUS,
    C_FLASH
} MENU_CMDE;

- (void) prepareMenu {
    
    SampleAppMenu * menu = [SampleAppMenu prepareWithCommandProtocol:self title:@"Occlusion Management"];
    SampleAppMenuGroup * group;
    
    group = [menu addGroup:@""];
    [group addTextItem:@"About" command:-1];

    group = [menu addGroup:@""];
    [group addSelectionItem:@"Autofocus" command:C_AUTOFOCUS isSelected:false];
    [group addSelectionItem:@"Flash" command:C_FLASH isSelected:NO];
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

