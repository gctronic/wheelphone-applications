/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "MultiTargetsEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>

@interface MultiTargetsViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    MultiTargetsEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    QCAR::DataSet*  dataSet;
    
    id backgroundObserver;
    id activeObserver;
}

@end
