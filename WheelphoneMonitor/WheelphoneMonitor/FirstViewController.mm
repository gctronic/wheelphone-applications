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

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
//    
//	// Allocate our singleton instance for the recorder
//	recorder = new AQRecorder();
//    
//	OSStatus error = AudioSessionInitialize(NULL, NULL, interruptionListener, self);
//	if (error) {
//        printf("ERROR INITIALIZING AUDIO SESSION! %d\n", (int)error);
//    } else {
//		UInt32 category = kAudioSessionCategory_PlayAndRecord;
//		error = AudioSessionSetProperty(kAudioSessionProperty_AudioCategory, sizeof(category), &category);
//		if (error) printf("couldn't set audio category!");
//        
//		error = AudioSessionAddPropertyListener(kAudioSessionProperty_AudioRouteChange, propListener, self);
//		if (error) printf("ERROR ADDING AUDIO SESSION PROP LISTENER! %d\n", (int)error);
//		UInt32 inputAvailable = 0;
//		UInt32 size = sizeof(inputAvailable);
//		
//		// we do not want to allow recording if input is not available
//		error = AudioSessionGetProperty(kAudioSessionProperty_AudioInputAvailable, &size, &inputAvailable);
//		if (error) printf("ERROR GETTING INPUT AVAILABILITY! %d\n", (int)error);
//		//btn_record.enabled = (inputAvailable) ? YES : NO;
//		
//		// we also need to listen to see if input availability changes
//		error = AudioSessionAddPropertyListener(kAudioSessionProperty_AudioInputAvailable, propListener, self);
//		if (error) printf("ERROR ADDING AUDIO SESSION PROP LISTENER! %d\n", (int)error);
//        
//		error = AudioSessionSetActive(true);
//		if (error) printf("AudioSessionSetActive (true) failed");
//	}
//	
//	//[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playbackQueueStopped:) name:@"playbackQueueStopped" object:nil];
//	//[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playbackQueueResumed:) name:@"playbackQueueResumed" object:nil];
//    
//    [self registerForBackgroundNotifications];
    
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
    groundAmb0.transform = transform;
    groundAmb1.transform = transform;
    groundAmb2.transform = transform;
    groundAmb3.transform = transform;
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

//#pragma mark AudioSession listeners
//void interruptionListener(	void *	inClientData,
//                          UInt32	inInterruptionState)
//{
//	FirstViewController *THIS = (__bridge FirstViewController*)inClientData;
//	if (inInterruptionState == kAudioSessionBeginInterruption)
//	{
//		if (THIS->recorder->IsRunning()) {
//			[THIS stopRecord];
//		}
//		//else if (THIS->player->IsRunning()) {
//		//	//the queue will stop itself on an interruption, we just need to update the UI
//		//	[[NSNotificationCenter defaultCenter] postNotificationName:@"playbackQueueStopped" object:THIS];
//		//	THIS->playbackWasInterrupted = YES;
//		//}
//	}
//	//else if ((inInterruptionState == kAudioSessionEndInterruption) && THIS->playbackWasInterrupted)
//	//{
//	//	// we were playing back when we were interrupted, so reset and resume now
//	//	THIS->player->StartQueue(true);
//	//	[[NSNotificationCenter defaultCenter] postNotificationName:@"playbackQueueResumed" object:THIS];
//	//	THIS->playbackWasInterrupted = NO;
//	//}
//}
//
//void propListener(	void *                  inClientData,
//                  AudioSessionPropertyID	inID,
//                  UInt32                  inDataSize,
//                  const void *            inData)
//{
//	FirstViewController *THIS = (__bridge FirstViewController*)inClientData;
//	if (inID == kAudioSessionProperty_AudioRouteChange)
//	{
//		CFDictionaryRef routeDictionary = (CFDictionaryRef)inData;
//		//CFShow(routeDictionary);
//		CFNumberRef reason = (CFNumberRef)CFDictionaryGetValue(routeDictionary, CFSTR(kAudioSession_AudioRouteChangeKey_Reason));
//		SInt32 reasonVal;
//		CFNumberGetValue(reason, kCFNumberSInt32Type, &reasonVal);
//		if (reasonVal != kAudioSessionRouteChangeReason_CategoryChange)
//		{
//			/*CFStringRef oldRoute = (CFStringRef)CFDictionaryGetValue(routeDictionary, CFSTR(kAudioSession_AudioRouteChangeKey_OldRoute));
//             if (oldRoute)
//             {
//             printf("old route:\n");
//             CFShow(oldRoute);
//             }
//             else
//             printf("ERROR GETTING OLD AUDIO ROUTE!\n");
//             
//             CFStringRef newRoute;
//             UInt32 size; size = sizeof(CFStringRef);
//             OSStatus error = AudioSessionGetProperty(kAudioSessionProperty_AudioRoute, &size, &newRoute);
//             if (error) printf("ERROR GETTING NEW AUDIO ROUTE! %d\n", error);
//             else
//             {
//             printf("new route:\n");
//             CFShow(newRoute);
//             }*/
//            
//			if (reasonVal == kAudioSessionRouteChangeReason_OldDeviceUnavailable)
//			{
//				//if (THIS->player->IsRunning()) {
//				//	[THIS pausePlayQueue];
//				//	[[NSNotificationCenter defaultCenter] postNotificationName:@"playbackQueueStopped" object:THIS];
//				//}
//			}
//            
//			// stop the queue if we had a non-policy route change
//			if (THIS->recorder->IsRunning()) {
//				[THIS stopRecord];
//			}
//		}
//	}
//	else if (inID == kAudioSessionProperty_AudioInputAvailable)
//	{
//		if (inDataSize == sizeof(UInt32)) {
//			UInt32 isAvailable = *(UInt32*)inData;
//			// disable recording if input is not available
//			//THIS->btn_record.enabled = (isAvailable > 0) ? YES : NO;
//		}
//	}
//}
//
//
//- (void)stopRecord
//{
//	recorder->StopRecord();
//}


 -(void)updateSensorsNotification: (NSNotification*)notification {
     NSDictionary *userInfo = notification.userInfo;
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox0"]) {
         prox0.progress = [[userInfo valueForKey:@"prox0"]floatValue]/255.0;
         [prox0txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox0"]intValue]]];
         printf("prox0!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox0"]intValue], [[userInfo valueForKey:@"prox0"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"prox0"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox1"]) {
         prox1.progress = [[userInfo valueForKey:@"prox1"]floatValue]/255.0;
         [prox1txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox1"]intValue]]];
         printf("prox1!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox1"]intValue], [[userInfo valueForKey:@"prox1"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"prox1"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox2"]) {
         prox2.progress = [[userInfo valueForKey:@"prox2"]floatValue]/255.0;
         [prox2txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox2"]intValue]]];
         printf("prox2!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox2"]intValue], [[userInfo valueForKey:@"prox2"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"prox2"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"prox3"]) {
         prox3.progress = [[userInfo valueForKey:@"prox3"]floatValue]/255.0;
         [prox3txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"prox3"]intValue]]];
         printf("prox3!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"prox3"]intValue], [[userInfo valueForKey:@"prox3"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"prox3"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground0"]) {
         ground0.progress = [[userInfo valueForKey:@"ground0"]floatValue]/255.0;
         [ground0txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground0"]intValue]]];
         printf("ground0!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground0"]intValue], [[userInfo valueForKey:@"ground0"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"ground0"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground1"]) {
         ground1.progress = [[userInfo valueForKey:@"ground1"]floatValue]/255.0;
         [ground1txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground1"]intValue]]];
         printf("ground1!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground1"]intValue], [[userInfo valueForKey:@"ground1"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"ground1"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground2"]) {
         ground2.progress = [[userInfo valueForKey:@"ground2"]floatValue]/255.0;
         [ground2txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground2"]intValue]]];
         printf("ground2!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground2"]intValue], [[userInfo valueForKey:@"ground2"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"ground2"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"ground3"]) {
         ground3.progress = [[userInfo valueForKey:@"ground3"]floatValue]/255.0;
         [ground3txt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"ground3"]intValue]]];
         printf("ground3!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"ground3"]intValue], [[userInfo valueForKey:@"ground3"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"ground3"]floatValue]/255.0);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"battery"]) {
         [batteryValueTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"battery"]intValue]]];
         printf("battery!!\n");
         printf("notification = %d, %f\n", [[userInfo valueForKey:@"battery"]intValue], [[userInfo valueForKey:@"battery"]floatValue]);
         printf("progress = %f\n", [[userInfo valueForKey:@"battery"]floatValue]);
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb0"]) {
         proxAmb0.progress = [[userInfo valueForKey:@"proxAmb0"]floatValue]/255.0;
         [prox0AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb0"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb1"]) {
         proxAmb1.progress = [[userInfo valueForKey:@"proxAmb1"]floatValue]/255.0;
         [prox1AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb1"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb2"]) {
         proxAmb2.progress = [[userInfo valueForKey:@"proxAmb2"]floatValue]/255.0;
         [prox2AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb2"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"proxAmb3"]) {
         proxAmb3.progress = [[userInfo valueForKey:@"proxAmb3"]floatValue]/255.0;
         [prox3AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"proxAmb3"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"groundAmb0"]) {
         groundAmb0.progress = [[userInfo valueForKey:@"groundAmb0"]floatValue]/255.0;
         [ground0AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"groundAmb0"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"groundAmb1"]) {
         groundAmb1.progress = [[userInfo valueForKey:@"groundAmb1"]floatValue]/255.0;
         [ground1AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"groundAmb1"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"groundAmb2"]) {
         groundAmb2.progress = [[userInfo valueForKey:@"groundAmb2"]floatValue]/255.0;
         [ground2AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"groundAmb2"]intValue]]];
     }
     if ([[[userInfo allKeys] objectAtIndex:0] isEqualToString:@"groundAmb3"]) {
         groundAmb3.progress = [[userInfo valueForKey:@"groundAmb3"]floatValue]/255.0;
         [ground3AmbTxt setText:[NSString stringWithFormat:@"%d", [[userInfo valueForKey:@"groundAmb3"]intValue]]];
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
     
 }
           
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
@synthesize groundAmb0;
@synthesize groundAmb1;
@synthesize groundAmb2;
@synthesize groundAmb3;
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
@synthesize ground0AmbTxt;
@synthesize ground1AmbTxt;
@synthesize ground2AmbTxt;
@synthesize ground3AmbTxt;
@synthesize batteryValueTxt;
@synthesize batteryStatusTxt;
@end
