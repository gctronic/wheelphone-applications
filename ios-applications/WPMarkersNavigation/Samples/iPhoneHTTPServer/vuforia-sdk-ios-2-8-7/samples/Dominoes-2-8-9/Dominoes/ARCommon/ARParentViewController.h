/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>

@class ARViewController, OverlayViewController;

@interface ARParentViewController : UIViewController {
    OverlayViewController* overlayViewController; // for the overlay view (buttons and action sheets)
    ARViewController* arViewController; // for the Augmented Reality view
    UIImageView* parentView; // a container view to allow use in tabbed views etc.
    
    CGRect arViewRect; // the size of the AR view
    
    // Splash view
    UIImageView* splashView;
    UIWindow* appWindow;
}

@property (nonatomic) CGRect arViewRect;

- (id)initWithWindow:(UIWindow*)window;
- (void)createParentViewAndSplashContinuation;
- (void)endSplash:(NSTimer*)theTimer;
- (void)updateSplashScreenImageForLandscape;
- (BOOL)isRetinaEnabled;
- (void)freeOpenGLESResources;

@end
