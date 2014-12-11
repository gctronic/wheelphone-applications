/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <Foundation/Foundation.h>
#import "Book.h"

@protocol BooksControllerDelegateProtocol <NSObject>

- (NSString *) lastTargetIDScanned;

- (void) setLastTargetIDScanned:(NSString *) targetId;

- (BOOL) isVisualSearchOn;

- (void) setVisualSearchOn:(BOOL) isOn;

- (void) enterScanningMode;

- (void) setLastScannedBook: (Book *) book;

- (void)setOverlayLayer:(CALayer *)overlayLayer;

@end
