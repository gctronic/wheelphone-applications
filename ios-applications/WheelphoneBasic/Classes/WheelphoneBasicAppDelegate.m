//
//  WheelphoneBasicAppDelegate.m
//  WheelphoneBasic
//
//  Created by Stefano Morgani on 3/13/13.
//  Copyright Stefano Morgani 2013. All rights reserved.
//

#import "WheelphoneBasicAppDelegate.h"
#import "WheelphoneBasicViewController.h"

@implementation WheelphoneBasicAppDelegate

@synthesize window;
@synthesize viewController;

- (void)applicationDidFinishLaunching:(UIApplication *)application {
    
    // Override point for customization after app launch    
    [window addSubview:viewController.view];
    [window makeKeyAndVisible];
}

- (void)applicationDidBecomeActive:(NSNotification *)notification {

}

- (void)dealloc {
    [viewController release];
    [window release];
    [super dealloc];
}

@end
