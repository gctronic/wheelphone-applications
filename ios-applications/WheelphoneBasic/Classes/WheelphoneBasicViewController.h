//
//  WheelphoneBasicViewController.h
//  WheelphoneBasic
//
//  Created by Stefano Morgani on 3/13/13.
//  Copyright Stefano Morgani 2013. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>
#import <ios-wheelphone-library/WheelphoneRobot.h>


#define SPEED_STEP 10
#define MAX_SPEED 350

@interface WheelphoneBasicViewController : UIViewController {
    WheelphoneRobot* robot;
    int lSpeed;
    int rSpeed;
    int rotSpeed;
    BOOL isAvoidingObstacles;
    BOOL isAvoidingCliff;
    BOOL debug;
}
-(void)updateSpeed;
@property (retain, nonatomic) IBOutlet UILabel *txtRightSpeed;
@property (retain, nonatomic) IBOutlet UILabel *txtLeftSpeed;
- (IBAction)fwTapped:(id)sender;
- (IBAction)bwTapped:(id)sender;
- (IBAction)leftTapped:(id)sender;
- (IBAction)rightTapped:(id)sender;
- (IBAction)stopTapped:(id)sender;
- (IBAction)calibrateTapped:(id)sender;
- (IBAction)avoidObstacleTapped:(id)sender;
- (IBAction)avoidCliffTapped:(id)sender;

@end

