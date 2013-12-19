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

@synthesize txtLeftSpeed;
@synthesize txtRightSpeed;

- (void)viewDidLoad {
    debug = false;
    robot = [WheelphoneRobot new];
}

- (void)updateSpeed {
    if(debug) {
        printf("updateSpeed\n");
    }
    if(lSpeed >= 0) {
        [robot setLeftSpeed:lSpeed-rotSpeed];
        [txtLeftSpeed setText:[NSString stringWithFormat:@"%d", lSpeed-rotSpeed]];
    } else {
        [robot setLeftSpeed:lSpeed+rotSpeed];
        [txtLeftSpeed setText:[NSString stringWithFormat:@"%d", lSpeed+rotSpeed]];
    }
    
    if(rSpeed >= 0) {
        [robot setRightSpeed:rSpeed+rotSpeed];
        [txtRightSpeed setText:[NSString stringWithFormat:@"%d", rSpeed+rotSpeed]];
    } else {
        [robot setRightSpeed:rSpeed-rotSpeed];
        [txtRightSpeed setText:[NSString stringWithFormat:@"%d", rSpeed-rotSpeed]];
    }

}

- (IBAction)fwTapped:(id)sender {
    if(rotSpeed == 0) {
        if(lSpeed < (MAX_SPEED-SPEED_STEP)) {
            lSpeed += SPEED_STEP;
        } else {
            lSpeed = MAX_SPEED;
        }
        if(rSpeed < (MAX_SPEED-SPEED_STEP)) {
            rSpeed += SPEED_STEP;
        } else {
            rSpeed = MAX_SPEED;
        }
    } else {
        rotSpeed = 0;
    }
    [self updateSpeed];
}

- (IBAction)bwTapped:(id)sender {
    if(rotSpeed == 0) {
        if(lSpeed > (-MAX_SPEED+SPEED_STEP)) {
            lSpeed -= SPEED_STEP;
        } else {
            lSpeed = -MAX_SPEED;
        }
        if(rSpeed > (-MAX_SPEED+SPEED_STEP)) {
            rSpeed -= SPEED_STEP;
        } else {
            rSpeed = -MAX_SPEED;
        }
    } else {
        rotSpeed = 0;
    }
    [self updateSpeed];
}

- (IBAction)leftTapped:(id)sender {
    if(rotSpeed < (MAX_SPEED-SPEED_STEP)) {
        rotSpeed += SPEED_STEP;
    } else {
        rotSpeed = MAX_SPEED;
    }
    [self updateSpeed];
}

- (IBAction)rightTapped:(id)sender {
    if(rotSpeed > (-MAX_SPEED+SPEED_STEP)) {
        rotSpeed -= SPEED_STEP;
    } else {
        rotSpeed = -MAX_SPEED;
    }
    [self updateSpeed];
}

- (IBAction)stopTapped:(id)sender {
    if(debug) {
        printf("stopTapped\n");
    }
    rotSpeed = 0;
    lSpeed = 0;
    rSpeed = 0;
    [self updateSpeed];
}

- (IBAction)calibrateTapped:(id)sender {
    [robot calibrateSensors];
}

- (IBAction)avoidObstacleTapped:(id)sender {
    if(isAvoidingObstacles) {
        isAvoidingObstacles = false;
        [robot disableObstacleAvoidance];
    } else {
        isAvoidingObstacles = true;
        [robot enableObstacleAvoidance];
    }
}

- (IBAction)avoidCliffTapped:(id)sender {
    if(isAvoidingCliff) {
        isAvoidingCliff = false;
        [robot disableCliffAvoidance];
    } else {
        isAvoidingCliff = true;
        [robot enableCliffAvoidance];
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
}

- (void)viewDidUnload {
}

- (void)dealloc {
    [txtLeftSpeed release];
    [txtRightSpeed release];
    [super dealloc];
}


@end
