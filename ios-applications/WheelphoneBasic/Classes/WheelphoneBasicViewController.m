//
//  WheelphoneBasicViewController.m
//  WheelphoneBasic
//
//  Created by Stefano Morgani on 3/13/13.
//  Copyright Stefano Morgani 2013. All rights reserved.
//

#import "WheelphoneBasicViewController.h"
#include <sys/time.h>

@implementation WheelphoneBasicViewController

- (void)viewDidLoad {
	
	// This is the simplest way to play a sound.
	// But note with System Sound services you can only use:
	// File Formats (a.k.a. audio containers or extensions): CAF, AIF, WAV
	// Data Formats (a.k.a. audio encoding): linear PCM (such as LEI16) or IMA4
	// Sounds must be 30 sec or less
	// And only one sound plays at a time!
	NSString *soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf0" ofType:@"wav"];
	NSURL *soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_calibrateSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf2" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_fwSound);

	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf8" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_bwSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf4" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_leftSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf6" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_rightSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf5" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_stopSound);

	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf7" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_speedControlSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf9" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_softAccSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf_star" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_avoidObstacleSound);
    
	soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf_hash" ofType:@"wav"];
	soundURL = [NSURL fileURLWithPath:soundPath];
	AudioServicesCreateSystemSoundID((CFURLRef)soundURL, &_avoidCliffSound);
    
}

- (IBAction)fwTapped:(id)sender {
	AudioServicesPlaySystemSound(_fwSound);
}

- (IBAction)bwTapped:(id)sender {
	AudioServicesPlaySystemSound(_bwSound);
}

- (IBAction)leftTapped:(id)sender {
	AudioServicesPlaySystemSound(_leftSound);
}

- (IBAction)rightTapped:(id)sender {
	AudioServicesPlaySystemSound(_rightSound);
}

- (IBAction)stopTapped:(id)sender {
	AudioServicesPlaySystemSound(_stopSound);
}

- (IBAction)calibrateTapped:(id)sender {
	AudioServicesPlaySystemSound(_calibrateSound);
}

- (IBAction)speedControlTapped:(id)sender {
	AudioServicesPlaySystemSound(_speedControlSound);
}

- (IBAction)softAccTapped:(id)sender {
	AudioServicesPlaySystemSound(_softAccSound);
}

- (IBAction)avoidObstacleTapped:(id)sender {
	AudioServicesPlaySystemSound(_avoidObstacleSound);
}

- (IBAction)avoidCliffTapped:(id)sender {
	AudioServicesPlaySystemSound(_avoidCliffSound);
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (void)viewDidUnload {
}

- (void)dealloc {
	AudioServicesDisposeSystemSoundID(_fwSound);
	AudioServicesDisposeSystemSoundID(_bwSound);
    AudioServicesDisposeSystemSoundID(_leftSound);
    AudioServicesDisposeSystemSoundID(_rightSound);
    AudioServicesDisposeSystemSoundID(_stopSound);
    AudioServicesDisposeSystemSoundID(_calibrateSound);
    AudioServicesDisposeSystemSoundID(_speedControlSound);
    AudioServicesDisposeSystemSoundID(_softAccSound);
    AudioServicesDisposeSystemSoundID(_avoidObstacleSound);
    AudioServicesDisposeSystemSoundID(_avoidCliffSound);
    [super dealloc];
}


@end
