/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>
#import "Book.h"

@interface BookWebDetailViewController : UIViewController
{
    IBOutlet UIWebView *webView;
    IBOutlet UINavigationBar *navigationBar;
    
    Book *book;
}

-(id)initWithBook:(Book *)aBook;

@property (retain) Book *book;

- (IBAction)doneButtonTapped:(id)sender;
@end
