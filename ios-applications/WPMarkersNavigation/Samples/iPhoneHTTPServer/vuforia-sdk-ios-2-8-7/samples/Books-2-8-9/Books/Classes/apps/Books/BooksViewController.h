/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "SampleAppMenu.h"
#import "BooksEAGLView.h"
#import "SampleApplicationSession.h"
#import "BooksControllerDelegateProtocol.h"
#import "BooksOverlayViewController.h"
#import "Book.h"


@interface BooksViewController : UIViewController <SampleApplicationControl, UIAlertViewDelegate, BooksControllerDelegateProtocol,SampleAppMenuCommandProtocol, UIGestureRecognizerDelegate>{
    CGRect viewFrame;
    BooksEAGLView* eaglView;
    CGRect arViewRect; // the size of the AR view

    UITapGestureRecognizer * tapGestureRecognizer;
    SampleApplicationSession * vapp;
    BooksOverlayViewController * bookOverlayController;
    
    BOOL scanningMode;
    BOOL isVisualSearchOn;
    
    NSString * lastTargetIDScanned;
    Book *lastScannedBook;
    BOOL isShowingAnAlertView;
    
    id backgroundObserver;
    id activeObserver;
    int lastErrorCode;
}

@end
