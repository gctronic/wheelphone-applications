/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>
#import "CustomToolbarDelegateProtocol.h"
#import "CustomButton.h"

//  CustomToolbar is used to mimic the iOS camera screen.
//  It contains a cancelButton that can be hidden and an actionImage that rotates according to
//  the device orientation. To get feedback from the buttons tapped you have to set a delegate that
//  implements CustomToolbarDelegateProtocol

@interface CustomToolbar : UIView
{
    UIButton *cancelButton;
    CustomButton *actionButton;
    UIImage *actionImage;
    UIImageView *backgroundImageView;
    
    id <CustomToolbarDelegateProtocol> delegate;
    
    BOOL shouldRotateActionButton;
}

@property (retain) UIImage *actionImage;
@property (assign) id <CustomToolbarDelegateProtocol> delegate;
@property (assign) BOOL isCancelButtonHidden;
@property (assign) BOOL shouldRotateActionButton;

@end
