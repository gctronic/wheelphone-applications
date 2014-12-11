/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import "TargetOverlayView.h"
#import <QuartzCore/QuartzCore.h>
#import "Book.h"
#import "StarRatingView.h"
#import "ImagesManager.h"

@implementation TargetOverlayView

@synthesize titleLabel;
@synthesize authorsLabel;
@synthesize ratingsLabel;
@synthesize priceLabel;
@synthesize oldPriceLabel;
@synthesize priceContainerView;
@synthesize starRatingView;
@synthesize bookCoverImageView;

- (void)dealloc {
    self.titleLabel = nil;
    self.authorsLabel = nil;
    self.ratingsLabel = nil;
    self.priceLabel = nil;
    self.priceContainerView = nil;
    self.oldPriceLabel = nil;
    self.starRatingView = nil;
    self.bookCoverImageView = nil;
    
    [super dealloc];
}

- (void)awakeFromNib {
    [super awakeFromNib];
    self.priceContainerView.layer.affineTransform = CGAffineTransformMakeRotation(0.26f);
}

- (void)setBook:(Book *)book {
    
    self.titleLabel.text = book.title;
    self.authorsLabel.text = book.author;
    self.ratingsLabel.text = [NSString stringWithFormat:@"(%d ratings)", book.ratingsQuantity];
    self.priceLabel.text = book.yourPriceString;
    self.oldPriceLabel.text = book.listPriceString;
    self.bookCoverImageView.image = [[ImagesManager sharedInstance] cachedImageFromURL:book.thumbnailURL];
    
    [self.starRatingView setRating:(int)book.ratingAverage];
    
    [self setNeedsDisplay];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [[NSNotificationCenter defaultCenter] postNotificationName:@"kTargetOverlayViewTapped" object:nil];
}

@end
