//
//  AppDelegate.h
//  Move
//
//  Created by Margarita Grinvald on 21/02/14.
//  Copyright (c) 2014 Universita della Svizzera Italiana (ITC). All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreMotion/CoreMotion.h>
#import <ios-wheelphone-library/WheelphoneRobot.h>
@class ViewController;


@interface AppDelegate : UIResponder <UIApplicationDelegate> {
    CMMotionManager *motionManager;
    WheelphoneRobot* robot;
}

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) ViewController *viewController;

@end


