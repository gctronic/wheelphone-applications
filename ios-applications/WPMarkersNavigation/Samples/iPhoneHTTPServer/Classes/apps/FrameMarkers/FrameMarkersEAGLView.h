/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import <CoreMotion/CoreMotion.h>
#import "SampleApplicationSession.h"
#import <ios-wheelphone-library/WheelphoneRobot.h>

int const NUM_AUGMENTATION_TEXTURES = 4;
int const NUM_TARGETS = 4;
int const NO_INFO = 0;
int const NO_TARGET_FOUND = 1;
int const TARGET_FOUND = 2;
int const DIST_DIFF_THR = 200;
int const ROT_SPEED = 70;
int const ROT_LEFT = 0;
int const ROT_RIGHT = 1;
int const STATE_WAITING_START = 1;
int const STATE_DOCKING_SEARCH = 2;
int const STATE_DOCKING_APPROACH_BIG = 3;
int const STATE_DOCKING_APPROACH_SMALL = 4;
int const STATE_DOCKING_REACHED = 5;
int const STATE_ROBOT_CHARGING = 6;
int const STATE_ROBOT_GO_BACK_DOCKING = 7;
int const STATE_TARGET_SEARCH_AND_APPROACH = 9;
int const STATE_ROBOT_GO_BACK_TARGET = 10;
int const STATE_ROTATE_FOR_SCREENSHOT = 11;
int const STATE_WAIT_STOPPED_FOR_SCREENSHOT = 12;
int const STATE_TAKE_SCREENSHOT = 13;

int const ROBOT_NOT_CHARGING = 0;
int const ROBOT_IN_CHARGE = 1;
int const ROBOT_CHARGED = 2;
int const BASE_SPEED = 30;

int const LINEAR_SPRING_CONST = 70; //12;
int const ANGULAR_SPRING_CONST = 8; //8;
double const DUMPING_FACTOR = 1;
double const mCoordCoeff = 0.07;
double const mOrientCoeff = 0.36;
int const mChargeTime = 5;
//int const mChargeTime = 1;
bool const mContinuousMode = true;

long const TARGET_TIMEOUT = 30000;
long const DOCK_TIMEOUT = 40000;
long const GLOBAL_TIMEOUT = 120000;
bool const gyroscope = true;



// FrameMarkers is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface FrameMarkersEAGLView : UIView <UIGLViewProtocol> {
@private
    // OpenGL ES context
    EAGLContext *context;
    
    // The OpenGL ES names for the framebuffer and renderbuffers used to render
    // to this view
    GLuint defaultFramebuffer;
    GLuint colorRenderbuffer;
    GLuint depthRenderbuffer;

    // Shader handles
    GLuint shaderProgramID;
    GLint vertexHandle;
    GLint normalHandle;
    GLint textureCoordHandle;
    GLint mvpMatrixHandle;
    GLint texSampler2DHandle;
    
    // Texture used when rendering augmentation
    Texture* augmentationTexture[NUM_AUGMENTATION_TEXTURES];
    NSMutableArray *objects3D;  // objects to draw

    BOOL offTargetTrackingEnabled;
    
    SampleApplicationSession * vapp;
    
    
    float targetX[NUM_TARGETS];
    float targetY[NUM_TARGETS];
    float targetOrientation[NUM_TARGETS];
    float targetDist[NUM_TARGETS];
    float targetPoseX[NUM_TARGETS];
    float targetPoseZ[NUM_TARGETS];
    float angleTargetRobot[NUM_TARGETS];
    float targetDetectedInfo[NUM_TARGETS];
    float targetDistPrev[NUM_TARGETS];
    bool switchToLittleTag[NUM_TARGETS];
    int lastTargetX[NUM_TARGETS];
    
    int mLastDetectedMarkerId;
    int mCurrentTargetId;
    int mSeenOnce;
    int lastRotation;
    int mDesiredAcceleration;
    int globalState;
    int prevGlobalState;
    int rotCounter;
    int chargeStatus;
    int mSearchAngle;
    int baseSpeed;
    int rotationFactor;
    int dockReachedTimeout;
    int chargeCounter, instableChargeCounter;
    
    double mDesiredRotation;
    double mObstacleDesiredRotation;
	double mObstacleDesiredAcceleration;
    bool mIsExploring, mIsAvoiding;
    bool invertRotation;
    bool rotationInProgress;
    bool takeScreenshot;
    int mLeftSpeed, mRightSpeed;
    int mStopDistance, mStopDistanceTemp;
    int mCurrentScreenshot;
    
    
    double angle;
    double x,y;
    int rSpeed,lSpeed, rSpeedTemp, lSpeedTemp;
    
}

- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app;

- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;

@property (nonatomic, retain) WheelphoneRobot* wheelphone;
@property (nonatomic, retain) CMMotionManager *motionManager;
@property (nonatomic, retain) CMAttitude *refAttitude;
@property (nonatomic, retain) CMAttitude *currentAttitude;
@property (nonatomic, retain) NSDate *mTimestampLastSeen;
@property (nonatomic, retain) NSDate *mObstacleLastSeenTimestamp;
@property (nonatomic, retain) NSDate *mGlobalTimestamp;
@property (nonatomic, retain) NSDate *mTimestampDocking;
@property (nonatomic, retain) NSDate *time;

@end

