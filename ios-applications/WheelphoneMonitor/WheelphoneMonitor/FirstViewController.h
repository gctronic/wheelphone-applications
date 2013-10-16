//
//  FirstViewController.h
//  WheelphoneMonitor
//
//  Created by Stefano Morgani on 14.05.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <UIKit/UIKit.h>
#include <AVFoundation/AVFoundation.h>
#import "AQRecorder.h"

#define DEBUG_PRINT 0

#define DTMF_0 0
#define DTMF_1 1
#define DTMF_2 2
#define DTMF_3 3
#define DTMF_4 4
#define DTMF_5 5
#define DTMF_6 6
#define DTMF_7 7
#define DTMF_8 8
#define DTMF_9 9
#define DTMF_STAR 10
#define DTMF_HASH 11
#define DTMF_SPEED_STEP 1

@interface FirstViewController : UIViewController <UITextFieldDelegate, AVAudioPlayerDelegate> {
    
    AQRecorder* recorder;
    AVAudioPlayer *testAudioPlayer;
    
    int currentLeftSpeed;
    int currentRightSpeed;
    int desiredLeftSpeed;
    int desiredRightSpeed;
    NSString *audioFilePath[12];
    NSURL *audioFileURL[12];
    
    int counter;
    int tempSpeed;
    int robotR;
    int robotL;
    BOOL played;
    
    //SystemSoundID _fwSound;

}

- (void)registerForBackgroundNotifications;
- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag;

- (IBAction)fwTapped:(id)sender;
@property (weak, nonatomic) IBOutlet UITextField *lineSpeedTxt;

@property (weak, nonatomic) IBOutlet UITextField *threshold;
@property (weak, nonatomic) IBOutlet UITextField *lineLostTx;
@property (readonly) AQRecorder *recorder;
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
