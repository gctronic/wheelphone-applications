/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <Foundation/Foundation.h>
#import "Book.h"

@protocol BooksManagerDelegateProtocol <NSObject>

-(void)infoRequestDidFinishForBook:(Book *)theBook withTrackableID:(const char*)trackable byCancelling:(BOOL)cancelled;

@end
