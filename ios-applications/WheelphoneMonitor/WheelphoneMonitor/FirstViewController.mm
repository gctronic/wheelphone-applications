//
//  FirstViewController.m
//  WheelphoneMonitor
//
//  Created by Stefano Morgani on 14.05.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import "FirstViewController.h"

@interface FirstViewController ()

@end

@implementation FirstViewController

@synthesize recorder;
@synthesize prox0;
@synthesize prox1;
@synthesize prox2;
@synthesize prox3;
@synthesize proxAmb0;
@synthesize proxAmb1;
@synthesize proxAmb2;
@synthesize proxAmb3;
@synthesize ground0;
@synthesize ground1;
@synthesize ground2;
@synthesize ground3;
@synthesize leftSpeed;
@synthesize rightSpeed;
@synthesize prox0txt;
@synthesize prox1txt;
@synthesize prox2txt;
@synthesize prox3txt;
@synthesize prox0AmbTxt;
@synthesize prox1AmbTxt;
@synthesize prox2AmbTxt;
@synthesize prox3AmbTxt;
@synthesize ground0txt;
@synthesize ground1txt;
@synthesize ground2txt;
@synthesize ground3txt;
@synthesize batteryValueTxt;
@synthesize batteryStatusTxt;
@synthesize btnFollowing;
@synthesize behaviorStat;
@synthesize threshold;
@synthesize lineLostTx;
@synthesize lineSpeedTxt;
@synthesize btnCliffDetection;

- (void)stopRecord
{
	
	recorder->StopRecord();
	
}

- (void)viewDidLoad
{
    
    printf("FirstViewController didLoad\n");
    
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.

    [[NSNotificationCenter defaultCenter] addObserver: self selector: @selector(updateSensorsNotification:) name: @"sensorsNotification" object: nil];
    
    
    CGAffineTransform transform = CGAffineTransformMakeScale(1.0f, 3.0f);
    prox0.transform = transform;
    prox1.transform = transform;
    prox2.transform = transform;
    prox3.transform = transform;
    proxAmb0.transform = transform;
    proxAmb1.transform = transform;
    proxAmb2.transform = transform;
    proxAmb3.transform = transform;
    ground0.transform = transform;
    ground1.transform = transform;
    ground2.transform = transform;
    ground3.transform = transform;
    
    threshold.delegate = self;
    lineLostTx.delegate = self;
    lineSpeedTxt.delegate = self;
    
    audioFilePath[DTMF_0] = [[NSBundle mainBundle] pathForResource:@"dtmf0" ofType:@"wav"];
    audioFilePath[DTMF_1] = [[NSBundle mainBundle] pathForResource:@"dtmf1" ofType:@"wav"];
    audioFilePath[DTMF_2] = [[NSBundle mainBundle] pathForResource:@"dtmf2" ofType:@"wav"];
    audioFilePath[DTMF_3] = [[NSBundle mainBundle] pathForResource:@"dtmf3" ofType:@"wav"];
    audioFilePath[DTMF_4] = [[NSBundle mainBundle] pathForResource:@"dtmf4" ofType:@"wav"];
    audioFilePath[DTMF_5] = [[NSBundle mainBundle] pathForResource:@"dtmf5" ofType:@"wav"];
    audioFilePath[DTMF_6] = [[NSBundle mainBundle] pathForResource:@"dtmf6" ofType:@"wav"];
    audioFilePath[DTMF_7] = [[NSBundle mainBundle] pathForResource:@"dtmf7" ofType:@"wav"];
    audioFilePath[DTMF_8] = [[NSBundle mainBundle] pathForResource:@"dtmf8" ofType:@"wav"];
    audioFilePath[DTMF_9] = [[NSBundle mainBundle] pathForResource:@"dtmf9" ofType:@"wav"];
    audioFilePath[DTMF_STAR] = [[NSBundle mainBundle] pathForResource:@"dtmf_star" ofType:@"wav"];
    audioFilePath[DTMF_HASH] = [[NSBundle mainBundle] pathForResource:@"dtmf_hash" ofType:@"wav"];
    audioFileURL[DTMF_0] = [NSURL fileURLWithPath:audioFilePath[DTMF_0]];
    audioFileURL[DTMF_1] = [NSURL fileURLWithPath:audioFilePath[DTMF_1]];
    audioFileURL[DTMF_2] = [NSURL fileURLWithPath:audioFilePath[DTMF_2]];
    audioFileURL[DTMF_3] = [NSURL fileURLWithPath:audioFilePath[DTMF_3]];
    audioFileURL[DTMF_4] = [NSURL fileURLWithPath:audioFilePath[DTMF_4]];
    audioFileURL[DTMF_5] = [NSURL fileURLWithPath:audioFilePath[DTMF_5]];
    audioFileURL[DTMF_6] = [NSURL fileURLWithPath:audioFilePath[DTMF_6]];
    audioFileURL[DTMF_7] = [NSURL fileURLWithPath:audioFilePath[DTMF_7]];
    audioFileURL[DTMF_8] = [NSURL fileURLWithPath:audioFilePath[DTMF_8]];
    audioFileURL[DTMF_9] = [NSURL fileURLWithPath:audioFilePath[DTMF_9]];
    audioFileURL[DTMF_STAR] = [NSURL fileURLWithPath:audioFilePath[DTMF_STAR]];
    audioFileURL[DTMF_HASH] = [NSURL fileURLWithPath:audioFilePath[DTMF_HASH]];
    /*
    testAudioPlayer[DTMF_0] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_0] error:nil];
    //testAudioPlayer[DTMF_0].delegate = self;
    [testAudioPlayer[DTMF_0] prepareToPlay];
    testAudioPlayer[DTMF_1] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_1] error:nil];
    testAudioPlayer[DTMF_1].delegate = self;
    [testAudioPlayer[DTMF_1] prepareToPlay];
    testAudioPlayer[DTMF_2] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_2] error:nil];
    //testAudioPlayer[DTMF_2].delegate = self;
    [testAudioPlayer[DTMF_2] prepareToPlay];
    testAudioPlayer[DTMF_3] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_3] error:nil];
    //testAudioPlayer[DTMF_3].delegate = self;
    [testAudioPlayer[DTMF_3] prepareToPlay];
    testAudioPlayer[DTMF_4] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_4] error:nil];
    //testAudioPlayer[DTMF_4].delegate = self;
    [testAudioPlayer[DTMF_4] prepareToPlay];
    testAudioPlayer[DTMF_5] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_5] error:nil];
    //testAudioPlayer[DTMF_5].delegate = self;
    [testAudioPlayer[DTMF_5] prepareToPlay];
    testAudioPlayer[DTMF_6] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_6] error:nil];
    //testAudioPlayer[DTMF_6].delegate = self;
    [testAudioPlayer[DTMF_6] prepareToPlay];
    testAudioPlayer[DTMF_7] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_7] error:nil];
    //testAudioPlayer[DTMF_7].delegate = self;
    [testAudioPlayer[DTMF_7] prepareToPlay];
    testAudioPlayer[DTMF_8] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_8] error:nil];
    //testAudioPlayer[DTMF_8].delegate = self;
    [testAudioPlayer[DTMF_8] prepareToPlay];
    testAudioPlayer[DTMF_9] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_9] error:nil];
    //testAudioPlayer[DTMF_9].delegate = self;
    [testAudioPlayer[DTMF_9] prepareToPlay];
    testAudioPlayer[DTMF_STAR] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_STAR] error:nil];
    //testAudioPlayer[DTMF_STAR].delegate = self;
    [testAudioPlayer[DTMF_STAR] prepareToPlay];
    testAudioPlayer[DTMF_HASH] = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_HASH] error:nil];
    //testAudioPlayer[DTMF_HASH].delegate = self;
    [testAudioPlayer[DTMF_HASH] prepareToPlay];
    */
    
    currentLeftSpeed=0;
    currentRightSpeed=0;
    desiredLeftSpeed=0;
    desiredRightSpeed=0;
    
    played=false;
    
    /*
    NSString *soundPath = [[NSBundle mainBundle] pathForResource:@"dtmf1" ofType:@"wav"];
	NSURL *soundURL = [NSURL fileURLWithPath:soundPath];
	int err = AudioServicesCreateSystemSoundID((__bridge CFURLRef)soundURL, &_fwSound);
    printf("err=%d\n", err);
    AudioServicesPlaySystemSound(_fwSound);
    while(1);
     */
    
    [NSThread detachNewThreadSelector:@selector(handleSpeedTask) toTarget:self withObject:nil];
    
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)fwTapped:(id)sender {
    
    if(recorder->isFollowingEnabled()) {
        recorder->setFollowingEnabled(false);
        [btnFollowing setTitle:@"Start line following" forState:UIControlStateNormal];
    } else {
        recorder->setFollowingEnabled(true);
        [btnFollowing setTitle:@"Stop line following" forState:UIControlStateNormal];
    }
    
}
- (IBAction)cliffDetectionPressed:(UIButton *)sender {
    if(recorder->isCliffDetectionEnabled()) {
        recorder->setCliffDetectionEnabled(false);
        [btnCliffDetection setTitle:@"Cliff detection ON" forState:UIControlStateNormal];
    } else {
        recorder->setCliffDetectionEnabled(true);
        [btnCliffDetection setTitle:@"Cliff detection OFF" forState:UIControlStateNormal];
    }
}

-(BOOL) textFieldShouldReturn: (UITextField *) textField
{
    //printf("textFieldShouldReturn\n");
    
    [textField resignFirstResponder];
    
    // You can access textField.text and do what you need to do with the text here
    
    return YES; // We'll let the textField handle the rest!
}
- (IBAction)textFieldDidBeginEditing:(id)sender {
    UITextField *textField = (UITextField*)sender;

[self animateTextField: textField up: YES];
}

- (IBAction)textFieldDidEndEditing:(id)sender {
    UITextField *textField = (UITextField*)sender;
    
    //printf("textFieldDidEndEditing\n");
        
        switch (textField.tag) {
            case 0:
                if ([textField.text isEqualToString:@""]) {
                    [textField setText:[NSString stringWithFormat:@"%d",180]];
                    recorder->setLineFollowThr(180);
                    return;
                }
                recorder->setLineFollowThr([textField.text intValue]);
                break;
                
            case 1:
                if ([textField.text isEqualToString:@""]) {
                    [textField setText:[NSString stringWithFormat:@"%d",15]];
                    recorder->setLineLostThr(15);
                    return;
                }
                recorder->setLineLostThr([textField.text intValue]);
                break;
            case 2:
                if ([textField.text isEqualToString:@""]) {
                    [textField setText:[NSString stringWithFormat:@"%d",10]];
                    recorder->setLineSpeed(10);
                    return;
                }
                recorder->setLineSpeed([textField.text intValue]);
                break;
                                
        }

     [self animateTextField: textField up: NO];
    
}

- (void) animateTextField: (UITextField*) textField up: (BOOL) up
{
    const int movementDistance = 100; // tweak as needed
    const float movementDuration = 0.3f; // tweak as needed
    
    int movement = (up ? -movementDistance : movementDistance);
    
    [UIView beginAnimations: @"anim" context: nil];
    [UIView setAnimationBeginsFromCurrentState: YES];
    [UIView setAnimationDuration: movementDuration];
    self.view.frame = CGRectOffset(self.view.frame, 0, movement);
    [UIView commitAnimations];
}

 -(void)updateSensorsNotification: (NSNotification*)notification {
     
     //NSDate *start = [NSDate date];
     
     NSDictionary *userInfo = notification.userInfo;
     
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"packetReceived"]) {
         
         prox0.progress = (float)recorder->getProx(0)/255.0;
         [prox0txt setText:[NSString stringWithFormat:@"%d", recorder->getProx(0)]];
         prox1.progress = (float)recorder->getProx(1)/255.0;
         [prox1txt setText:[NSString stringWithFormat:@"%d", recorder->getProx(1)]];
         prox2.progress = (float)recorder->getProx(2)/255.0;
         [prox2txt setText:[NSString stringWithFormat:@"%d", recorder->getProx(2)]];
         prox3.progress = (float)recorder->getProx(3)/255.0;
         [prox3txt setText:[NSString stringWithFormat:@"%d", recorder->getProx(3)]];
         
         ground0.progress = (float)recorder->getGround(0)/255.0;
         [ground0txt setText:[NSString stringWithFormat:@"%d", recorder->getGround(0)]];
         ground1.progress = (float)recorder->getGround(1)/255.0;
         [ground1txt setText:[NSString stringWithFormat:@"%d", recorder->getGround(1)]];
         ground2.progress = (float)recorder->getGround(2)/255.0;
         [ground2txt setText:[NSString stringWithFormat:@"%d", recorder->getGround(2)]];
         ground3.progress = (float)recorder->getGround(3)/255.0;
         [ground3txt setText:[NSString stringWithFormat:@"%d", recorder->getGround(3)]];
         
         proxAmb0.progress = (float)recorder->getProxAmb(0)/255.0;
         [prox0AmbTxt setText:[NSString stringWithFormat:@"%d", recorder->getProxAmb(0)]];
         //robotL = recorder->getProxAmb(0);
         proxAmb1.progress = (float)recorder->getProxAmb(1)/255.0;
         [prox1AmbTxt setText:[NSString stringWithFormat:@"%d", recorder->getProxAmb(1)]];
         //robotR = recorder->getProxAmb(1);
         proxAmb2.progress = (float)recorder->getProxAmb(2)/255.0;
         [prox2AmbTxt setText:[NSString stringWithFormat:@"%d", recorder->getProxAmb(2)]];
         proxAmb3.progress = (float)recorder->getProxAmb(3)/255.0;
         [prox3AmbTxt setText:[NSString stringWithFormat:@"%d", recorder->getProxAmb(3)]];
         
         [batteryValueTxt setText:[NSString stringWithFormat:@"%d", recorder->getBattery()]];
         
         [leftSpeed setText:[NSString stringWithFormat:@"%d", recorder->getMeasLeftSpeed()]];
         [rightSpeed setText:[NSString stringWithFormat:@"%d", recorder->getMeasRightSpeed()]];
         
         unsigned int temp = recorder->getFlagsRobotToPhone();
         if((temp&0x20)==0x20) {
             if((temp&0x40)==0x40) {
                 [batteryStatusTxt setText:@"CHARGED"];
             } else {
                 [batteryStatusTxt setText:@"CHARGING"];
             }
         } else {
             // not charging
             [batteryStatusTxt setText:@""];
         }
         
         desiredLeftSpeed = recorder->getLeftSpeed();
         desiredRightSpeed = recorder->getRightSpeed();
         temp = recorder->getBehaviorStatus();
         if(temp==0) {
             [behaviorStat setText:[NSString stringWithFormat:@"LINE SEARCH (%d,%d) (%d,%d)", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed]];
         } else if(temp==1) {
             [behaviorStat setText:[NSString stringWithFormat:@"LINE FOLLOW (%d,%d) (%d,%d)", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed]];
         }
         
         
     }
     
     /*
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox0"]) {
         prox0.progress = [[userInfo valueForKey:@"prox0"]floatValue]/255.0;
         [prox0txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox0"]intValue]]];
         if(DEBUG_PRINT) {
             printf("prox0!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox0"]intValue], [[userInfo valueForKey:@"prox0"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"prox0"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox1"]) {
         prox1.progress = [[userInfo valueForKey:@"prox1"]floatValue]/255.0;
         [prox1txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox1"]intValue]]];
         if(DEBUG_PRINT) {
             printf("prox1!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox1"]intValue], [[userInfo valueForKey:@"prox1"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"prox1"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox2"]) {
         prox2.progress = [[userInfo valueForKey:@"prox2"]floatValue]/255.0;
         [prox2txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox2"]intValue]]];
         if(DEBUG_PRINT) {
             printf("prox2!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox2"]intValue], [[userInfo valueForKey:@"prox2"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"prox2"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox3"]) {
         prox3.progress = [[userInfo valueForKey:@"prox3"]floatValue]/255.0;
         [prox3txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox3"]intValue]]];
         if(DEBUG_PRINT) {
             printf("prox3!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox3"]intValue], [[userInfo valueForKey:@"prox3"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"prox3"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground0"]) {
         ground0.progress = [[userInfo valueForKey:@"ground0"]floatValue]/255.0;
         [ground0txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground0"]intValue]]];
         if(DEBUG_PRINT) {
             printf("ground0!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground0"]intValue], [[userInfo valueForKey:@"ground0"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"ground0"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground1"]) {
         ground1.progress = [[userInfo valueForKey:@"ground1"]floatValue]/255.0;
         [ground1txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground1"]intValue]]];
         if(DEBUG_PRINT) {
             printf("ground1!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground1"]intValue], [[userInfo valueForKey:@"ground1"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"ground1"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground2"]) {
         ground2.progress = [[userInfo valueForKey:@"ground2"]floatValue]/255.0;
         [ground2txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground2"]intValue]]];
         if(DEBUG_PRINT) {
             printf("ground2!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground2"]intValue], [[userInfo valueForKey:@"ground2"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"ground2"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground3"]) {
         ground3.progress = [[userInfo valueForKey:@"ground3"]floatValue]/255.0;
         [ground3txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground3"]intValue]]];
         if(DEBUG_PRINT) {
             printf("ground3!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground3"]intValue], [[userInfo valueForKey:@"ground3"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"ground3"]floatValue]/255.0);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"battery"]) {
         [batteryValueTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"battery"]intValue]]];
         if(DEBUG_PRINT) {
             printf("battery!!\n");
             printf("notification = %d, %f\n", [[userInfo valueForKey:@"battery"]intValue], [[userInfo valueForKey:@"battery"]floatValue]);
             printf("progress = %f\n", [[userInfo valueForKey:@"battery"]floatValue]);
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb0"]) {
         proxAmb0.progress = [[userInfo valueForKey:@"proxAmb0"]floatValue]/255.0;
         [prox0AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb0"]intValue]]];
         robotL = [[userInfo valueForKey:@"proxAmb0"]intValue];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb1"]) {
         proxAmb1.progress = [[userInfo valueForKey:@"proxAmb1"]floatValue]/255.0;
         [prox1AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb1"]intValue]]];
         robotR = [[userInfo valueForKey:@"proxAmb1"]intValue];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb2"]) {
         proxAmb2.progress = [[userInfo valueForKey:@"proxAmb2"]floatValue]/255.0;
         [prox2AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb2"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb3"]) {
         proxAmb3.progress = [[userInfo valueForKey:@"proxAmb3"]floatValue]/255.0;
         [prox3AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb3"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"leftSpeed"]) {
         [leftSpeed setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"leftSpeed"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"rightSpeed"]) {
         [rightSpeed setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"rightSpeed"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"flagsRobotToPhone"]) {
         unsigned int temp = [[userInfo valueForKey:@"flagsRobotToPhone"]intValue];
         if((temp&0x20)==0x20) {
             if((temp&0x40)==0x40) {
                 [batteryStatusTxt setText:@"CHARGED"];
             } else {
                 [batteryStatusTxt setText:@"CHARGING"];
             }
         } else {
             // not charging
             [batteryStatusTxt setText:@""];
         }
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"behaviorStatus"]) {
         
         desiredLeftSpeed = recorder->getLeftSpeed();
         desiredRightSpeed = recorder->getRightSpeed();
         
         //printf("des(%d,%d), curr(%d,%d), robot(%d,%d)\n", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed, robotL, robotR);
         printf("algo update!\n");
         
         unsigned int temp = [[userInfo valueForKey:@"behaviorStatus"]intValue];
         if(temp==0) {
             [behaviorStat setText:[NSString stringWithFormat:@"LINE SEARCH (%d,%d) (%d,%d)", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed]];
         } else if(temp==1) {
             [behaviorStat setText:[NSString stringWithFormat:@"LINE FOLLOW (%d,%d) (%d,%d)", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed]];
         }
         
     }
     */
     
     //NSDate *stop = [NSDate date];
     //NSTimeInterval executionTime = [stop timeIntervalSinceDate:start];
     //printf("execution time update = %f\n", executionTime);
     
 }

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    played=true;
    printf("played\n");
}

- (void)handleSpeedTask {
    
    counter=0;
    tempSpeed=0;
    BOOL sleepDone=false;
    double pause=0.045; // wait 45 ms to let the command be interpreted
    
    //NSDate *start = [NSDate date];
    //NSDate *stop = [NSDate date];
    //NSTimeInterval executionTime = [stop timeIntervalSinceDate:start];
    
    //NSDate *start1 = [NSDate date];
    //NSDate *stop1 = [NSDate date];
    //NSTimeInterval executionTime1 = [stop timeIntervalSinceDate:start];
    
    while(1) {
        
        //start1 = [NSDate date];
        
        sleepDone = false;
        
        /*
        if(recorder->isFollowingEnabled()) {
            //counter++;
            //if(counter>=1) {
            //    counter=0;
            if(tempSpeed <= (125-125)) {
                tempSpeed+=125;
            } else {
                tempSpeed = 125;
            }
            //}
            desiredLeftSpeed = tempSpeed;
            desiredRightSpeed = 0;
        }
        */
        
        //if(recorder->isFollowingEnabled()) {
            
            if(desiredLeftSpeed==0 && desiredRightSpeed==0) {
                currentLeftSpeed=0;
                currentRightSpeed=0;
                
                //played=false;
                //[testAudioPlayer[DTMF_5] prepareToPlay];
                //[testAudioPlayer[DTMF_5] play];
                //while(!played);
                
                //[testAudioPlayer[DTMF_5] prepareToPlay];
                //[testAudioPlayer[DTMF_5] play];
                
                testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_5] error:nil];
                [testAudioPlayer prepareToPlay];
                [testAudioPlayer play];
                
                //printf("5\n");
                [NSThread sleepForTimeInterval:pause];
                
                sleepDone = true;
            } else if((desiredLeftSpeed*currentLeftSpeed)<0 && (desiredRightSpeed*currentRightSpeed)<0) {   // inverted direction for both motors
                currentLeftSpeed=0;
                currentRightSpeed=0;
                
                //played=false;
                //[testAudioPlayer[DTMF_5] prepareToPlay];
                //[testAudioPlayer[DTMF_5] play];
                //while(!played);
                
                //[testAudioPlayer[DTMF_5] prepareToPlay];
                //[testAudioPlayer[DTMF_5] play];
                
                testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_5] error:nil];
                [testAudioPlayer prepareToPlay];
                [testAudioPlayer play];
                
                //printf("5\n");
                [NSThread sleepForTimeInterval:pause];
                
                sleepDone = true;
            } else {
                int diffLeft = desiredLeftSpeed-currentLeftSpeed;
                int diffRight = desiredRightSpeed-currentRightSpeed;
                
                if(diffLeft>=DTMF_SPEED_STEP && diffRight>=DTMF_SPEED_STEP) {   // current speed is lower than desired for both motors
                    currentLeftSpeed+=DTMF_SPEED_STEP;
                    currentRightSpeed+=DTMF_SPEED_STEP;
                    
                    //played=false;
                    //[testAudioPlayer[DTMF_2] prepareToPlay];
                    //[testAudioPlayer[DTMF_2] play];
                    //while(!played);
                    
                    //[testAudioPlayer[DTMF_2] prepareToPlay];
                    //[testAudioPlayer[DTMF_2] play];
                    
                    testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_2] error:nil];
                    [testAudioPlayer prepareToPlay];
                    [testAudioPlayer play];
                    
                    //printf("2 (diffL=%d, diffR=%d)\n", diffLeft, diffRight);
                    [NSThread sleepForTimeInterval:pause];
                    
                    sleepDone = true;
                } else if(diffLeft<=-DTMF_SPEED_STEP && diffRight<=-DTMF_SPEED_STEP) {    // current speed is higher than desired for both motors
                    currentLeftSpeed-=DTMF_SPEED_STEP;
                    currentRightSpeed-=DTMF_SPEED_STEP;
                    
                    //played=false;
                    //[testAudioPlayer[DTMF_8] prepareToPlay];
                    //[testAudioPlayer[DTMF_8] play];
                    //while(!played);
                    
                    //[testAudioPlayer[DTMF_8] prepareToPlay];
                    //[testAudioPlayer[DTMF_8] play];
                    
                    testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_8] error:nil];
                    [testAudioPlayer prepareToPlay];
                    [testAudioPlayer play];
                    
                    //printf("8 (diffL=%d, diffR=%d)\n", diffLeft, diffRight);
                    [NSThread sleepForTimeInterval:pause];
                    
                    sleepDone = true;
                } else {
                    
                    if(diffLeft>=DTMF_SPEED_STEP) { // current left speed is lower than desired
                        currentLeftSpeed+=DTMF_SPEED_STEP;
                        
                        //start = [NSDate date];
                        
                        //AudioServicesPlaySystemSound(_fwSound);
                        
                        //played=false;
                        //[testAudioPlayer[DTMF_1] prepareToPlay];
                        //[testAudioPlayer[DTMF_1] play];
                        //while(!played);
                        
                        //[testAudioPlayer[DTMF_1] prepareToPlay];
                        //[testAudioPlayer[DTMF_1] play];
                        
                        testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_1] error:nil];
                        [testAudioPlayer prepareToPlay];
                        [testAudioPlayer play];
                        
                        //stop = [NSDate date];
                        //executionTime = [stop timeIntervalSinceDate:start];
                        //printf("execution time 1 = %f\n", executionTime);
                        
                        //printf("1 (diff=%d)\n", diffLeft);
                        
                        //start = [NSDate date];
                        [NSThread sleepForTimeInterval:pause];
                        //[testAudioPlayer[DTMF_1] prepareToPlay];
                        //stop = [NSDate date];
                        //executionTime = [stop timeIntervalSinceDate:start];
                        //printf("execution time 2 = %f\n", executionTime);
                        
                        sleepDone = true;
                    } else if(diffLeft<=-DTMF_SPEED_STEP) {  // current left speed is higher than desired
                        currentLeftSpeed-=DTMF_SPEED_STEP;
                        
                        //played=false;
                        //[testAudioPlayer[DTMF_7] prepareToPlay];
                        //[testAudioPlayer[DTMF_7] play];
                        //while(!played);
                        
                        //[testAudioPlayer[DTMF_7] prepareToPlay];
                        //[testAudioPlayer[DTMF_7] play];
                        
                        testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_7] error:nil];
                        [testAudioPlayer prepareToPlay];
                        [testAudioPlayer play];
                        
                        //printf("7 (diff=%d)\n", diffLeft);
                        [NSThread sleepForTimeInterval:pause];
                        
                        sleepDone = true;
                    }
                    
                    if(diffRight>=DTMF_SPEED_STEP) { // current right speed is lower than desired
                        currentRightSpeed+=DTMF_SPEED_STEP;
                        
                        //played=false;
                        //[testAudioPlayer[DTMF_3] prepareToPlay];
                        //[testAudioPlayer[DTMF_3] play];
                        //while(!played);
                        
                        //[testAudioPlayer[DTMF_3] prepareToPlay];
                        //[testAudioPlayer[DTMF_3] play];
                        
                        testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_3] error:nil];
                        [testAudioPlayer prepareToPlay];
                        [testAudioPlayer play];
                        
                        //printf("3 (diff=%d)\n", diffRight);
                        [NSThread sleepForTimeInterval:pause];
                        
                        sleepDone = true;
                    } else if(diffRight<=-DTMF_SPEED_STEP) {
                        currentRightSpeed-=DTMF_SPEED_STEP;
                        
                        //played=false;
                        //[testAudioPlayer[DTMF_9] prepareToPlay];
                        //[testAudioPlayer[DTMF_9] play];
                        //while(!played);
                        
                        //[testAudioPlayer[DTMF_9] prepareToPlay];
                        //[testAudioPlayer[DTMF_9] play];
                        
                        testAudioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL[DTMF_9] error:nil];
                        [testAudioPlayer prepareToPlay];
                        [testAudioPlayer play];
                        
                        //printf("9 (diff=%d)\n", diffRight);
                        [NSThread sleepForTimeInterval:pause];
                        
                        sleepDone = true;
                    }
                    
                }
            }
            
            //printf("des(%d,%d), curr(%d,%d), robot(%d,%d)\n", desiredLeftSpeed, desiredRightSpeed, currentLeftSpeed, currentRightSpeed, robotL, robotR);
            
        //}
        
        if(!sleepDone) {
            [NSThread sleepForTimeInterval:0.015];   // wait 50 ms to avoid running continuously
        }
        
        //stop1 = [NSDate date];
        //executionTime1 = [stop1 timeIntervalSinceDate:start1];
        //printf("total time = %f\n", executionTime1);
        
    }
    //[self performSelectorOnMainThread:@selector(makeMyProgressBarMoving) withObject:nil waitUntilDone:NO];
    
}

#pragma mark AudioSession listeners
void interruptionListener(	void *	inClientData,
                          UInt32	inInterruptionState)
{
	FirstViewController *THIS = (__bridge FirstViewController*)inClientData;
	if (inInterruptionState == kAudioSessionBeginInterruption)
	{
		if (THIS->recorder->IsRunning()) {
			[THIS stopRecord];
		}
	}
    
}

void propListener(	void *                  inClientData,
                  AudioSessionPropertyID	inID,
                  UInt32                  inDataSize,
                  const void *            inData)
{
	FirstViewController *THIS = (__bridge FirstViewController*)inClientData;
	if (inID == kAudioSessionProperty_AudioRouteChange)
	{
		CFDictionaryRef routeDictionary = (CFDictionaryRef)inData;
		//CFShow(routeDictionary);
		CFNumberRef reason = (CFNumberRef)CFDictionaryGetValue(routeDictionary, CFSTR(kAudioSession_AudioRouteChangeKey_Reason));
		SInt32 reasonVal;
		CFNumberGetValue(reason, kCFNumberSInt32Type, &reasonVal);
		if (reasonVal != kAudioSessionRouteChangeReason_CategoryChange)
		{
			/*CFStringRef oldRoute = (CFStringRef)CFDictionaryGetValue(routeDictionary, CFSTR(kAudioSession_AudioRouteChangeKey_OldRoute));
             if (oldRoute)
             {
             printf("old route:\n");
             CFShow(oldRoute);
             }
             else
             printf("ERROR GETTING OLD AUDIO ROUTE!\n");
             
             CFStringRef newRoute;
             UInt32 size; size = sizeof(CFStringRef);
             OSStatus error = AudioSessionGetProperty(kAudioSessionProperty_AudioRoute, &size, &newRoute);
             if (error) printf("ERROR GETTING NEW AUDIO ROUTE! %d\n", error);
             else
             {
             printf("new route:\n");
             CFShow(newRoute);
             }*/
            
			if (reasonVal == kAudioSessionRouteChangeReason_OldDeviceUnavailable)
			{
                //				if (THIS->player->IsRunning()) {
                //					[THIS pausePlayQueue];
                //					[[NSNotificationCenter defaultCenter] postNotificationName:@"playbackQueueStopped" object:THIS];
                //				}
			}
            
			// stop the queue if we had a non-policy route change
			if (THIS->recorder->IsRunning()) {
				[THIS stopRecord];
			}
		}
	}
	else if (inID == kAudioSessionProperty_AudioInputAvailable)
	{
		if (inDataSize == sizeof(UInt32)) {
			UInt32 isAvailable = *(UInt32*)inData;
			// disable recording if input is not available
		}
	}
}

#pragma mark Initialization routines
- (void)awakeFromNib
{
    
    NSLog(@"awakeFromNib called!");
    
	// Allocate our singleton instance for the recorder object
	recorder = new AQRecorder();
    
	OSStatus error = AudioSessionInitialize(NULL, NULL, interruptionListener, (__bridge void *)self);
	if (error) printf("ERROR INITIALIZING AUDIO SESSION! %d\n", (int)error);
	else
	{
		UInt32 category = kAudioSessionCategory_PlayAndRecord;
        //UInt32 category = kAudioSessionCategory_RecordAudio;
		error = AudioSessionSetProperty(kAudioSessionProperty_AudioCategory, sizeof(category), &category);
		if (error) printf("couldn't set audio category!");
        
        //UInt32 allowMixing = true;
        //AudioSessionSetProperty(kAudioSessionProperty_OverrideCategoryMixWithOthers, sizeof(allowMixing), &allowMixing);
        //AudioSessionSetProperty(kAudioSessionProperty_OtherMixableAudioShouldDuck, sizeof(allowMixing), &allowMixing);
        
		error = AudioSessionAddPropertyListener(kAudioSessionProperty_AudioRouteChange, propListener, (__bridge void *)self);
		if (error) printf("ERROR ADDING AUDIO SESSION PROP LISTENER! %d\n", (int)error);
		UInt32 inputAvailable = 0;
		UInt32 size = sizeof(inputAvailable);
		
		// we do not want to allow recording if input is not available
		error = AudioSessionGetProperty(kAudioSessionProperty_AudioInputAvailable, &size, &inputAvailable);
		if (error) printf("ERROR GETTING INPUT AVAILABILITY! %d\n", (int)error);
		
		// we also need to listen to see if input availability changes
		error = AudioSessionAddPropertyListener(kAudioSessionProperty_AudioInputAvailable, propListener, (__bridge void *)self);
		if (error) printf("ERROR ADDING AUDIO SESSION PROP LISTENER! %d\n", (int)error);
        
		error = AudioSessionSetActive(true);
		if (error) printf("AudioSessionSetActive (true) failed");
	}
    
    // Start the recorder
    recorder->StartRecord();
    
    /*
     NSString *audioFilePath = [[NSBundle mainBundle] pathForResource:@"dtmf2" ofType:@"wav"];
     NSURL *audioFileURL = [NSURL fileURLWithPath:audioFilePath];
     
     AVAudioPlayer *audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioFileURL error:nil];
     audioPlayer.volume = 1;
     [audioPlayer prepareToPlay];
     [audioPlayer play];
     */
    
    [self registerForBackgroundNotifications];
    
}

#pragma mark background notifications
- (void)registerForBackgroundNotifications
{
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(resignActive)
												 name:UIApplicationWillResignActiveNotification
											   object:nil];
	
	[[NSNotificationCenter defaultCenter] addObserver:self
											 selector:@selector(enterForeground)
												 name:UIApplicationWillEnterForegroundNotification
											   object:nil];
}

- (void)resignActive
{
    if (recorder->IsRunning()) [self stopRecord];
}

- (void)enterForeground
{
    OSStatus error = AudioSessionSetActive(true);
    if (error) printf("AudioSessionSetActive (true) failed");
}

#pragma mark Cleanup
- (void)dealloc
{
	delete recorder;
    
}

@end
