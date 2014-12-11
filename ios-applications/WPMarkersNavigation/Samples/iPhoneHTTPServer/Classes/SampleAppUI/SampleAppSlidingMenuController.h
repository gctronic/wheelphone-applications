/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

@class SampleAppLeftMenuViewController;

@interface SampleAppSlidingMenuController : UIViewController <UIGestureRecognizerDelegate>{
    
    // we keep track of the gesture recognizers in order to be able to enable/disable them
    UITapGestureRecognizer * tapGestureRecognizer;
    
    CGFloat kSlidingMenuWidth;
    BOOL ignoreDoubleTap;
    
    // true when the left menu is displayed
    BOOL showingLeftMenu;
}

- (id)initWithRootViewController:(UIViewController*)controller;

- (void) shouldIgnoreDoubleTap;
- (void) showRootController:(BOOL)animated;
- (void) showLeftMenu:(BOOL)animated;

- (void) dismiss;

@end

