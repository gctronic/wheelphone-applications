//
//  FirstViewController.h
//  WheelphoneMonitor
//
//  Created by Stefano Morgani on 14.05.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <UIKit/UIKit.h>
//#import "AQRecorder.h"

@interface FirstViewController : UIViewController {

    //AQRecorder*					recorder;
    
}
//- (void)registerForBackgroundNotifications;

//@property (readonly)			AQRecorder			*recorder;

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
@property (weak, nonatomic) IBOutlet UIProgressView *groundAmb0;
@property (weak, nonatomic) IBOutlet UIProgressView *groundAmb1;
@property (weak, nonatomic) IBOutlet UIProgressView *groundAmb2;
@property (weak, nonatomic) IBOutlet UIProgressView *groundAmb3;
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
@property (weak, nonatomic) IBOutlet UILabel *ground0AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *ground1AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *ground2AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *ground3AmbTxt;
@property (weak, nonatomic) IBOutlet UILabel *batteryValueTxt;
@property (weak, nonatomic) IBOutlet UILabel *batteryStatusTxt;
@end
