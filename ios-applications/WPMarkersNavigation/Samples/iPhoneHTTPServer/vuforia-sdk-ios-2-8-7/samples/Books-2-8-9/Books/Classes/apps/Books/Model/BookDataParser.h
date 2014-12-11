/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <Foundation/Foundation.h>
#import "Book.h"

//  IMPORTANT: BookDataParser is written to parse data specific to the Books
//  sample application and is not designed to be used in other applications.

@interface BookDataParser : NSObject

+(NSDictionary *)parseData:(NSData *)dataToParse;
+(NSDictionary *)parseString:(NSString *)stringToParse;

@end
