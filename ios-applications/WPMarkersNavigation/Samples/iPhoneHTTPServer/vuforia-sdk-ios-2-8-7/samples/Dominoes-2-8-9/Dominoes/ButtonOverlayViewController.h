/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "EAGLView.h"


@interface ButtonOverlayViewController : UIViewController {
    UIButton* menuButton;
    UIButton* resetButton;
    UIButton* runButton;
    UIButton* clearButton;
    UIButton* deleteButton;
    UILabel* messageLabel;
    
    NSTimer* messageTimer;    
    id menuId;
    SEL menuSel;
}

@property (nonatomic, retain) UIButton* menuButton;
@property (nonatomic, retain) UIButton* resetButton;
@property (nonatomic, retain) UIButton* runButton;
@property (nonatomic, retain) UIButton* clearButton;
@property (nonatomic, retain) UIButton* deleteButton;
@property (nonatomic, retain) UILabel* messageLabel;

- (void) setMenuCallBack:(SEL)callback forTarget:(id)target;

- (void) pressMenuButton;
- (void) pressResetButton;
- (void) pressRunButton;
- (void) pressClearButton;
- (void) pressDeleteButton;

- (void) showDeleteButton;
- (void) hideDeleteButton;
- (void) showMessage:(NSString *)theMessage;
- (void) hideMessage;

@end
