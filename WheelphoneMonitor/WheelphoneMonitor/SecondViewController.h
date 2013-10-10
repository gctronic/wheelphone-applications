//
//  SecondViewController.h
//  WheelphoneMonitor
//
//  Created by Stefano Morgani on 14.05.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>

@interface SecondViewController : UIViewController {
	SystemSoundID _fwSound;
    SystemSoundID _bwSound;
    SystemSoundID _leftSound;
    SystemSoundID _rightSound;
    SystemSoundID _stopSound;
	SystemSoundID _calibrateSound;
    SystemSoundID _speedControlSound;
    SystemSoundID _softAccSound;
    SystemSoundID _avoidObstacleSound;
    SystemSoundID _avoidCliffSound;
}

- (IBAction)fwTapped:(id)sender;
- (IBAction)bwTapped:(id)sender;
- (IBAction)leftTapped:(id)sender;
- (IBAction)rightTapped:(id)sender;
- (IBAction)stopTapped:(id)sender;
- (IBAction)calibrateTapped:(id)sender;
- (IBAction)speedControlTapped:(id)sender;
- (IBAction)softAccTapped:(id)sender;
- (IBAction)avoidObstacleTapped:(id)sender;
- (IBAction)avoidCliffTapped:(id)sender;

@end
