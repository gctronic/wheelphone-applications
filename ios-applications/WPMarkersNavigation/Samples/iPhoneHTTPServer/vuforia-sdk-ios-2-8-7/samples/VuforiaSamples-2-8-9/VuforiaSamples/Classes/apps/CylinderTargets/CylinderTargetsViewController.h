/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "CylinderTargetsEAGLView.h"
#import "SampleAppMenu.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>

@interface CylinderTargetsViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    CylinderTargetsEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    QCAR::DataSet*  dataSet;
    
    id backgroundObserver;
    id activeObserver;
}

@end
