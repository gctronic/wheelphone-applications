/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>
#import "TextRecoEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>
#import "SampleAppMenu.h"

@interface TextRecoViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    CGRect viewQCARFrame;
    TextRecoEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;

    id backgroundObserver;
    id activeObserver;
    
    int ROICenterX;
    int ROICenterY;
    int ROIWidth;
    int ROIHeight;
}

@end
