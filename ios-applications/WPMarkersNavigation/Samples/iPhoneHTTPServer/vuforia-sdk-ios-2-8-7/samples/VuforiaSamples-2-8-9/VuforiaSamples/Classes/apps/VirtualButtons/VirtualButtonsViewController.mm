/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "VirtualButtonsViewController.h"
#import <QCAR/QCAR.h>
#import <QCAR/TrackerManager.h>
#import <QCAR/ImageTracker.h>
#import <QCAR/Rectangle.h>
#import <QCAR/ImageTarget.h>
#import <QCAR/DataSet.h>
#import <QCAR/VirtualButton.h>
#import <QCAR/CameraDevice.h>

@interface VirtualButtonsViewController ()

@end

@implementation VirtualButtonsViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        vapp = [[SampleApplicationSession alloc] initWithDelegate:self];
        
        // Custom initialization
        self.title = @"Virtual Buttons";
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
        
        // store the state of the buttons
        buttonStateChanged = false;
        for(int i = 0; i < NB_BUTTONS; i++) {
            buttonActivated[i] = true;
        }
        
        // a single tap will trigger a single autofocus operation
        tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(autofocus:)];
        
        // we use the iOS notification to pause/resume the AR when the application goes (or comeback from) background
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
    eaglView = [[VirtualButtonsEAGLView alloc] initWithFrame:viewFrame appSession:vapp];
    [self setView:eaglView];
    
    [self showLoadingAnimation];
    
    // initialize the AR session
    [vapp initAR:QCAR::GL_20 ARViewBoundsSize:viewFrame.size orientation:UIInterfaceOrientationPortrait];
}


- (void)viewDidLoad
{
    [super viewDidLoad];
    [self prepareMenu];

  // Do any additional setup after loading the view.
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    [self.view addGestureRecognizer:tapGestureRecognizer];
    
    NSLog(@"self.navigationController.navigationBarHidden:%d",self.navigationController.navigationBarHidden);
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

// Initialize the application trackers        
- (bool) doInitTrackers {
    // Initialize the image or marker tracker
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    
    // Image Tracker...
    QCAR::Tracker* trackerBase = trackerManager.initTracker(QCAR::ImageTracker::getClassType());
    if (trackerBase == NULL)
    {
        NSLog(@"Failed to initialize ImageTracker.");
        return NO;
    }
    return YES;
}

// load the data associated to the trackers
- (bool) doLoadTrackersData {
    return [self loadAndActivateImageTrackerDataSet:@"Wood.xml"];
}

// start the application trackers
- (bool) doStartTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::Tracker* tracker = trackerManager.getTracker(QCAR::ImageTracker::getClassType());
    if(tracker == 0) {
        return NO;
    }
    
    tracker->start();
    return YES;
}

// callback called when the initailization of the AR is done
- (void) onInitARDone:(NSError *)initError {
    [self hideLoadingAnimation];
    
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
    if (buttonStateChanged) {
        if (dataSet != NULL) {
            assert(dataSet->getNumTrackables() > 0);
            
            QCAR::ImageTracker* it = reinterpret_cast<QCAR::ImageTracker*>(QCAR::TrackerManager::getInstance().getTracker(QCAR::ImageTracker::getClassType()));
            
            // Deactivate the data set prior to reconfiguration:
            it->deactivateDataSet(dataSet);
            
            // we retrive the image target from the dataset
            QCAR::Trackable* trackable = dataSet->getTrackable(0);
            assert(trackable);
            
            assert(trackable->getType().isOfType(QCAR::ImageTarget::getClassType()));
            QCAR::ImageTarget* imageTarget = static_cast<QCAR::ImageTarget*>(trackable);
            
            static const char* virtualButtonColors[] = {
                "red",
                "blue",
                "yellow",
                "green"
            };
            
            static const float virtualButtonPositions[][4]= {
                {-108.68f, -53.52f, -75.75f, -65.87f},    // red
                {-45.28f, -53.52f, -12.35f, -65.87f},     // blue
                {14.82f, -53.52f, 47.75f, -65.87f},       // yellow
                {76.57f, -53.52f, 109.50f, -65.87f}       // green
            };
            
            for(int i = 0; i < NB_BUTTONS ; i++) {
                [self setVirtualButtonState:virtualButtonColors[i] position:virtualButtonPositions[i] state:buttonActivated[i] imageTarget:imageTarget];
            }
            
            // we can reactivate the dataset
            it->activateDataSet(dataSet);
        }
        buttonStateChanged = false;
    }
}

- (void) setVirtualButtonState:(const char *) name position:(const float *) position state:(bool) state imageTarget:(QCAR::ImageTarget* ) imageTarget{
    bool success = true;
    QCAR::VirtualButton* virtualButton;
    
    virtualButton = imageTarget->getVirtualButton(name);
    
    if (virtualButton) {
        if (! state) {
            // we delete this virtual button
            success = imageTarget->destroyVirtualButton(virtualButton);
        }
    } else {
        if (state) {
            QCAR::Rectangle vbRectangle(position[0], position[1], position[2], position[3]);
            virtualButton = imageTarget->createVirtualButton(name, vbRectangle);
            if (virtualButton) {
                // This is just a showcase; the values used here are set by default
                // on virtual button creation
                virtualButton->setEnabled(true);
                virtualButton->setSensitivity(QCAR::VirtualButton::MEDIUM);
                success = true;
            }
        }
    }
}



// stop your trackerts
- (bool) doStopTrackers {
    // Stop the tracker
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
        }
        else
        {
            // Activate the data set:
            if (!imageTracker->deactivateDataSet(dataSet))
            {
                NSLog(@"Failed to deactivate data set.");
            }
        }
        dataSet = NULL;
    }
    return YES;
}

// deinitialize your trackers
- (bool) doDeinitTrackers {
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    trackerManager.deinitTracker(QCAR::ImageTracker::getClassType());
    return YES;
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
    C_FLASH,
    C_CAMERA_FRONT,
    C_CAMERA_REAR,
    C_BUTTON_RED,
    C_BUTTON_BLUE,
    C_BUTTON_YELLOW,
    C_BUTTON_GREEN
} MENU_COMMAND;

- (void) prepareMenu {
    
    SampleAppMenu * menu = [SampleAppMenu prepareWithCommandProtocol:self title:@"Virtual Buttons"];
    SampleAppMenuGroup * group;
    
    group = [menu addGroup:@""];
    [group addTextItem:@"Vuforia Samples" command:-1];

    group = [menu addGroup:@""];
    [group addSelectionItem:@"Autofocus" command:C_AUTOFOCUS isSelected:NO];
    [group addSelectionItem:@"Flash" command:C_FLASH isSelected:NO];

    group = [menu addSelectionGroup:@"CAMERA"];
    [group addSelectionItem:@"Front" command:C_CAMERA_FRONT isSelected:NO];
    [group addSelectionItem:@"Rear" command:C_CAMERA_REAR isSelected:YES];
    
    group = [menu addGroup:@"BUTTON"];
    [group addSelectionItem:@"Red" command:C_BUTTON_RED isSelected:YES];
    [group addSelectionItem:@"Blue" command:C_BUTTON_BLUE isSelected:YES];
    [group addSelectionItem:@"Yellow" command:C_BUTTON_YELLOW isSelected:YES];
    [group addSelectionItem:@"Green" command:C_BUTTON_GREEN isSelected:YES];
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
        case C_CAMERA_FRONT:
        case C_CAMERA_REAR: {
            if ([vapp stopCamera:&error]) {
                result = [vapp startAR:(command == C_CAMERA_FRONT) ? QCAR::CameraDevice::CAMERA_FRONT:QCAR::CameraDevice::CAMERA_BACK error:&error];
            } else {
                result = false;
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
            
        case C_BUTTON_RED:
            buttonStateChanged = true;
            buttonActivated[0] = value;
            break;
            
        case C_BUTTON_BLUE:
            buttonStateChanged = true;
            buttonActivated[1] = value;
            break;
            
        case C_BUTTON_YELLOW:
            buttonStateChanged = true;
            buttonActivated[2] = value;
            break;
            
        case C_BUTTON_GREEN:
            buttonStateChanged = true;
            buttonActivated[3] = value;
            break;
            
        default:
            result = false;
            break;
    }
    return result;
}

@end

