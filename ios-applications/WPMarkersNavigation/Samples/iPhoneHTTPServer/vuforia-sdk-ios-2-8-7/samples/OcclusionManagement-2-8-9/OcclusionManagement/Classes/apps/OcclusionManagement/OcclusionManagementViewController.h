/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "OcclusionManagementEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>

@interface OcclusionManagementViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    CGRect arViewRect; // the size of the AR view

    OcclusionManagementEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    QCAR::DataSet*  dataSet;

    id backgroundObserver;
    id activeObserver;
}

@end
