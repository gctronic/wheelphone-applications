/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <Foundation/Foundation.h>

@interface Texture : NSObject {
    int width;
    int height;
    int channels;
    int textureID;
    unsigned char* pngData;
}

@property (nonatomic, readonly) int width;
@property (nonatomic, readonly) int height;
@property (nonatomic) int textureID;
@property (nonatomic, readonly) unsigned char* pngData;

- (BOOL)loadImage:(NSString*)filename;

@end
