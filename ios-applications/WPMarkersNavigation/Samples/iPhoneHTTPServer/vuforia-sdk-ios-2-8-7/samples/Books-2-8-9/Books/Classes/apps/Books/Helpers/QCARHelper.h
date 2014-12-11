/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>

typedef enum {
    kTargetStatusRequesting,
    kTargetStatusNone
} TargetStatus;

@interface QCARHelper : NSObject
{
    
}

+(TargetStatus)targetStatus;
+(NSString*) errorStringFromCode:(int) code;

+ (void) startDetection;
+ (void) stopDetection;

+ (BOOL) isRetinaDevice;
@end
