/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

@class QCARutils;

// OverlayViewController class overrides one UIViewController method
@interface OverlayViewController : UIViewController <UIActionSheetDelegate> 
{
@protected
    UIView *optionsOverlayView; // the view for the options pop-up
    UIActionSheet *mainOptionsAS; // the options menu
    NSInteger selectedTarget; // remember the selected target so we can mark it
    NSInteger selectTargetIx; // index of the option that is 'Select Target'
    NSInteger autofocusContIx;  // index of camera continuous autofocus button
    NSInteger autofocusSingleIx; // index of single-shot autofocus button (not used on most samples)
    NSInteger flashIx;  // index of camera flash button (not used on most samples)
////STRIP_BEGIN
#ifdef AR_PRIVATE_BUILD
    NSInteger swapCameraIx;     // index of back/front camera swap button
#endif
////STRIP_END
    
    struct tagCameraCapabilities {
        BOOL autofocus;
        BOOL autofocusContinuous;
        BOOL torch;	
    } cameraCapabilities;
    
    enum { MENU_OPTION_WANTED = -1, MENU_OPTION_UNWANTED = -2 };
    
    QCARutils *qUtils;
}

- (void) handleViewRotation:(UIInterfaceOrientation)interfaceOrientation;
- (void) showOverlay;
- (void) populateActionSheet;
+ (BOOL) doesOverlayHaveContent;

// UIActionSheetDelegate event handlers (accessible by subclasses)
- (void) mainOptionClickedButtonAtIndex:(NSInteger)buttonIndex;

@end
