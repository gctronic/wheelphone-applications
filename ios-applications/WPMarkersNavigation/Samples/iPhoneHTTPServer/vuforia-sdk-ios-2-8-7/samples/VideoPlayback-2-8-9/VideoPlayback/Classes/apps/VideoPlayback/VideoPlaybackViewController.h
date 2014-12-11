/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "VideoPlaybackEAGLView.h"
#import "SampleApplicationSession.h"
#import <QCAR/DataSet.h>

@interface VideoPlaybackViewController : UIViewController <SampleApplicationControl, SampleAppMenuCommandProtocol>{
    CGRect viewFrame;
    VideoPlaybackEAGLView* eaglView;
    QCAR::DataSet*  dataSet;
    SampleApplicationSession * vapp;
    
    id backgroundObserver;
    id activeObserver;
}

- (void)rootViewControllerPresentViewController:(UIViewController*)viewController inContext:(BOOL)currentContext;
- (void)rootViewControllerDismissPresentedViewController;

@end
