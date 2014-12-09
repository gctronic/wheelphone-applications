//
//  ViewController_h.h
//  Move
//
//  Created by Margarita Grinvald on 22/02/14.
//  Copyright (c) 2014 Universita della Svizzera Italiana (ITC). All rights reserved.
//

#import <UIKit/UIKit.h>
#import <CoreMotion/CoreMotion.h>
#import <AudioToolbox/AudioToolbox.h>
#import <ios-wheelphone-library/WheelphoneRobot.h>

@interface Queue:NSObject
{
    
}
@property (strong, nonatomic) NSMutableArray* objects;
- (void)addObject:(id)object;
- (id)takeObject;
- (void)execute;
- (bool)next;
- (void)clear;
@end

@interface ViewController : UIViewController<UIAccelerometerDelegate> {
    NSMutableArray * commands;
    Queue * commandQueue;
}
@property (retain, nonatomic) IBOutlet UISwitch *localizationSwitch;
@property (retain, nonatomic) IBOutlet UISegmentedControl *segmentedControl;
@property (retain, nonatomic) IBOutlet UIButton *stopButton;
@property (retain, nonatomic) IBOutlet UIButton *runButton;
@property (strong, nonatomic) CMMotionManager *motionManager;
@property (strong, nonatomic) WheelphoneRobot *robot;
@property (strong, nonatomic) NSTimer * currentTimer;
@property (strong, nonatomic) CMAttitude *refAttitude;
@property (strong, nonatomic) NSDate *time;
@property (nonatomic) double angle;
@property (nonatomic) double x;
@property (nonatomic) double y;
@property (nonatomic) bool gyro;
@property (nonatomic) bool localization;

@end

@interface Command:NSObject {
    ViewController *viewController;
    WheelphoneRobot *robot;
    CMMotionManager *motionManager;
    CMAttitude *currentAttitude;
}
@property (strong, nonatomic) Queue *queue;
@property (nonatomic) double distance;
@property (nonatomic) double angle;
@property (nonatomic) double x;
@property (nonatomic) double y;
@property (nonatomic) double scalarDistance;
@property (nonatomic) double scalarAngle;
-(void)run;
-(void)doSomething:(NSTimer*)timer;
@end