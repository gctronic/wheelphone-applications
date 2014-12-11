/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "FrameMarkersEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>

@interface FrameMarkersViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    FrameMarkersEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    
    id backgroundObserver;
    id activeObserver;
}

@end
