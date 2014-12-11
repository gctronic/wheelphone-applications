/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "BooksControllerDelegateProtocol.h"

@class TargetOverlayView;

// OverlayViewController class overrides one UIViewController method
@interface BooksOverlayViewController : UIViewController
{
    UILabel *statusLabel;
    UIButton *closeButton;
    NSTimer *statusTimer;
    
    UIView *optionsOverlayView; // the view for the options pop-up
    UIView *loadingView;
    
    id <BooksControllerDelegateProtocol> booksDelegate;
}

- (id)initWithDelegate:(id<BooksControllerDelegateProtocol>) delegate;

- (void) handleViewRotation:(UIInterfaceOrientation)interfaceOrientation;

@property (nonatomic, retain) IBOutlet TargetOverlayView *targetOverlayView;

@end
