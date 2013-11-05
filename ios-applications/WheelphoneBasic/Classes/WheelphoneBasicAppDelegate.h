//
//  WheelphoneBasicAppDelegate.h
//  WheelphoneBasic
//
//  Created by Stefano Morgani on 3/13/13.
//  Copyright Stefano Morgani 2013. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>

@class WheelphoneBasicViewController;

@interface WheelphoneBasicAppDelegate : NSObject <UIApplicationDelegate> {
    UIWindow *window;
    WheelphoneBasicViewController *viewController;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet WheelphoneBasicViewController *viewController;

@end

