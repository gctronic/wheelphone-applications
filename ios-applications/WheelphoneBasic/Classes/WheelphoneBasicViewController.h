//
//  WheelphoneBasicViewController.h
//  WheelphoneBasic
//
//  Created by Stefano Morgani on 3/13/13.
//  Copyright Stefano Morgani 2013. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AudioToolbox/AudioToolbox.h>

@interface WheelphoneBasicViewController : UIViewController {
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

