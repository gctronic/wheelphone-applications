//
//  FirstViewController.h
//  WheelphoneMonitor
//
//  Created by Stefano Morgani on 14.05.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <UIKit/UIKit.h>
#include <AVFoundation/AVFoundation.h>
#include <MediaPlayer/MediaPlayer.h>
#import <ios-wheelphone-library/WheelphoneRobot.h>


#define DEBUG_PRINT 0

#define LINE_SEARCH 0
#define LINE_FOLLOW 1
#define AVOID_OBJECT 2
#define PIVOT_ROTATION 3
#define INIT_GROUND_THR 180
#define INIT_SPEED 10
#define INIT_LOST_THR 15
#define MAX_SPEED 350
#define OBJECT_THR 35

@interface FirstViewController : UIViewController <UITextFieldDelegate, AVAudioPlayerDelegate> {
    
    // LINE  FOLLOWING
    BOOL isFollowing;
    char globalState;
    int desiredSpeed;
    int groundThreshold;
    int lineFound;
    int lineLostThr;
    int outOfLine;
    int tempSpeed;
    int minSpeedLineFollow;
    BOOL isNearObject;
    NSDate *startAvoidTime;
    NSDate *stopAvoidTime;
    NSTimeInterval avoidTime;
    char globalStatePrev;
    int sameObjDetectCount;
    
    // CLIFF DETECTION
    BOOL isCliffDetecting;
    
    // VARIOUS
    WheelphoneRobot* robot;    
    int lSpeed;
    int rSpeed;
    int lSpeedPrev;
    int rSpeedPrev;
    int robGroundValues[4];
    int robProxValues[4];
    int robProxAmbValues[4];
    int robBattery;
    int robFlagsRobotToPhone;
    int robLeftSpeed;
    int robRightSpeed;

}

- (void)updateUI;
- (void)executeBehaviors;
- (IBAction)calibrateSensors:(id)sender;
- (IBAction)fwTapped:(id)sender;
@property (weak, nonatomic) IBOutlet UITextField *lineSpeedTxt;
@property (weak, nonatomic) IBOutlet UITextField *threshold;
@property (weak, nonatomic) IBOutlet UITextField *lineLostTx;
@property (nonatomic, retain) IBOutlet UIProgressView *prox0;
@property (nonatomic, retain) IBOutlet UIProgressView *prox1;
@property (nonatomic, retain) IBOutlet UIProgressView *prox2;
@property (nonatomic, retain) IBOutlet UIProgressView *prox3;
@property (weak, nonatomic) IBOutlet UIProgressView *proxAmb0;
@property (weak, nonatomic) IBOutlet UIProgressView *proxAmb1;
@property (weak, nonatomic) IBOutlet UIProgressView *proxAmb2;
@property (weak, nonatomic) IBOutlet UIProgressView *proxAmb3;
@property (weak, nonatomic) IBOutlet UIProgressView *ground0;
@property (weak, nonatomic) IBOutlet UIProgressView *ground1;
@property (weak, nonatomic) IBOutlet UIProgressView *ground2;
@property (weak, nonatomic) IBOutlet UIProgressView *ground3;
@property (weak, nonatomic) IBOutlet UILabel *leftSpeed;
@property (weak, nonatomic) IBOutlet UILabel *rightSpeed;
@property (weak, nonatomic) IBOutlet UILabel *prox0txt;
@property (weak, nonatomic) IBOutlet UILabel *prox1txt;
@property (weak, nonatomic) IBOutlet UILabel *prox2txt;
@property (weak, nonatomic) IBOutlet UILabel *prox3txt;
@property (weak, nonatomic) IBOutlet UILabel *prox0AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *prox1AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *prox2AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *prox3AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *ground0txt;
@property (weak, nonatomic) IBOutlet UILabel *ground1txt;
@property (weak, nonatomic) IBOutlet UILabel *ground2txt;
@property (weak, nonatomic) IBOutlet UILabel *ground3txt;
@property (weak, nonatomic) IBOutlet UILabel *batteryValueTxt;
@property (weak, nonatomic) IBOutlet UILabel *batteryStatusTxt;
@property (weak, nonatomic) IBOutlet UIButton *btnFollowing;
@property (weak, nonatomic) IBOutlet UILabel *behaviorStat;
@property (weak, nonatomic) IBOutlet UIButton *btnCliffDetection;


@end
