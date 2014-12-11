/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <Foundation/Foundation.h>
#import "ImagesManagerDelegateProtocol.h"


@interface ImagesManager : NSObject <NSURLConnectionDelegate, NSURLConnectionDataDelegate>
{
    NSMutableData *bookImage;
    Book *thisBook;
    id <ImagesManagerDelegateProtocol> delegate;
}

@property (readwrite, nonatomic) BOOL cancelNetworkOperation;
@property (readonly, nonatomic) BOOL networkOperationInProgress;

+(id)sharedInstance;
-(void)imageForBook:(Book *)theBook withDelegate:(id <ImagesManagerDelegateProtocol>)aDelegate;
-(UIImage *)cachedImageFromURL:(NSString*)anURLString;

@end
