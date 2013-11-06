//
//  ViewController.h
//  WPBlobTracking
//
//  Created by Stefano Morgani on 22.10.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <opencv2/highgui/cap_ios.h>
using namespace cv;
#import <UIKit/UIKit.h>
#include "UIView+ColorOfPoint.h"

@interface ViewController : UIViewController<CvVideoCameraDelegate> {
    CvVideoCamera* videoCamera;
    UIColor* pickedColor;
}

@property (weak, nonatomic) IBOutlet UIImageView *imageView;
@property (weak, nonatomic) IBOutlet UIButton *btnStart;
@property (nonatomic, retain) CvVideoCamera* videoCamera;
@property (nonatomic, retain) UIColor* pickedColor;

- (IBAction)actionStart:(UIButton *)sender;

@end
