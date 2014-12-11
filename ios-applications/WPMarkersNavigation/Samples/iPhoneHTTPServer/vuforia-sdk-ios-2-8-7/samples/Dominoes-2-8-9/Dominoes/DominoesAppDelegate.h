/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>
#import "EAGLView.h"

@class ARParentViewController;
    
    
@interface DominoesAppDelegate : NSObject <UIApplicationDelegate> {
    UIWindow* window;
    ARParentViewController* arParentViewController;
    UIImageView *splashV;
}

@end
