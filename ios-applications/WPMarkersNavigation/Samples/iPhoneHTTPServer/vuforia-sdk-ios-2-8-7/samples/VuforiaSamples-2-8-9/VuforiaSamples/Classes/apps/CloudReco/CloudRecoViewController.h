/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>
#import "CloudRecoEAGLView.h"
#import "SampleApplicationSession.h"
#import "SampleAppMenu.h"

@interface CloudRecoViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    CloudRecoEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    
    BOOL scanningMode;
    BOOL isVisualSearchOn;
    BOOL offTargetTrackingEnabled;
    
    id backgroundObserver;
    id activeObserver;
    BOOL isShowingAnAlertView;
    int lastErrorCode;
}

@end
