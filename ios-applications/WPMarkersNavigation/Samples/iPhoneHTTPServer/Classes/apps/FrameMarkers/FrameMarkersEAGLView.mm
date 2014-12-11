/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <QuartzCore/QuartzCore.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <sys/time.h>

#import <QCAR/QCAR.h>
#import <QCAR/State.h>
#import <QCAR/Tool.h>
#import <QCAR/Renderer.h>
#import <QCAR/TrackableResult.h>
#import <QCAR/VideoBackgroundConfig.h>
#import <QCAR/MarkerResult.h>
#import <QCAR/Image.h>

#import "VuforiaObject3D.h"

#include "SampleMath.h"
#import "A_object.h"
#import "C_object.h"
#import "Q_object.h"
#import "R_object.h"

#import "FrameMarkersEAGLView.h"
#import "Texture.h"
#import "SampleApplicationUtils.h"
#import "SampleApplicationShaderUtils.h"




//******************************************************************************
// *** OpenGL ES thread safety ***
//
// OpenGL ES on iOS is not thread safe.  We ensure thread safety by following
// this procedure:
// 1) Create the OpenGL ES context on the main thread.
// 2) Start the QCAR camera, which causes QCAR to locate our EAGLView and start
//    the render thread.
// 3) QCAR calls our renderFramurleQCAR method periodically on the render thread.
//    The first time this happens, the defaultFramebuffer does not exist, so it
//    is created with a call to createFramebuffer.  createFramebuffer is called
//    on the main thread in order to safely allocate the OpenGL ES storage,
//    which is shared with the drawable layer.  The render (background) thread
//    is blocked during the call to createFramebuffer, thus ensuring no
//    concurrent use of the OpenGL ES context.
//
//******************************************************************************


namespace {
    // --- Data private to this unit ---
    // Letter object scale factor and translation
    const float kLetterScale = 25.0f;
    const float kLetterTranslate = 25.0f;
    
    // Texture filenames
    const char* textureFilenames[] = {
        "letter_Q.png",
        "letter_C.png",
        "letter_A.png",
        "letter_R.png"
    };
}


@interface FrameMarkersEAGLView (PrivateMethods)

- (void)initShaders;
- (void)createFramebuffer;
- (void)deleteFramebuffer;
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;

- (void)updateMarkersInfo :(int)markerId :(bool)detected :(int)i1 :(int)i2 :(float)dist :(float)z :(float)tpx :(float)tpz;
- (void)startExploring;
- (void)goToNextTarget;
- (void)updateWheelphone: (NSTimer*)timer;
- (void)calculateSpeeds;
- (void)rotataImages;
- (void)saveScreenShot :(int)x :(int)y :(int)w :(int)h :(NSString*)filename;
- (void)calibrateSensors;
- (void)startButtonTapped;

@end


@implementation FrameMarkersEAGLView

// You must implement this method, which ensures the view's underlying layer is
// of type CAEAGLLayer
+ (Class)layerClass
{
    return [CAEAGLLayer class];
}


//------------------------------------------------------------------------------
#pragma mark - Lifecycle

- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app
{
    
    self = [super initWithFrame:frame];
    
    
    UIButton *buttonCalibrate = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [buttonCalibrate addTarget:self
               action:@selector(calibrateSensors)
     forControlEvents:UIControlEventTouchUpInside];
    [buttonCalibrate setTitle:@"Calibrate Sensors" forState:UIControlStateNormal];
    buttonCalibrate.frame = CGRectMake(6.0, 410.0, 150.0, 40.0);
    [self addSubview:buttonCalibrate];
    
    UIButton *buttonStart = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [buttonStart addTarget:self
               action:@selector(startButtonTapped)
     forControlEvents:UIControlEventTouchUpInside];
    [buttonStart setTitle:@"Start" forState:UIControlStateNormal];
    buttonStart.frame = CGRectMake(163.0, 410.0, 150.0, 40.0);
    [self addSubview:buttonStart];
    
    if (self) {
        mLastDetectedMarkerId = 0;
        mCurrentTargetId = 2;
        mCurrentScreenshot = 2;
        rotCounter = 0;
        chargeCounter = 0;
        instableChargeCounter = 0;
        rSpeed = 0;
        lSpeed = 0;
        rSpeedTemp = 0;
        lSpeedTemp = 0;
        mSearchAngle = 20;
        mStopDistance = 120;
        mStopDistanceTemp = 120;
        rotationFactor = 0;
        dockReachedTimeout = 0;
        baseSpeed = BASE_SPEED;
        mSeenOnce = false;
        invertRotation = true;
        rotationInProgress = false;
        takeScreenshot = false;
        lastRotation = ROT_LEFT;
        globalState = STATE_TARGET_SEARCH_AND_APPROACH;
        prevGlobalState = STATE_TARGET_SEARCH_AND_APPROACH;
        chargeStatus = ROBOT_NOT_CHARGING;
        self.mTimestampLastSeen = [[NSDate alloc] init];
        self.mGlobalTimestamp = [[NSDate alloc] init];
        self.mTimestampDocking = nil;
        self.mObstacleLastSeenTimestamp = nil;
        self.wheelphone = [WheelphoneRobot new];
        [self.wheelphone enableSpeedControl];
        [self.wheelphone enableSoftAcceleration];
        [self.wheelphone setCommunicationTimeout:10000];
        
        self.refAttitude = nil;
        self.motionManager = [[CMMotionManager alloc] init];
        if (self.motionManager.deviceMotionAvailable ) {
            [self.motionManager setDeviceMotionUpdateInterval: 1.0/20.0];
            [self.motionManager startDeviceMotionUpdates];
        }
        
        for(int i=0; i<NUM_TARGETS; i++) {
        	targetDetectedInfo[i] = NO_INFO;
        }
        [self startExploring];
        
        vapp = app;
        // Enable retina mode if available on this device
        if (YES == [vapp isRetinaDisplay]) {
            [self setContentScaleFactor:2.0f];
        }
        
        objects3D = [[NSMutableArray alloc] initWithCapacity:4];

        // Load the augmentation textures
        for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
            augmentationTexture[i] = [[Texture alloc] initWithImageFile:[NSString stringWithCString:textureFilenames[i] encoding:NSASCIIStringEncoding]];
        }
        
        // Create the OpenGL ES context
        context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
        
        // The EAGLContext must be set for each thread that wishes to use it.
        // Set it the first time this method is called (on the main thread)
        if (context != [EAGLContext currentContext]) {
            [EAGLContext setCurrentContext:context];
        }
        
        // Generate the OpenGL ES texture and upload the texture data for use
        // when rendering the augmentation
        for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
            GLuint textureID;
            glGenTextures(1, &textureID);
            [augmentationTexture[i] setTextureID:textureID];
            glBindTexture(GL_TEXTURE_2D, textureID);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, [augmentationTexture[i] width], [augmentationTexture[i] height], 0, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid*)[augmentationTexture[i] pngData]);
        }
        [self setup3dObjects];

        offTargetTrackingEnabled = NO;

        [self initShaders];

    }

    self.time = [[NSDate alloc] init];
    //NSTimer *currentTimer = [NSTimer scheduledTimerWithTimeInterval: 1.0/20.0 target: self selector:@selector(updateWheelphone:) userInfo: nil repeats:YES];
    return self;
}


-(void)robotUpdateNotification: (NSNotification*)notification {
    
    /*if(getFirmwareFlag) {
        firmwareVersion=wheelphone.getFirmwareVersion();
        if(firmwareVersion>0) {	// wait for the first USB transaction to be accomplished
            getFirmwareFlag = false;
            if(firmwareVersion >= 3) {
                Toast.makeText(WheelphoneTargetNavigation.this, "Firmware version "+firmwareVersion+".0, fully compatible.", Toast.LENGTH_SHORT).show();
                //msgbox("Firmware version "+firmwareVersion+".0", "Firmware is fully compatible.");
            } else {
                //Toast.makeText(WheelphoneActivity.this, "Firmware version "+firmwareVersion+".0, NOT fully compatible. Update robot firmware.", Toast.LENGTH_LONG).show();
                msgbox("Firmware version "+firmwareVersion+".0", "Firmware is NOT fully compatible. Update robot firmware.");
            }
        }
    }
     */
    
    
    if (self.refAttitude == nil) {
        self.refAttitude = self.motionManager.deviceMotion.attitude;
        [self.wheelphone setOdometryx:0.0 y:0.0 theta: 0.0];
        angle = 0;
    }
    
    NSTimeInterval timeInterval = [self.time timeIntervalSinceNow];
    self.time = [NSDate date];
    
    double currentAngle = [self.wheelphone getOdometryTheta];
    
    x = [self.wheelphone getOdometryX];
    y = [self.wheelphone getOdometryY];
    
    if (gyroscope) {
        self.currentAttitude = self.motionManager.deviceMotion.attitude;
        [self.currentAttitude multiplyByInverseOfAttitude: self.refAttitude];
        
        currentAngle = self.currentAttitude.roll;
        
        int leftSpeed = [self.wheelphone getLeftSpeed];
        int rightSpeed = [self.wheelphone getRightSpeed];
        double R = (9.0/2.0) * ((leftSpeed + rightSpeed) / (rightSpeed - leftSpeed));
        double w = (rightSpeed - leftSpeed)/9.0;
        double ICCx = x - R * sin(currentAngle);
        double ICCy = y + R * cos(currentAngle);
        double currentX = cos(w * (-timeInterval)) * (x - ICCx) - sin(w * (-timeInterval)) * (y - ICCy) + ICCx;
        double currentY = sin(w * (-timeInterval)) * (x - ICCx) + cos(w * (-timeInterval)) * (y - ICCy) + ICCy;
        currentAngle = currentAngle * 180/M_PI;
        x = currentX;
        y = currentY;
        
        angle = angle * 180/M_PI;
        
        if (angle >= 0) {
            if (currentAngle >= 0) {
                if (angle > 330) {
                    angle = 360 + currentAngle;
                } else {
                    angle = currentAngle;
                }
                
            } else {
                if (angle > 150) {
                    angle = 360 + currentAngle;
                } else {
                    angle = currentAngle;
                }
            }
        } else {
            if (currentAngle < 0) {
                if (angle < -330) {
                    angle = -360 + currentAngle;
                } else {
                    angle = currentAngle;
                }
                
            } else {
                if (angle < -150) {
                    angle = -360 + currentAngle;
                } else {
                    angle = currentAngle;
                }
            }
        }
        angle = angle * M_PI/180;
        
    } else {
        angle = currentAngle;
    }

    //////////////////////
    
    if([self.wheelphone isCharging] || [self.wheelphone isCharged]) {
        if([self.wheelphone isCharged]) {
            chargeStatus = ROBOT_CHARGED;
        } else {
            chargeStatus = ROBOT_IN_CHARGE;
        }
    } else {
        chargeStatus = ROBOT_NOT_CHARGING;
    }
    NSLog(@"ARE YOU FOR REAL CHARGING???? %d", chargeStatus);
    
    if([self.wheelphone isCalibrating]) {
        return;
    }
    
    /*
    if(mAppStatus == APPSTATUS_CAMERA_RUNNING) {
        getTrackInfo();
    }*/
    
    //		fpv.updateRobotInfo(100, 100, (0 + (int)targetOrientation[0] + 180)%360);
    
    if(mIsExploring) {
        mObstacleDesiredRotation = 0;
        mObstacleDesiredAcceleration = 0;
    } else {
        int frontProx[4];
        [self.wheelphone getFrontProxs:frontProx];
        int maxIdx = 0;
        double max = 0;
        
        //find closest object:
        for (int i=0 ; i<4 ; i++){

            if (frontProx[i]>max){
                maxIdx = i;
                max = frontProx[i];
            }
        }
        
        
        //Rotate away from the closest sensed object (scaling: uses sensor idx to scale)
        if (max > 40){
            self.mObstacleLastSeenTimestamp = [[NSDate alloc] init];
            
            //obstacle is present, rotation points away from the sensor that feels the obstacle the closest. Range [-2, 2]. (turn faster than the target seeker)
            mObstacleDesiredRotation = - (double)2 / (double)((maxIdx <= 1) ? maxIdx-2 : maxIdx-1);
            //the higher the value of the sensor, the closest the obstacle. So the closer the slower we should go. Range [0, 1]
            mObstacleDesiredAcceleration = abs(frontProx[maxIdx] / 255. - 1);
            
            
        } else {
            //Obstacle is on one side of the robot, so go parallel to it:
            mObstacleDesiredRotation = 0;
            mObstacleDesiredAcceleration = 1;
        }
        
    }
    
    [self calculateSpeeds];
    
    NSLog(@"State: %d", globalState);
    if(globalState == STATE_WAITING_START) {
        
        rSpeed = 0;
        lSpeed = 0;
		
    } else if(globalState==STATE_TARGET_SEARCH_AND_APPROACH) {
        
        timeInterval = [self.time timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > GLOBAL_TIMEOUT) {
            [self goToNextTarget];
        }
        
        if(mIsExploring) {
            if(rotationInProgress) {
                if(abs((angle/M_PI*180.0)) >= mSearchAngle) {
                    lSpeed = 0;
                    rSpeed = 0;
                    rotCounter = 0;
                    rotationInProgress = false;
                }
            } else {
                rotCounter++;
                if(rotCounter >= 15) {	// stopped for about 1.5 seconds
                    [self.wheelphone resetOdometry];
                    self.refAttitude = nil;
                    rotationInProgress = true;
                    lSpeed = mLeftSpeed;
                    rSpeed = mRightSpeed;
                }
            }
        } else {
            lSpeed = mLeftSpeed;
            rSpeed = mRightSpeed;
        }
        
        if(switchToLittleTag[mCurrentTargetId]) {
            mStopDistanceTemp = mStopDistance*4;
        } else {
            mStopDistanceTemp = mStopDistance;
        }
        if(targetDetectedInfo[mCurrentTargetId]==TARGET_FOUND) {
            if(targetX[mCurrentTargetId] < self.frame.size.width/2) {
                lastRotation = ROT_LEFT;
            } else {
                lastRotation = ROT_RIGHT;
            }
        }
        if(targetDist[mCurrentTargetId] <= mStopDistanceTemp && targetDetectedInfo[mCurrentTargetId]==TARGET_FOUND) {
            
            if(mCurrentTargetId == 2) {
                //tts.speak("living room", TextToSpeech.QUEUE_FLUSH, null);
            } else if(mCurrentTargetId == 3) {
                //tts.speak("kitchen", TextToSpeech.QUEUE_FLUSH, null);
            }
            [self startExploring];
            lSpeed = 0;
            rSpeed = ROT_SPEED;
            rotationInProgress = false;
            rotCounter = 0;
            [self.wheelphone resetOdometry];
            self.refAttitude = nil;
            globalState = STATE_ROTATE_FOR_SCREENSHOT;
            prevGlobalState = STATE_TARGET_SEARCH_AND_APPROACH;
            
        }
        
        //globalState = STATE_DOCKING_SEARCH;
        
    } else if(globalState == STATE_DOCKING_SEARCH) {
        timeInterval = [self.mTimestampDocking timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > DOCK_TIMEOUT) {
            [self goToNextTarget];
        }
        [self.wheelphone disableSoftAcceleration];
        baseSpeed = 35; //BASE_SPEED;
        [self.wheelphone disableObstacleAvoidance];
        if(invertRotation) {
            invertRotation = false;
            if(lastRotation == ROT_RIGHT) {
                lSpeedTemp = baseSpeed;		// pivot rotating to find the docking station
                rSpeedTemp = -baseSpeed;		// rotate at low speed to have time to detect the target
            } else {
                lSpeedTemp = -baseSpeed;
                rSpeedTemp = baseSpeed;
            }

        }
        if(rotationInProgress) {
            if(abs((angle/M_PI*180.0)) >= mSearchAngle) {
                lSpeedTemp = lSpeed;
                rSpeedTemp = rSpeed;
                lSpeed = 0;
                rSpeed = 0;
                rotCounter = 0;
                rotationInProgress = false;
            }
        } else {
            rotCounter++;
            if(rotCounter >= 15) {	// stopped for about 1.5 seconds
                [self.wheelphone resetOdometry];
                self.refAttitude = nil;
                rotationInProgress = true;
                lSpeed = lSpeedTemp;
                rSpeed = rSpeedTemp;
            }
        }
        
        if(targetDetectedInfo[0]==TARGET_FOUND) {
            globalState = STATE_DOCKING_APPROACH_SMALL;
            prevGlobalState = STATE_DOCKING_SEARCH;
        } else if(targetDetectedInfo[1]==TARGET_FOUND) {
            globalState = STATE_DOCKING_APPROACH_BIG;
            prevGlobalState = STATE_DOCKING_SEARCH;
            [self.wheelphone enableObstacleAvoidance];
        }
        
        
    } else if(globalState == STATE_DOCKING_APPROACH_BIG) {
        timeInterval = [self.mGlobalTimestamp timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > GLOBAL_TIMEOUT) {
            [self goToNextTarget];
        }
        
        self.mTimestampDocking = [NSDate date];
        
        [self.wheelphone enableSoftAcceleration];
        baseSpeed = BASE_SPEED;
        
        if(targetDetectedInfo[1] == TARGET_FOUND) {
            rotationFactor = (int)(mCoordCoeff*(self.frame.size.width/2-targetX[1]) + mOrientCoeff*angleTargetRobot[1]);
            if(targetX[1] < self.frame.size.width/2) {
                lastRotation = ROT_LEFT;
            } else {
                lastRotation = ROT_RIGHT;
            }
            lSpeed = baseSpeed - rotationFactor;
            rSpeed = baseSpeed + rotationFactor;
        } else if(targetDetectedInfo[0] == TARGET_FOUND) {
            globalState = STATE_DOCKING_APPROACH_SMALL;
            prevGlobalState = STATE_DOCKING_APPROACH_BIG;
        } else if(targetDetectedInfo[1] == NO_TARGET_FOUND) {
            //globalState = STATE_DOCKING_SEARCH;
            dockReachedTimeout = 0;
            globalState = STATE_DOCKING_SEARCH;
            prevGlobalState = STATE_DOCKING_APPROACH_BIG;
            invertRotation = true;
            rotCounter = 0;
            rotationInProgress = true;
        }
        
    } else if(globalState == STATE_DOCKING_APPROACH_SMALL) {
        
        timeInterval = [self.mGlobalTimestamp timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > GLOBAL_TIMEOUT) {
            [self goToNextTarget];
        }
        
        self.mTimestampDocking = [NSDate date];
        
        [self.wheelphone enableSoftAcceleration];
        baseSpeed = BASE_SPEED;
        
        if(targetDist[0] <= 380) {	// disable obstacle avoidance when approaching the docking station
            [self.wheelphone disableObstacleAvoidance];
            baseSpeed = BASE_SPEED*2/3;
        } else {
            [self.wheelphone enableObstacleAvoidance];
            baseSpeed = BASE_SPEED;
        }
        
        if(targetDist[0] <= 200) {
            globalState = STATE_DOCKING_REACHED;
            prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
            dockReachedTimeout = 0;
        }
        
        if(targetDetectedInfo[1] == TARGET_FOUND) {
            globalState = STATE_DOCKING_APPROACH_BIG;
            prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
            [self.wheelphone enableObstacleAvoidance];
        } else if(targetDetectedInfo[0] == NO_TARGET_FOUND) {
            //globalState = STATE_DOCKING_SEARCH;
            dockReachedTimeout = 0;
            globalState = STATE_DOCKING_SEARCH;
            prevGlobalState = STATE_DOCKING_APPROACH_SMALL;
            invertRotation = true;
            rotCounter = 0;
            rotationInProgress = true;
        } else if(targetDetectedInfo[0] == TARGET_FOUND) {
            lastTargetX[0] = targetX[0];
            rotationFactor = (int)(mCoordCoeff*(self.frame.size.width/2-targetX[0]) + mOrientCoeff*angleTargetRobot[0]);
            if(targetX[0] < self.frame.size.width/2) {
                lastRotation = ROT_LEFT;
            } else {
                lastRotation = ROT_RIGHT;
            }
            lSpeed = baseSpeed - rotationFactor;
            rSpeed = baseSpeed + rotationFactor;
        }
        
    } else if(globalState == STATE_DOCKING_REACHED) {
        
        timeInterval = [self.mTimestampDocking timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > DOCK_TIMEOUT) {
            [self goToNextTarget];
        }
        
        [self.wheelphone enableSoftAcceleration];
        baseSpeed = BASE_SPEED;
        
        lSpeed = baseSpeed; //*2;
        rSpeed = baseSpeed; //*2;
        NSLog(@"Robot in charge???: %d", chargeStatus);
        if(chargeStatus == ROBOT_IN_CHARGE || chargeStatus == ROBOT_CHARGED) {
            chargeCounter = 0;
            globalState = STATE_ROBOT_CHARGING;
            prevGlobalState = STATE_DOCKING_REACHED;
        }
        
        if(prevGlobalState==STATE_ROBOT_CHARGING) {
            instableChargeCounter++;
            if(instableChargeCounter >= 40) {
                lSpeed = 40;
                rSpeed = 40;
            }
            if(instableChargeCounter >= 55) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 70) {
                lSpeed = -20;
                rSpeed = 20;
            }
            if(instableChargeCounter >= 85) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 100) {
                lSpeed = 40;
                rSpeed = 40;
            }
            if(instableChargeCounter >= 115) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 130) {
                lSpeed = 20;
                rSpeed = -20;
            }
            if(instableChargeCounter >= 145) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 160) {
                lSpeed = 40;
                rSpeed = 40;
            }
            if(instableChargeCounter >= 175) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 190) {
                lSpeed = 20;
                rSpeed = -20;
            }
            if(instableChargeCounter >= 205) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 220) {
                lSpeed = 40;
                rSpeed = 40;
            }
            if(instableChargeCounter >= 235) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 250) {
                lSpeed = -20;
                rSpeed = 20;
            }
            if(instableChargeCounter >= 265) {
                lSpeed = 0;
                rSpeed = 0;
            }
            if(instableChargeCounter >= 280) {
                instableChargeCounter = 40;
            }
        }
        
        dockReachedTimeout++;	// if the robot is near the charger but do not get ever the contact with the charger station
        // or cannot get well docked then go back and retry
        
        if(dockReachedTimeout>=50 && prevGlobalState==STATE_DOCKING_APPROACH_SMALL) {	// the first time we try to dock maybe we are a little skew,
            if(lastTargetX[0] < self.frame.size.width/2) {										// so try to straighten
                lSpeed = -20;
                rSpeed = 20;
            } else {
                lSpeed = 20;
                rSpeed = -20;
            }
        }
        if(dockReachedTimeout>=58 && prevGlobalState==STATE_DOCKING_APPROACH_SMALL) {
            lSpeed = baseSpeed*2;
            rSpeed = baseSpeed*2;
        }
        
        if(dockReachedTimeout >= 300) {	// about 15 seconds
            if(lastTargetX[0] < self.frame.size.width/2) {										// so try to straighten
                lSpeed = -70;
                rSpeed = -50;
            } else {
                lSpeed = -50;
                rSpeed = -70;
            }
            dockReachedTimeout = 0;
            globalState = STATE_ROBOT_GO_BACK_DOCKING;
            prevGlobalState = STATE_DOCKING_REACHED;
        }
        
        if(targetDist[0]>380 || targetDist[1]>380) { // || (targetDist[0]==0 && targetDist[1]==0)) {
            globalState = STATE_DOCKING_SEARCH;
            prevGlobalState = STATE_DOCKING_REACHED;
            invertRotation = true;
            rotCounter = 0;
            rotationInProgress = false;
        }
        
        
    } else if(globalState == STATE_ROBOT_GO_BACK_DOCKING) {
        
        timeInterval = [self.mTimestampDocking timeIntervalSinceNow] * 1000;
        if(abs(timeInterval) > DOCK_TIMEOUT) {
            [self goToNextTarget];
        }
        
        dockReachedTimeout++;
        if(dockReachedTimeout >= 20) {	// go back for about 0.5 seconds
            globalState = STATE_DOCKING_SEARCH;
            prevGlobalState = STATE_ROBOT_GO_BACK_DOCKING;
            invertRotation = true;
            rotCounter = 0;
            rotationInProgress = false;
        }
        
    } else if(globalState == STATE_ROBOT_CHARGING) {
        
        rSpeed = 0;
        lSpeed = 0;
        
        if(chargeStatus == ROBOT_NOT_CHARGING) {
            globalState = STATE_DOCKING_REACHED;
            prevGlobalState = STATE_ROBOT_CHARGING;
            dockReachedTimeout = 0;
        } else {
            chargeCounter++;
            if(chargeCounter == 20) {
                instableChargeCounter = 0;
            } else if(chargeCounter >= (mChargeTime*20)) {	// *1000/50
                self.mGlobalTimestamp = [NSDate date];
                self.mTimestampDocking = [NSDate date];
                chargeCounter = mChargeTime*20;
                if(mContinuousMode) {
                    dockReachedTimeout = 0;
                    lSpeed = -70;
                    rSpeed = -70;
                    globalState = STATE_ROBOT_GO_BACK_TARGET;
                    prevGlobalState = STATE_ROBOT_CHARGING;
                }
            }
        }					    	
        
    } else if(globalState == STATE_ROBOT_GO_BACK_TARGET) {
        dockReachedTimeout++;
        if(dockReachedTimeout >= 20) {	// go back for about 0.5 seconds
            lSpeed = 0;
            rSpeed = ROT_SPEED;
            globalState = STATE_ROTATE_FOR_SCREENSHOT;
            prevGlobalState = STATE_ROBOT_GO_BACK_TARGET;
            invertRotation = true;
            rotCounter = 0;
            [self.wheelphone resetOdometry];
            self.refAttitude = nil;
        }
    } else if(globalState == STATE_ROTATE_FOR_SCREENSHOT) {
        //rotCounter++;
        //if(rotCounter >= 55) {	
        if(angle >= (M_PI - (25.0/180.0*M_PI))) {
            lSpeed = 0;
            rSpeed = 0;
            rotCounter=0;
            globalState = STATE_WAIT_STOPPED_FOR_SCREENSHOT;
            prevGlobalState = STATE_ROTATE_FOR_SCREENSHOT;
        }
    } else if(globalState == STATE_WAIT_STOPPED_FOR_SCREENSHOT) {
        rotCounter++;
        if(rotCounter>=5) {
            takeScreenshot = true;
            //mRenderer.saveScreenShot();
            prevGlobalState = STATE_WAIT_STOPPED_FOR_SCREENSHOT;
            globalState = STATE_TAKE_SCREENSHOT;
        }
    } else if(globalState == STATE_TAKE_SCREENSHOT) {
        if(takeScreenshot == false) {
            [self goToNextTarget];
        }
    }
    NSLog(@"left speed %d",lSpeed);
    NSLog(@"right speed %d",rSpeed);
    
    [self.wheelphone setLeftSpeed:lSpeed];
    [self.wheelphone setRightSpeed:rSpeed];
    
    
}


- (void)dealloc
{
    [self deleteFramebuffer];
    
    // Tear down context
    if ([EAGLContext currentContext] == context) {
        [EAGLContext setCurrentContext:nil];
    }
    
    //[context release];

    /*for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
        [augmentationTexture[i] release];
    }

    [super dealloc];*/
}


- (void)finishOpenGLESCommands
{
    // Called in response to applicationWillResignActive.  The render loop has
    // been stopped, so we now make sure all OpenGL ES commands complete before
    // we (potentially) go into the background
    if (context) {
        [EAGLContext setCurrentContext:context];
        glFinish();
    }
}


- (void)freeOpenGLESResources
{
    // Called in response to applicationDidEnterBackground.  Free easily
    // recreated OpenGL ES resources
    [self deleteFramebuffer];
    glFinish();
}


- (void) add3DObjectWith:(int)numVertices ofVertices:(const float *)vertices normals:(const float *)normals texcoords:(const float *)texCoords with:(int)numIndices ofIndices:(const unsigned short *)indices usingTextureIndex:(NSInteger)textureIndex
{
    VuforiaObject3D *obj3D = [[VuforiaObject3D alloc] init];
    
    obj3D.numVertices = numVertices;
    obj3D.vertices = vertices;
    obj3D.normals = normals;
    obj3D.texCoords = texCoords;
    
    obj3D.numIndices = numIndices;
    obj3D.indices = indices;
    
    obj3D.texture = augmentationTexture[textureIndex];
    
    [objects3D addObject:obj3D];
    //[obj3D release];
}

- (void) setup3dObjects
{
    // build the array of objects we want drawn and their texture
    // in this example we have 4 textures and 4 objects - Q, C, A, R
    
    [self add3DObjectWith:NUM_Q_OBJECT_VERTEX ofVertices:QobjectVertices normals:QobjectNormals texcoords:QobjectTexCoords
                     with:NUM_Q_OBJECT_INDEX ofIndices:QobjectIndices usingTextureIndex:0];
    
    [self add3DObjectWith:NUM_C_OBJECT_VERTEX ofVertices:CobjectVertices normals:CobjectNormals texcoords:CobjectTexCoords
                     with:NUM_C_OBJECT_INDEX ofIndices:CobjectIndices usingTextureIndex:1];
    
    [self add3DObjectWith:NUM_A_OBJECT_VERTEX ofVertices:AobjectVertices normals:AobjectNormals texcoords:AobjectTexCoords
                     with:NUM_A_OBJECT_INDEX ofIndices:AobjectIndices usingTextureIndex:2];
    
    [self add3DObjectWith:NUM_R_OBJECT_VERTEX ofVertices:RobjectVertices normals:RobjectNormals texcoords:RobjectTexCoords
                     with:NUM_R_OBJECT_INDEX ofIndices:RobjectIndices usingTextureIndex:3];
}

- (void) setOffTargetTrackingMode:(BOOL) enabled {
    offTargetTrackingEnabled = enabled;
}


//------------------------------------------------------------------------------
#pragma mark - UIGLViewProtocol methods

// Draw the current frame using OpenGL
//
// This method is called by QCAR when it wishes to render the current frame to
// the screen.
//
// *** QCAR will call this method periodically on a background thread ***
- (void)renderFrameQCAR
{
    [self setFramebuffer];
    bool isFrontCamera = false;
    
    //QCAR::Vec2F markerSize;
   	int jx[4] = {0};
	int jy[4] = {0};
	float distance[4] = {0};
	float cam_z[4] = {0};
	float target_pose_x[4] = {0};	// x, y, z coordinates of the targets with respect to the camera frame
	float target_pose_y[4] = {0};
	float target_pose_z[4] = {0};
	
	bool detected[4] = {false};
    
    // Clear colour and depth buffers
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // Retrieve tracking state and render video background and
    QCAR::State state = QCAR::Renderer::getInstance().begin();
    QCAR::Renderer::getInstance().drawVideoBackground();
    
    glEnable(GL_DEPTH_TEST);
    // We must detect if background reflection is active and adjust the culling direction.
    // If the reflection is active, this means the pose matrix has been reflected as well,
    // therefore standard counter clockwise face culling will result in "inside out" models.
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    if(QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON) {
        glFrontFace(GL_CW);  //Front camera
        isFrontCamera = true;
    } else {
        glFrontFace(GL_CCW);   //Back camera
    }
    
    // Did we find any trackables this frame?
    for(int i = 0; i < state.getNumTrackableResults(); ++i) {
        // Get the trackable
        const QCAR::TrackableResult* trackableResult = state.getTrackableResult(i);
        QCAR::Matrix44F modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(trackableResult->getPose());
        // Check the type of the trackable:
        assert (trackableResult->isOfType(QCAR::MarkerResult::getClassType()));
        const QCAR::MarkerResult* markerResult = static_cast<
        const QCAR::MarkerResult*>(trackableResult);
        const QCAR::Marker& marker = markerResult->getTrackable();
        
        NSLog(@"[%s] tracked", marker.getName());
        if (trackableResult->getStatus() == QCAR::TrackableResult::EXTENDED_TRACKED) {
            NSLog(@"[%s] tracked with target out of view!", marker.getName());
        }
        
        // Choose the object and texture based on the marker ID
        int textureIndex = marker.getMarkerId();
        assert(textureIndex < NUM_AUGMENTATION_TEXTURES);
        
        QCAR::Vec2F result(0,0);
		const QCAR::CameraCalibration& cameraCalibration = QCAR::CameraDevice::getInstance().getCameraCalibration();
	    QCAR::Vec2F cameraPoint = QCAR::Tool::projectPoint(cameraCalibration, trackableResult->getPose(), QCAR::Vec3F(0, 0, 0));
	    QCAR::VideoMode videoMode = QCAR::CameraDevice::getInstance().getVideoMode(QCAR::CameraDevice::MODE_OPTIMIZE_QUALITY); //MODE_DEFAULT);
	    QCAR::VideoBackgroundConfig config = QCAR::Renderer::getInstance().getVideoBackgroundConfig();
        float screenWidth = self.frame.size.width;
        float screenHeight = self.frame.size.height;
	    int xOffset = ((int) screenWidth - config.mSize.data[0]) / 2.0f + config.mPosition.data[0];
	    int yOffset = ((int) screenHeight - config.mSize.data[1]) / 2.0f - config.mPosition.data[1];
        
        

        //if (isActivityInPortraitMode)
        if (true)
	    {
	        // camera image is rotated 90 degrees
	        int rotatedX = videoMode.mHeight - cameraPoint.data[1];
	        int rotatedY = cameraPoint.data[0];
            
	        result = QCAR::Vec2F(rotatedX * config.mSize.data[0] / (float) videoMode.mHeight + xOffset,
                                 rotatedY * config.mSize.data[1] / (float) videoMode.mWidth + yOffset);
	    }
	    else
	    {
	        result = QCAR::Vec2F(cameraPoint.data[0] * config.mSize.data[0] / (float) videoMode.mWidth + xOffset,
                                 cameraPoint.data[1] * config.mSize.data[1] / (float) videoMode.mHeight + yOffset);
	    }
        jx[textureIndex] = (int)result.data[0];
        jy[textureIndex] = (int)result.data[1];

        // get position and orientation of the target respect to the camera reference frame
       	QCAR::Matrix34F pose = trackableResult->getPose();
       	target_pose_x[textureIndex] = pose.data[3];
       	target_pose_y[textureIndex] = pose.data[7];
       	target_pose_z[textureIndex] = pose.data[11];
		QCAR::Vec3F position(pose.data[3], pose.data[7], pose.data[11]);
		// dist = modulo del vettore traslazione = sqrt(x*x + y*y + z*z)
		distance[textureIndex] = sqrt(position.data[0] * position.data[0] + position.data[1] * position.data[1] + position.data[2] * position.data[2]);
        QCAR::Matrix44F inverseMV = SampleMath::Matrix44FInverse(modelViewMatrix);
        QCAR::Matrix44F invTranspMV = SampleMath::Matrix44FTranspose(inverseMV);
        // position of the camera and orientation axis with coordinates represented in the reference frame of the trackable
        //cam_x[textureIndex] = invTranspMV.data[4];
        //cam_y[textureIndex] = invTranspMV.data[5];
        cam_z[textureIndex] = invTranspMV.data[6];
        detected[textureIndex] = true;
        
        
        VuforiaObject3D *obj3D = [objects3D objectAtIndex:textureIndex];
        
        // Render with OpenGL 2
        QCAR::Matrix44F modelViewProjection;
        if (isFrontCamera) {
            SampleApplicationUtils::scalePoseMatrix(-1, 1, 1, &modelViewMatrix.data[0]);
        }
        SampleApplicationUtils::translatePoseMatrix(-kLetterTranslate, -kLetterTranslate, 0.f, &modelViewMatrix.data[0]);
        SampleApplicationUtils::scalePoseMatrix(kLetterScale, kLetterScale, kLetterScale, &modelViewMatrix.data[0]);
        SampleApplicationUtils::multiplyMatrix(&vapp.projectionMatrix.data[0], &modelViewMatrix.data[0], &modelViewProjection.data[0]);
        
        glUseProgram(shaderProgramID);
        
        glVertexAttribPointer(vertexHandle, 3, GL_FLOAT, GL_FALSE, 0, obj3D.vertices);
        glVertexAttribPointer(normalHandle, 3, GL_FLOAT, GL_FALSE, 0, obj3D.normals);
        glVertexAttribPointer(textureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, obj3D.texCoords);
        
        glEnableVertexAttribArray(vertexHandle);
        glEnableVertexAttribArray(normalHandle);
        glEnableVertexAttribArray(textureCoordHandle);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, [obj3D.texture textureID]);
        glUniformMatrix4fv(mvpMatrixHandle, 1, GL_FALSE, (GLfloat*)&modelViewProjection.data[0]);
        glUniform1i(texSampler2DHandle, 0 /*GL_TEXTURE0*/);
        glDrawElements(GL_TRIANGLES, obj3D.numIndices, GL_UNSIGNED_SHORT, obj3D.indices);
        
        SampleApplicationUtils::checkGlError("FrameMarkerss renderFrameQCAR");
    }
    
    for (int i = 0; i<4; i++) {
        [self updateMarkersInfo: i :detected[i] :jx[i] :jy[i] :distance[i] :cam_z[i] :target_pose_y[i] :target_pose_z[i]];
    }
    
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glDisableVertexAttribArray(vertexHandle);
    glDisableVertexAttribArray(normalHandle);
    glDisableVertexAttribArray(textureCoordHandle);
    
    if (takeScreenshot) {
        [self saveScreenShot: 0 :0 :self.frame.size.width : self.frame.size.height :@"screenshot.png"];
        takeScreenshot = false;
    }
    
    QCAR::Renderer::getInstance().end();
    [self presentFramebuffer];


}

//------------------------------------------------------------------------------
#pragma mark - OpenGL ES management

- (void)initShaders
{
    shaderProgramID = [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"Simple.vertsh"
                                                   fragmentShaderFileName:@"Simple.fragsh"];

    if (0 < shaderProgramID) {
        vertexHandle = glGetAttribLocation(shaderProgramID, "vertexPosition");
        normalHandle = glGetAttribLocation(shaderProgramID, "vertexNormal");
        textureCoordHandle = glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle  = glGetUniformLocation(shaderProgramID,"texSampler2D");
    }
    else {
        NSLog(@"Could not initialise augmentation shader");
    }
}


- (void)createFramebuffer
{
    if (context) {
        // Create default framebuffer object
        glGenFramebuffers(1, &defaultFramebuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        
        // Create colour renderbuffer and allocate backing store
        glGenRenderbuffers(1, &colorRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        
        // Allocate the renderbuffer's storage (shared with the drawable object)
        [context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(CAEAGLLayer*)self.layer];
        GLint framebufferWidth;
        GLint framebufferHeight;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &framebufferWidth);
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &framebufferHeight);
        
        // Create the depth render buffer and allocate storage
        glGenRenderbuffers(1, &depthRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, framebufferWidth, framebufferHeight);
        
        // Attach colour and depth render buffers to the frame buffer
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbuffer);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);
        
        // Leave the colour render buffer bound so future rendering operations will act on it
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
    }
}


- (void)deleteFramebuffer
{
    if (context) {
        [EAGLContext setCurrentContext:context];
        
        if (defaultFramebuffer) {
            glDeleteFramebuffers(1, &defaultFramebuffer);
            defaultFramebuffer = 0;
        }
        
        if (colorRenderbuffer) {
            glDeleteRenderbuffers(1, &colorRenderbuffer);
            colorRenderbuffer = 0;
        }
        
        if (depthRenderbuffer) {
            glDeleteRenderbuffers(1, &depthRenderbuffer);
            depthRenderbuffer = 0;
        }
    }
}


- (void)setFramebuffer
{
    // The EAGLContext must be set for each thread that wishes to use it.  Set
    // it the first time this method is called (on the render thread)
    if (context != [EAGLContext currentContext]) {
        [EAGLContext setCurrentContext:context];
    }
    
    if (!defaultFramebuffer) {
        // Perform on the main thread to ensure safe memory allocation for the
        // shared buffer.  Block until the operation is complete to prevent
        // simultaneous access to the OpenGL context
        [self performSelectorOnMainThread:@selector(createFramebuffer) withObject:self waitUntilDone:YES];
    }
    
    glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
}


- (BOOL)presentFramebuffer
{
    // setFramebuffer must have been called before presentFramebuffer, therefore
    // we know the context is valid and has been set for this (render) thread
    
    // Bind the colour render buffer and present it
    glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
    
    return [context presentRenderbuffer:GL_RENDERBUFFER];
}

- (void)updateMarkersInfo:(int)markerId :(bool)detected :(int)i1 :(int)i2 :(float)dist :(float)z :(float)tpx :(float)tpz {
    targetX[markerId] = i1;
    targetY[markerId] = i2;
    targetOrientation[markerId] = (float) ((asin(z)/M_PI)*180.0);
    targetDist[markerId] = dist;
    targetPoseX[markerId] = tpx;
    targetPoseZ[markerId] = tpz;
    angleTargetRobot[markerId] = (float) ((atan2(tpx, tpz)/M_PI)*180.0) + targetOrientation[markerId];
    
    
    
    if(!detected) {
        targetDetectedInfo[markerId] = NO_TARGET_FOUND;

        //switchToLittleTag[markerId] = false;	// if we lose the big target and some time is needed to detect the small target then we don't stop
        // if we set this flag to false!
    } else {
        targetDetectedInfo[markerId] = TARGET_FOUND;
        mLastDetectedMarkerId = markerId;
        
        if(targetDistPrev[markerId]==0) {	// at the beginning we don't have a previous distance value, so initializes it with the current distance
            targetDistPrev[markerId] = targetDist[markerId];
        }
        
        if((targetDist[markerId] - targetDistPrev[markerId]) > DIST_DIFF_THR) {	// from big to small
            switchToLittleTag[markerId] = true;
        } else if((targetDist[markerId] - targetDistPrev[markerId]) < -DIST_DIFF_THR) {	// from small to big
            switchToLittleTag[markerId] = false;
        }
        targetDistPrev[markerId] = targetDist[markerId];
        
    }
    if(markerId >= 2) {	// only for targets that aren't on docking station
        if(markerId==mCurrentTargetId) {
            if(targetDetectedInfo[markerId]==NO_TARGET_FOUND) {
                NSTimeInterval interval = [self.mTimestampLastSeen timeIntervalSinceNow] * 1000;
                if((abs(interval) > 500) && mSeenOnce==true) {
                    mIsExploring = true;
                    if(lastRotation == ROT_LEFT) {
                        mDesiredRotation = -4;
                    } else {
                        mDesiredRotation = 4;
                    }
                    mDesiredAcceleration = 0;
                }
                if (abs(interval) > TARGET_TIMEOUT && globalState!=STATE_WAITING_START) {
                    [self startExploring];
                    [self goToNextTarget];
                }
            } else {
                mSeenOnce = true;
                mIsExploring = false;
                // Target is/was visible, advance while trying to reach it:
                // Set the rotation on a range between -1 and 1:
                mDesiredRotation = (2 * ((double)targetX[markerId] / (double)self.frame.size.width)) - 1;
                mDesiredAcceleration = 1;
                
                self.mTimestampLastSeen = [NSDate date];
            }
        }
    }
    
}

- (void)startExploring {
    mIsExploring = true;
    mDesiredRotation = -5;
    mDesiredAcceleration = 0;
}

-(void)goToNextTarget {
    self.mGlobalTimestamp = [NSDate date];
    prevGlobalState = globalState;
    if(mCurrentTargetId == (NUM_TARGETS-1)) {	// all target points reached, go to docking station
        mCurrentScreenshot = 1;
        mCurrentTargetId = 1;	// set it to "first target id - 1" because we take a screenshot also after being docked
        // thus the mCurrentTargetId is incremented by 1
        globalState = STATE_DOCKING_SEARCH;
        lastRotation = ROT_LEFT;
        invertRotation = true;
        rotCounter = 0;
        rotationInProgress = false;
        self.mTimestampDocking = [NSDate date];
    } else {
        mCurrentScreenshot++;
        mCurrentTargetId = mCurrentTargetId + 1;
        rotCounter = 0;
        rotationInProgress = false;
        globalState = STATE_TARGET_SEARCH_AND_APPROACH;
        [self startExploring];
        switchToLittleTag[mCurrentTargetId] = false;
        self.mTimestampLastSeen = [NSDate date];
        mSeenOnce = false;
    }
    
    targetDistPrev[mCurrentTargetId] = 0;
    
    if(mCurrentScreenshot == 2
       ) {
        [self rotataImages];
    }
}

-(void)calculateSpeeds {
    //angularAcc should come in a range from -1 to +1
    double angularAcc = mDesiredRotation;
    
    //Spring damping system (should always be between -1 and 1)
    double linearAcc = mDesiredAcceleration;
    
    mIsAvoiding = false;

    if (!mIsExploring && self.mObstacleLastSeenTimestamp!= nil) {
        NSTimeInterval timeInterval = [self.mObstacleLastSeenTimestamp timeIntervalSinceNow] * 1000;
        if (abs(timeInterval) < 500) { //300) {
            //seeing or recently saw an obstacle: use obstacle avoidance rotation and acceleration values
            angularAcc = mObstacleDesiredRotation;
            linearAcc = mObstacleDesiredAcceleration;
            mIsAvoiding = true;
        }
    }
    
    mLeftSpeed = (int)(mLeftSpeed
                       +   linearAcc * LINEAR_SPRING_CONST
                       +	angularAcc * ANGULAR_SPRING_CONST
                       -	DUMPING_FACTOR * mLeftSpeed);
    mRightSpeed = (int)(mRightSpeed
                        +   linearAcc * LINEAR_SPRING_CONST
                        -	angularAcc * ANGULAR_SPRING_CONST
                        -	DUMPING_FACTOR * mRightSpeed);
}

- (void)rotataImages {
    int k=0;
    NSFileManager *fileMgr = [NSFileManager defaultManager];
    for (k = 1; k<4; k++) {
        NSString *sourcePath = [NSHomeDirectory() stringByAppendingPathComponent:[NSString stringWithFormat: @"Documents/tour2_screenshot%d.jpg", k]];
        NSString *destPath = [NSHomeDirectory() stringByAppendingPathComponent:[NSString stringWithFormat: @"Documents/tour3_screenshot%d.jpg", k]];
        
        NSString *dataFile = [NSString stringWithContentsOfFile:sourcePath encoding:NSUTF8StringEncoding error:nil];
        [dataFile writeToFile:destPath
                   atomically:YES encoding:NSUTF8StringEncoding error:nil];
        
        NSError *error;
        BOOL success = [fileMgr removeItemAtPath:sourcePath error:&error];
    }
    for (k = 1; k<4; k++) {
        NSString *sourcePath = [NSHomeDirectory() stringByAppendingPathComponent:[NSString stringWithFormat: @"Documents/tour1_screenshot%d.jpg", k]];
        NSString *destPath = [NSHomeDirectory() stringByAppendingPathComponent:[NSString stringWithFormat: @"Documents/tour2_screenshot%d.jpg", k]];
        
        NSString *dataFile = [NSString stringWithContentsOfFile:sourcePath encoding:NSUTF8StringEncoding error:nil];
        [dataFile writeToFile:destPath
                   atomically:YES encoding:NSUTF8StringEncoding error:nil];
        
        NSError *error;
        BOOL success = [fileMgr removeItemAtPath:sourcePath error:&error];
    }
}

- (void) saveScreenShot:(int)x :(int)y :(int)w :(int)h :(NSString *)filename {
    int s = 1;
    UIScreen* screen = [ UIScreen mainScreen ];
    if ( [ screen respondsToSelector:@selector(scale) ] )
        s = (int) [ screen scale ];

    NSInteger myDataLength = w * h * 4 * s * s;
    // allocate array and read pixels into it.
    GLubyte *buffer = (GLubyte *) malloc(myDataLength);
    glReadPixels(0, 0, w * s, h * s, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    // gl renders "upside down" so swap top to bottom into new array.
    // there's gotta be a better way, but this works.
    GLubyte *buffer2 = (GLubyte *) malloc(myDataLength);
    for(int yy = 0; yy < h * s; yy++)
    {
        for(int xx = 0; xx < w * 4 * s; xx++)
        {
            buffer2[(h * s -1 - yy) * w * s * 4 + xx] = buffer[yy * 4 * w * s + xx];
        }
    }
    // make data provider with data.
    CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, buffer2, myDataLength, NULL);
    // prep the ingredients
    int bitsPerComponent = 8;
    int bitsPerPixel = w/10;
    int bytesPerRow = 4 * w * s;
    CGColorSpaceRef colorSpaceRef = CGColorSpaceCreateDeviceRGB();
    CGBitmapInfo bitmapInfo = kCGBitmapByteOrderDefault;
    CGColorRenderingIntent renderingIntent = kCGRenderingIntentDefault;
    
    // make the cgimage
    CGImageRef imageRef = CGImageCreate(w * s, h * s, bitsPerComponent, bitsPerPixel, bytesPerRow, colorSpaceRef, bitmapInfo, provider, NULL, NO, renderingIntent);
    // then make the uiimage from that
    UIImage *myImage = [UIImage imageWithCGImage:imageRef];
    // Create paths to output images
    NSString *jpgPath = [NSHomeDirectory() stringByAppendingPathComponent:[NSString stringWithFormat: @"Documents/tour1_screenshot%d.jpg", mCurrentScreenshot]];
    
    
    // Write a UIImage to JPEG with minimum compression (best quality)
    // The value 'image' must be a UIImage object
    // The value '1.0' represents image compression quality as value from 0.0 to 1.0
    
    // Write image to PNG
    [UIImageJPEGRepresentation(myImage, 1.0) writeToFile:jpgPath atomically:YES];
    
    }

-(void) calibrateSensors {
    [self.wheelphone calibrateSensors];
}

-(void) startButtonTapped {
    [[NSNotificationCenter defaultCenter] addObserver: self selector: @selector(robotUpdateNotification:) name: @"WPUpdate" object: nil];
}


@end

