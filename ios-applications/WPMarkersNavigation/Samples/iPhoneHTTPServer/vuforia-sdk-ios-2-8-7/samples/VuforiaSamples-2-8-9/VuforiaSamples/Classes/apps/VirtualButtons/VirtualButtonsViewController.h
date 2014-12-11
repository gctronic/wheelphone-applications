/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "VirtualButtonsEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>



@interface VirtualButtonsViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    VirtualButtonsEAGLView* eaglView;
    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    
    QCAR::DataSet*  dataSet;

    bool buttonStateChanged;
    bool buttonActivated[NB_BUTTONS];
    
    id backgroundObserver;
    id activeObserver;
}

@end
