/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <QuartzCore/QuartzCore.h>

#import "ButtonOverlayViewController.h"
#import "Dominoes.h"
#import "OverlayViewController.h"

bool isInterfaceOrientationPortrait=false;
@implementation ButtonOverlayViewController

@synthesize menuButton, resetButton, runButton, clearButton, deleteButton, messageLabel;

// Implement loadView to create a view hierarchy programmatically, without using a nib.
- (void)loadView
{

    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    self.view = [[UIView alloc] initWithFrame: screenBounds];
     
    // MenuButton
    menuButton= [self createConfigureMenuButton:@"Menu"];
    
    // Clear Button
    clearButton= [self createConfigureMenuButton:@"Clear"];
       
    // Reset Button
    resetButton= [self createConfigureMenuButton:@"Reset"];
   
    // Run Button
    runButton= [self createConfigureMenuButton:@"Run"];
   
    // Delete Button
    deleteButton= [self createConfigureMenuButton:@"Delete"];
    deleteButton.hidden=true;
    
    // Message Label
    messageLabel = [[UILabel alloc] init];
    messageLabel.hidden=true;
  
    [self.view addSubview:menuButton];
    [self.view addSubview:clearButton];
    [self.view addSubview:resetButton];
    [self.view addSubview:runButton];
    [self.view addSubview:deleteButton];
    [self.view addSubview:messageLabel];
    
}

-(UIButton*) createConfigureMenuButton:(NSString*)buttonTitle
{
    NSString* selectorName= @"press";
    selectorName=[selectorName stringByAppendingString:buttonTitle];
    selectorName = [selectorName stringByAppendingString:@"Button"];
    UIButton* customButton= [[UIButton buttonWithType:UIButtonTypeRoundedRect] retain];
    [customButton addTarget:self action:NSSelectorFromString(selectorName) forControlEvents:UIControlEventTouchUpInside];
    [customButton setTitle:buttonTitle forState:UIControlStateNormal];   
    customButton.titleLabel.font= [UIFont fontWithName:@"Helvetica-Bold" size:15.0];
    customButton.hidden = ![OverlayViewController doesOverlayHaveContent];
    [customButton.layer setBorderWidth:0];
    customButton.contentHorizontalAlignment = UIControlContentHorizontalAlignmentCenter;
    return customButton;
    
}
- (void) setMenuCallBack:(SEL)callback forTarget:(id)target
{
    menuId = target;
    menuSel = callback;
}

- (void) updateButtonStatus
{
    bool notRunning = !dominoesIsSimulating();
    clearButton.hidden = !(notRunning && dominoesHasDominoes());
    runButton.hidden = !(notRunning && dominoesHasDominoes() && !dominoesHasRun());
    resetButton.hidden = !(notRunning && dominoesHasDominoes() && dominoesHasRun());
}

- (void) viewDidLoad
{
    [self updateButtonStatus];
    
}

- (void) viewDidAppear:(BOOL)animated
{
    [self updateButtonStatus];
}

- (void) pressMenuButton
{
    if ((menuId != nil) && [menuId respondsToSelector:menuSel])
    {
        [menuId performSelector:menuSel];
    }
}

- (void) pressResetButton
{
    dominoesReset();
    [self updateButtonStatus];
}


- (void) pressRunButton
{
    if (dominoesHasDominoes())
        dominoesStart();   
    [self updateButtonStatus];
}


- (void) pressClearButton
{
    dominoesClear();
    [self updateButtonStatus];
}


- (void) pressDeleteButton
{
    dominoesDelete();
    [self updateButtonStatus];
}


- (void) showDeleteButton
{
    deleteButton.hidden = NO;
}


- (void) hideDeleteButton
{
    deleteButton.hidden = YES;
}

#define FADE_DURATION 0.5
#define SHOW_DURATION 5.0

- (void) showMessage:(NSString *)theMessage
{
    if (!isInterfaceOrientationPortrait)
    {
        messageLabel.frame = CGRectMake(self.view.frame.size.height/2-130,self.view.frame.size.height/2-10,415, 30);
        messageLabel.font= [UIFont fontWithName:@"Helvetica-Bold" size:17.0];
    }
    else
    {
        messageLabel.frame = CGRectMake(self.view.frame.size.height/2-230,self.view.frame.size.width/2+30,300, 30);
        messageLabel.font= [UIFont fontWithName:@"Helvetica-Bold" size:10.0];
    }
    [messageLabel setNumberOfLines:0];
    [messageLabel setLineBreakMode:UILineBreakModeWordWrap];
    messageLabel.backgroundColor = [UIColor lightGrayColor];
    
    messageLabel.layer.borderColor = [UIColor whiteColor].CGColor;
    messageLabel.layer.borderWidth = 2.0;
    messageLabel.layer.cornerRadius = 4.0;
    messageLabel.clipsToBounds = YES;
    messageLabel.textColor = [UIColor whiteColor];
    messageLabel.textAlignment = UITextAlignmentCenter;
    
    messageLabel.text = theMessage;
    messageLabel.alpha = 0.0;
    messageLabel.hidden = NO;

    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:FADE_DURATION];
    messageLabel.alpha = 1.0;
    [UIView commitAnimations];
    
    messageTimer = [NSTimer scheduledTimerWithTimeInterval:FADE_DURATION + SHOW_DURATION target:self selector:@selector(hideMessageOnTimeout:) userInfo:nil repeats:NO];
}

- (void) hideMessage
{
    [UIView beginAnimations:nil context:NULL];
    [UIView setAnimationDuration:FADE_DURATION];
    [UIView  setAnimationDelegate:self];
    [UIView setAnimationDidStopSelector:@selector(messageFadedOut:finished:context:)];
    messageLabel.alpha = 0.0;
    [UIView commitAnimations];    
}

- (void) messageFadedOut:(NSString *)animationID finished:(BOOL)finished context:(void *)context
{
    messageLabel.hidden = YES;
}

- (void) hideMessageOnTimeout:(NSTimer *)theTimer
{
    messageTimer = nil;
    [self hideMessage];
}

- (void)dealloc {
    [menuButton release];
    [resetButton release];
    [runButton release];
    [clearButton release];
    [deleteButton release];
    [messageLabel release];
    messageLabel = nil;
    if (messageTimer != nil)
    {
        [messageTimer invalidate];
        messageTimer = nil;
    }
    
    [super dealloc];
}

- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesBegan:touches withEvent:event];
}

@end
