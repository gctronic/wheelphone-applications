/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

@interface SampleAppAboutViewController : UIViewController <UIWebViewDelegate>
@property (retain, nonatomic) IBOutlet UIWebView *uiWebView;
@property (nonatomic, copy) NSString * appTitle;
@property (nonatomic, copy) NSString * appAboutPageName;
@property (nonatomic, copy) NSString * appViewControllerClassName;


- (IBAction)startButtonTapped:(id)sender;

@end
