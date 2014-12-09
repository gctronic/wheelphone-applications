//
//  ViewController.m
//  Move
//
//  Created by Margarita Grinvald on 22/02/14.
//  Copyright (c) 2014 Universita della Svizzera Italiana (ITC). All rights reserved.
//

#import "ViewController.h"
#import "AppDelegate.h"
#define MIN(A,B) ((A)<(B)?(A):(B))
#define MAX(A,B) ((A)>(B)?(A):(B))
#define SET_IN_RANGE(MN,V,MX) (MAX((MN),(MIN((MX),(V)))))
#define PRAMP(ZERO,V,ONE) ((V)<(ZERO)?0.0:((V)>(ONE)?1.0:(((V)-(ZERO))/((ONE)-(ZERO)))))
#define NRAMP(ZERO,V,ONE) ((V)>(ZERO)?0.0:((V)<(ONE)?1.0:(((ZERO)-(V))/((ZERO)-(ONE)))))
#define RAMP(ZERO,V,ONE) ((ZERO)<(ONE)?PRAMP((ZERO),(V),(ONE)):NRAMP((ZERO),(V),(ONE)))

@implementation ViewController

- (IBAction)run:(id)sender {
    int selectedModeIndex = self.segmentedControl.selectedSegmentIndex;
    double distance = 400.0;
    [super viewDidLoad];
    
    if (selectedModeIndex == 0) {
        int speed = 40;
        int diameter = 90;
        double distMov = distance;
        double distRot= M_PI * diameter / 2.0;
        double timeMov = distMov/speed;
        double timeRot = distRot/speed;
        
        NSNumber *speedObject = [NSNumber numberWithInteger:speed];
        NSNumber *zeroObject = [NSNumber numberWithInteger:0];
        commands = [[NSMutableArray alloc] initWithCapacity:8];
        
        [commands addObject:[NSArray arrayWithObjects:speedObject,speedObject,[NSNumber numberWithDouble:timeMov], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,zeroObject,[NSNumber numberWithDouble:timeRot], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,speedObject,[NSNumber numberWithDouble:timeMov], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,zeroObject,[NSNumber numberWithDouble:timeRot], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,speedObject,[NSNumber numberWithDouble:timeMov], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,zeroObject,[NSNumber numberWithDouble:timeRot], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,speedObject,[NSNumber numberWithDouble:timeMov], nil]];
        [commands addObject:[NSArray arrayWithObjects:speedObject,zeroObject,[NSNumber numberWithDouble:timeRot], nil]];
        
        [self nextAction];
    } else {
        self.gyro = false;
        self.refAttitude = nil;
        self.angle = 0;
        
        if (selectedModeIndex == 2 || selectedModeIndex == 3) {
            self.gyro = true;
        }
        
        if (selectedModeIndex == 3) {
            self.localization = true;
        }
        if (self.localization) {
            int sign = -1;
            commandQueue = [[Queue alloc] init];
            
            Command *command = [[Command alloc] init];
            command.x = distance;
            command.y = 0;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.x = distance;
            command.y = sign * distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.x = 0;
            command.y = sign * distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.x = 0;
            command.y = 0;
            [commandQueue addObject: command];
            
            [commandQueue execute];
            
        } else {
            int sign = -1;
            commandQueue = [[Queue alloc] init];
            
            Command *command = [[Command alloc] init];
            command.angle = 0;
            command.distance = distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 90;
            command.distance = 0;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 90;
            command.distance = distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 180;
            command.distance = 0;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 180;
            command.distance = distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 270;
            command.distance = 0;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 270;
            command.distance = distance;
            [commandQueue addObject: command];
            
            command = [[Command alloc] init];
            command.angle = sign * 360;
            command.distance = 0;
            [commandQueue addObject: command];
            
            [commandQueue execute];
        }
    }
}

- (IBAction)stop:(id)sender {
    [self.currentTimer invalidate];
    [commands removeAllObjects];
    [commandQueue clear];
    self.refAttitude = nil;
    
    [self.robot setLeftSpeed:0];
    [self.robot setRightSpeed:0];
    
}

- (void)viewDidLoad {
    self.localizationSwitch.on = false;
    
}
-(void)nextAction {
    if ([commands count]) {
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, 0.5 * NSEC_PER_SEC);
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [self runAction:[[[commands objectAtIndex:0] objectAtIndex:0] integerValue] :[[[commands objectAtIndex:0] objectAtIndex:1] integerValue] :[[[commands objectAtIndex:0] objectAtIndex:2] doubleValue]];
        });
    }
}

-(void)runAction:(int)leftSpeed:(int)rightSpeed:(double)time {
    [self.robot setLeftSpeed:leftSpeed];
    [self.robot setRightSpeed:rightSpeed];
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, time * NSEC_PER_SEC);
    dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
        [self.robot setLeftSpeed:0];
        [self.robot setRightSpeed:0];
        if ([commands count]) {
            [commands removeObjectAtIndex:0];
            [self nextAction];
        }
    });
}

- (void)dealloc {
    [_segmentedControl release];
    [_runButton release];
    [_stopButton release];
    [_localizationSwitch release];
    [super dealloc];
}

@end

@implementation Command
- (id)init {
    self=[super init];
    viewController =(ViewController *)((AppDelegate*)[[UIApplication sharedApplication]delegate]).window.rootViewController;
    motionManager = viewController.motionManager;
    robot = viewController.robot;
    self.distance = 0.0;
    self.angle = 0.0;
    self.x = 0.0;
    self.y = 0.0;
    self.scalarDistance = 1.0/2.0;
    self.scalarAngle = 1.0/1.0;
    return self;
}
-(void)run {
    viewController.time = [NSDate date];
    if (!viewController.localization) {
        [robot setOdometryx:0.0 y:0.0 theta: [robot getOdometryTheta]];
        viewController.currentTimer = [NSTimer scheduledTimerWithTimeInterval: 1.0/20.0 target: self selector:@selector(doSomething:) userInfo: nil repeats:YES];
    } else {
        viewController.currentTimer = [NSTimer scheduledTimerWithTimeInterval: 1.0/20.0 target: self selector:@selector(doSomethingLocalization:) userInfo: nil repeats:YES];
    }
    
}
-(void) doSomething: (NSTimer*)timer {
    if (viewController.refAttitude == nil) {
        NSLog(@"BUAHAHAH");
        viewController.refAttitude = motionManager.deviceMotion.attitude;
        
        [robot setOdometryx:0.0 y:0.0 theta: 0.0];
        [robot resetOdometry];
        viewController.angle = 0;
    }
    
    NSTimeInterval timeInterval = [viewController.time timeIntervalSinceNow];
    viewController.time = [NSDate date];
    double angle = [robot getOdometryTheta];
    viewController.x = [robot getOdometryX];
    viewController.y = [robot getOdometryY];
    
    if (viewController.gyro) {
        currentAttitude = motionManager.deviceMotion.attitude;
        [currentAttitude multiplyByInverseOfAttitude: viewController.refAttitude];
        
        angle = currentAttitude.roll;
        
        int leftSpeed = [robot getLeftSpeed];
        int rightSpeed = [robot getRightSpeed];
        double R = (9.0/2.0) * ((leftSpeed + rightSpeed) / (rightSpeed - leftSpeed));
        double w = (rightSpeed - leftSpeed)/9.0;
        double ICCx = viewController.x - R * sin(angle);
        double ICCy = viewController.y + R * cos(angle);
        double x = cos(w * (-timeInterval)) * (viewController.x - ICCx) - sin(w * (-timeInterval)) * (viewController.y - ICCy) + ICCx;
        double y = sin(w * (-timeInterval)) * (viewController.x - ICCx) + cos(w * (-timeInterval)) * (viewController.y - ICCy) + ICCy;
        angle = angle * 180/M_PI;
        viewController.x = x;
        viewController.y = y;
        
        viewController.angle = viewController.angle * 180/M_PI;
        
        if (viewController.angle >= 0) {
            if (angle >= 0) {
                if (viewController.angle > 350) {
                    viewController.angle = 360 + angle;
                } else {
                    viewController.angle = angle;
                }
                
            } else {
                if (viewController.angle > 170) {
                    viewController.angle = 360 + angle;
                } else {
                    viewController.angle = angle;
                }
            }
        } else {
            if (angle <= 0) {
                if (viewController.angle < -350) {
                    viewController.angle = -360 + angle;
                } else {
                    viewController.angle = angle;
                }
                
            } else {
                if (viewController.angle < -170) {
                    viewController.angle = -360 + angle;
                } else {
                    viewController.angle = angle;
                }
            }
        }
        viewController.angle = viewController.angle * M_PI/180;
        
    } else {
        viewController.angle = angle;
    }
    
    double rotError = self.angle - (viewController.angle * 180/M_PI);
    double measuredDist = sqrt(pow(viewController.x,2) + pow(viewController.y,2));
    double movError = 0;
    if (self.distance > 0) {
        movError = self.distance - measuredDist;
    }
    NSLog(@"Measured distance: %g", measuredDist);
    NSLog(@"Mov error: %g", movError);
    NSLog(@"Roll: %f", viewController.angle * 180/M_PI);
    NSLog(@"Rot error: %f", rotError);
    
    if ((movError < 3 && movError !=0 && rotError > - 1 && rotError < 1) || (movError == 0 && rotError > - 6 && rotError < 6 )){
        [robot setLeftSpeed:0];
        [robot setRightSpeed:0];
        [timer invalidate];
        timer = nil;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, 0.5 * NSEC_PER_SEC);
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [self.queue takeObject];
            NSLog(@"BOOOHHH %f", 3.0);
            if (![self.queue next]) {
                NSLog(@"RESET");
                viewController.refAttitude = nil;
            }
        });
    } else {
        int rightSpeed = 2;
        int leftSpeed = 2;
        if (movError == 0) {
            if (rotError > 0) {
                rightSpeed += abs((int)lround(rotError * self.scalarAngle));
                if (rightSpeed < 10) {
                    rightSpeed = 10;
                }
            } else {
                leftSpeed += abs((int)lround(rotError * self.scalarAngle));
                if (leftSpeed<10) {
                    leftSpeed = 10;
                }
            }
        } else {
            if (rotError > 0) {
                rightSpeed += abs((int)lround(rotError));
            } else {
                leftSpeed += abs((int)lround(rotError));
            }
        }
        
        if (movError > 10) {
            rightSpeed += (int)movError * self.scalarDistance;
            leftSpeed += (int) movError * self.scalarDistance;
        }
        
        NSLog(@"leftSpeed: %d", leftSpeed);
        NSLog(@"rightSpeed: %d", rightSpeed);
        [robot setLeftSpeed: leftSpeed];
        [robot setRightSpeed: rightSpeed];
    }
}

-(void) doSomethingLocalization: (NSTimer*)timer {
    if (viewController.refAttitude == nil) {
        
        [robot setOdometryx:0.0 y:0.0 theta: 0.0];
        [robot resetOdometry];
        viewController.refAttitude = motionManager.deviceMotion.attitude;
        viewController.angle = 0;
    }
    
    NSTimeInterval timeInterval = [viewController.time timeIntervalSinceNow];
    viewController.time = [NSDate date];
    
    
    double angle = [robot getOdometryTheta];
    
    viewController.angle = viewController.angle * 180/M_PI;
    if (viewController.gyro) {
        currentAttitude = motionManager.deviceMotion.attitude;
        [currentAttitude multiplyByInverseOfAttitude: viewController.refAttitude];
        
        angle = currentAttitude.roll;
    }
    
    NSLog(@"Current attitude: %f", angle * 180/M_PI);
    int leftSpeed = [robot getLeftSpeed];
    int rightSpeed = [robot getRightSpeed];
    double R = (9.0/2.0) * ((leftSpeed + rightSpeed) / (rightSpeed - leftSpeed));
    double w = (rightSpeed - leftSpeed)/9.0;
    double ICCx = viewController.x - R * sin(angle);
    double ICCy = viewController.y + R * cos(angle);
    double x = cos(w * (-timeInterval)) * (viewController.x - ICCx) - sin(w * (-timeInterval)) * (viewController.y - ICCy) + ICCx;
    double y = sin(w * (-timeInterval)) * (viewController.x - ICCx) + cos(w * (-timeInterval)) * (viewController.y - ICCy) + ICCy;
    viewController.x = x;
    viewController.y = y;
    viewController.angle = angle;
    
    NSLog(@"calculated X :%g", viewController.x);
    NSLog(@"calculated Y :%g", viewController.y);
    
    double alpha = atan2(cos(angle) * (self.y - viewController.y) - sin(angle) * (self.x - viewController.x), cos(angle) * (self.x - viewController.x) + sin(angle)* (self.y - viewController.y)) * 180/M_PI;
    
    double rho = sqrt(pow(fabs(self.x - viewController.x),2) + pow(fabs(self.y - viewController.y),2));
    double v = 0.0;
    w = 0.0;
    if (rho > 0) {
        v = 50.0*RAMP(90.0,fabs(alpha),5.0);
    }
    if (rho > 10) {
        w = 1 * SET_IN_RANGE(-15,alpha,15);
    }
    NSLog(@"Alpha %g", alpha);
    NSLog(@"Rho %g", rho);
    NSLog(@"linear speed: %g", v);
    NSLog(@"rotational speed: %g", w);
    
    rightSpeed = v + w;;
    leftSpeed = v - w;
    
    NSLog(@"leftSpeed: %d", leftSpeed);
    NSLog(@"rightSpeed: %d", rightSpeed);
    [robot setLeftSpeed: leftSpeed];
    [robot setRightSpeed: rightSpeed];
    
    if (rho < 20) {
        [robot setLeftSpeed:0];
        [robot setRightSpeed:0];
        [timer invalidate];
        timer = nil;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, 0.5 * NSEC_PER_SEC);
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [self.queue takeObject];
            if (![self.queue next]) {
                viewController.refAttitude = nil;
            }
        });
    }
    
}
@end


@implementation Queue
- (id)init {
    if (self=[super init]) {
        self.objects=[[NSMutableArray alloc] init];
    }
    return self;
}
- (void)addObject:(id)object {
    [self.objects addObject:object];
    ((Command *)object).queue = self;
}
- (id)takeObject {
    id object=nil;
    if ([self.objects count]) {
        object=[self.objects objectAtIndex:0];
        [object retain];
        [self.objects removeObjectAtIndex:0];
    }
    return [object autorelease];
}

-(void)clear {
    [self.objects removeAllObjects];
}

-(void)execute {
    [self next];
}

-(bool)next {
    if ([self.objects count]) {
        [(Command *)[self.objects objectAtIndex:0] run];
        return true;
    }
    return false;
}
@end
