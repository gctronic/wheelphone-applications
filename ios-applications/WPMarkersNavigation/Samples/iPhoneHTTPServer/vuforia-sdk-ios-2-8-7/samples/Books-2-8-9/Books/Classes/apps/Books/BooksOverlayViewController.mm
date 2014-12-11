/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <AVFoundation/AVFoundation.h>
#import <QCAR/QCAR.h>
#import <QCAR/CameraDevice.h>
#import "BooksOverlayViewController.h"
#import "BooksManager.h"
#import "TargetOverlayView.h"
#import "BooksViewController.h"
#import "BooksEAGLView.h"
#import "BookWebDetailViewController.h"
#import "ImagesManager.h"
#import "OverlayView.h"

@interface BooksOverlayViewController()
- (void)centerViewInFrame;
@end

@implementation BooksOverlayViewController

@synthesize targetOverlayView;

#pragma mark - Private

- (id)initWithDelegate:(id<BooksControllerDelegateProtocol>) delegate
{
    self = [super init];
    if (self) {
        booksDelegate = delegate;
    }
    return self;
}

- (void) addLoadingView
{
    //  Adds basic spining wheel that appears every time the user scans a book
    
    float loadingViewWidth = 100;
    float loadingViewHeight = 100;
    
    //  Initiate loadingView
    loadingView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, loadingViewWidth, loadingViewHeight)];
    loadingView.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.5];
    loadingView.layer.cornerRadius = 10;
    
    //  Initiate activity indicator
    UIActivityIndicatorView *anActivityIndicator = [[[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge] autorelease];
    [anActivityIndicator startAnimating];
    anActivityIndicator.center = loadingView.center;
    
    [loadingView addSubview:anActivityIndicator];
    
    loadingView.autoresizingMask = UIViewAutoresizingFlexibleBottomMargin |
                                    UIViewAutoresizingFlexibleTopMargin |
                                    UIViewAutoresizingFlexibleLeftMargin |
                                    UIViewAutoresizingFlexibleRightMargin;
    
    //  Add loading view to the overlay view
    loadingView.center = self.view.center;
    
    [self.view addSubview:loadingView];
}

- (void)removeBookFromScreen
{
    closeButton.hidden = YES;
    
    [self.targetOverlayView setHidden:YES];
    
    [booksDelegate enterScanningMode];
    [booksDelegate setLastScannedBook:nil];
    [booksDelegate setLastTargetIDScanned:nil];
}

- (void)centerViewInFrame
{
    CGFloat containerWidth = self.view.frame.size.width;
    CGFloat containerHeight = self.view.frame.size.height;
        
    CGRect newFrame = CGRectMake((containerWidth - targetOverlayView.frame.size.width) / 2,
                                (containerHeight - targetOverlayView.frame.size.height) / 2,
                                targetOverlayView.frame.size.width,
                                targetOverlayView.frame.size.height);
    
    self.targetOverlayView.frame = newFrame;
}

- (void) refreshStatusLabel:(NSTimer *)theTimer
{
    // You can use this method to display a requesting state on the screen
    //    statusLabel.hidden = NO;
    //    or
    //    statusLabel.hidden = YES;
}

- (CGRect) rectForStatusLabelWithString:(NSString *)aString
{
    UIFont *statusLabelFont = nil;
    float statusLabelBottomMargin = 0;
    
    if([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad)
    {
        statusLabelFont = [UIFont fontWithName:@"Helvetica" size:18];
        statusLabelBottomMargin = 75;
    }
    else
    {
        statusLabelFont = [UIFont fontWithName:@"Helvetica" size:14];
        statusLabelBottomMargin = 40;
    }
    
    float statusLabelHeight = [aString sizeWithFont:statusLabelFont].height * 2.25;
    float statusLabelWidth = [aString sizeWithFont:statusLabelFont].width * 1.75;
    
    CGRect aRect = CGRectMake(self.view.frame.size.width/2 - statusLabelWidth/2,
                              self.view.frame.size.height - statusLabelHeight - statusLabelBottomMargin,
                              statusLabelWidth,
                              statusLabelHeight);
    
    return aRect;
}

- (void) addStatusLabel
{
    //  Adds "Requesting" black label at the bottom of the screen
    
    NSString *statusLabelText = @"Requesting";
    
    CGRect aRect = [self rectForStatusLabelWithString:statusLabelText];
    statusLabel = [[[UILabel alloc] initWithFrame:aRect] autorelease];
    
    statusLabel.text = statusLabelText;
    statusLabel.textAlignment =  NSTextAlignmentCenter;
    statusLabel.textColor = [UIColor whiteColor];
    statusLabel.backgroundColor = [UIColor colorWithRed:0 green:0 blue:0 alpha:0.5];
    statusLabel.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin |
                                    UIViewAutoresizingFlexibleRightMargin |
                                    UIViewAutoresizingFlexibleTopMargin;
    //  Hide it by default
    statusLabel.hidden = YES;
    
    [self.view addSubview:statusLabel];
    statusTimer = [[NSTimer scheduledTimerWithTimeInterval:1.0
                                                        target:self
                                                      selector:@selector(refreshStatusLabel:)
                                                      userInfo:nil
                                                       repeats:YES] retain];
}

- (void)addCloseButton
{
    //  Adds close button on the upper right corner
    
    UIImage *closeButtonImage = [UIImage imageNamed:@"button_close_normal.png"];
    UIImage *closeButtonTappedImage = [UIImage imageNamed:@"button_close_pressed.png"];
    
    CGRect aRect = CGRectMake(self.view.frame.size.width - closeButtonImage.size.width,
                              0,
                              closeButtonImage.size.width,
                              closeButtonImage.size.height);
    
    closeButton = [UIButton buttonWithType:UIButtonTypeCustom];
    closeButton.frame = aRect;
    
    [closeButton setImage:closeButtonImage forState:UIControlStateNormal];
    [closeButton setImage:closeButtonTappedImage forState:UIControlStateHighlighted];
    
    closeButton.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin;
    
    [closeButton addTarget:self action:@selector(closeButtonTapped:) forControlEvents:UIControlEventTouchUpInside];
    
    [self.view addSubview:closeButton];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Support all orientations
    return YES;
}

#pragma mark - Public

- (void) loadView
{
    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    optionsOverlayView = [[OverlayView alloc] initWithFrame: screenBounds];
    self.view = optionsOverlayView;
    
    // We're going to let the parent VC handle all interactions so disable any UI
    // Further on, we'll also implement a touch pass-through
    self.view.userInteractionEnabled = NO;
}

- (void) handleViewRotation:(UIInterfaceOrientation)interfaceOrientation
{
    // adjust the size according to the rotation
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGRect overlayRect = screenRect;
    
    if (interfaceOrientation == UIInterfaceOrientationLandscapeLeft || interfaceOrientation == UIInterfaceOrientationLandscapeRight)
    {
        overlayRect.size.width = screenRect.size.height;
        overlayRect.size.height = screenRect.size.width;
    }
    
    optionsOverlayView.frame = overlayRect;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self addStatusLabel];
    [self addCloseButton];
    [self addLoadingView];
    
    //  Loading view should be visible only on the requests
    loadingView.hidden = YES;
    
    //  Just show closeButton when an AR book is on display
    closeButton.hidden = YES;
    
    [self.view setUserInteractionEnabled:YES];
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(targetFound:) name:@"kTargetFound" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(targetLost:) name:@"kTargetLost" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(targetReacquired:) name:@"kTargetReacquired" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(bookWebDetailDismissed:) name:@"kBookWebDetailDismissed" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(startLoading:) name:@"kStartLoading" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(stopLoading:) name:@"kStopLoading" object:nil];
}

-(void) dealloc
{
    self.targetOverlayView = nil;
    [statusTimer invalidate];
    [statusTimer release];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    
    [super dealloc];
}

#pragma mark - Notifications
- (void)startLoading:(NSNotification *)notification
{
    dispatch_async(dispatch_get_main_queue(), ^{
        loadingView.hidden = NO;
        closeButton.hidden = NO;
    });
}

- (void)stopLoading:(NSNotification *)notification
{
    dispatch_async(dispatch_get_main_queue(), ^{
        loadingView.hidden = YES;
        [self removeBookFromScreen];
    });
}

- (void)targetFound:(NSNotification *)notification
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.targetOverlayView = [[[NSBundle mainBundle] loadNibNamed:@"targetOverlayView" owner:nil options:nil] objectAtIndex:0];
        
        Book *aBook = [notification object];
        [self.targetOverlayView setBook:aBook];
        [booksDelegate setLastScannedBook:aBook];
        
        [booksDelegate setOverlayLayer:self.targetOverlayView.layer];
        [self.view addSubview:self.targetOverlayView];
        [self.targetOverlayView setHidden:YES];
        
        [self centerViewInFrame];
        
        loadingView.hidden = YES;
        
        //  Show close button
        closeButton.hidden = NO;
    });
}

- (void)targetReacquired:(NSNotification *)notification
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.targetOverlayView setHidden:YES];
    });
}

- (void)targetLost:(NSNotification *)notification
{
    dispatch_async(dispatch_get_main_queue(), ^{    
        [self.targetOverlayView setHidden:NO];
    });
}

- (void)bookWebDetailDismissed:(NSNotification *)notification
{
    // we don't do anything here: when the book detail webView is dismissed,
    // the 2D book overlay will popup
    // Uncomment this line if you want to reenter scanning mode
    //[self removeBookFromScreen];
    //[self handleViewRotation:self.interfaceOrientation];
}

#pragma mark - Actions

- (void) closeButtonTapped:(id)sender
{
    if (YES == [[BooksManager sharedInstance] isNetworkOperationInProgress])
    {
        // Cancel the network operation
        [[BooksManager sharedInstance] cancelNetworkOperations:YES];
    }
    else
    {
        [self removeBookFromScreen];
    }
}
@end
