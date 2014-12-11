/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

@class SampleAppSlidingMenuController;

@interface SampleAppLeftMenuViewController : UIViewController {
    id observer;
}

@property(nonatomic,strong) UITableView *tableView;

@property(nonatomic,strong) SampleAppSlidingMenuController *slidingMenuViewController;

- (void) updateMenu;

@end
