/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import "BookDataParser.h"
#import "Book.h"

@implementation BookDataParser

#define kBookParseTitleKey @"title"
#define kBookParseAuthorKey @"author"
#define kBookParseAverageRatingKey @"average rating"
#define kBookParseNumberOfRatingsKey @"# of ratings"
#define kBookParseListPriceKey @"list price"
#define kBookParseYourPriceKey @"your price"
#define kBookParseThumbUrlKey @"thumburl"
#define kBookParseUrlKey @"bookurl"

#pragma mark - Private

+(NSString *)stringBetweenDoubleQuotes:(NSString *)val
{
    NSString *retVal = nil;
    
    //  Get index of first quote
    NSInteger initialIndex = [val rangeOfString:@"\""].location;
    
    //  Get index of last quote
    NSRange rangeForFinalIndex = NSMakeRange(initialIndex+1, [val length] - initialIndex - 1);
    NSInteger finalIndex = [val rangeOfString:@"\"" options:NSLiteralSearch range:rangeForFinalIndex].location;
    
    //  Get range of string between quotes
    NSRange substringRange = NSMakeRange(initialIndex+1, finalIndex - initialIndex -1);

    //  Get substring
    retVal = [val substringWithRange:substringRange];
    return retVal;
}

+(NSString *)keyFromLine:(NSString *)aJSONLine
{
    NSArray *elements = [aJSONLine componentsSeparatedByString:@":"];
    NSString *retVal = [BookDataParser stringBetweenDoubleQuotes:[elements objectAtIndex:0]];
    return retVal;
}

+(NSString *)valueFromLine:(NSString *)aJSONLine isURL:(BOOL)isURL
{
    NSArray *elements = [aJSONLine componentsSeparatedByString:@":"];
    NSString *stringToTransform = nil;

    if (isURL)
    {
        stringToTransform = [NSString stringWithFormat:@"%@:%@", [elements objectAtIndex:1], [elements objectAtIndex:2]];
    }
    else
    {
        stringToTransform = [elements objectAtIndex:1];
    }
    
    NSString *retVal = [BookDataParser stringBetweenDoubleQuotes:stringToTransform];
    return retVal;
}

#pragma mark - Public

+(NSDictionary *)parseData:(NSData *)dataToParse
{
    NSString *stringToParse = [[[NSString alloc] initWithData:dataToParse encoding:NSUTF8StringEncoding] autorelease];
    NSDictionary *retVal = [BookDataParser parseString:stringToParse];
    return retVal;
}

+(NSDictionary *)parseString:(NSString *)stringToParse
{    
    NSMutableDictionary *tmpDict = [[[NSMutableDictionary alloc] init] autorelease];
    
    NSArray *jsonLines = [stringToParse componentsSeparatedByString:@","];
    
    for(NSString *line in jsonLines)
    {
        NSString *jsonKey = [BookDataParser keyFromLine:line];
        NSString *jsonValue = nil;
        
        if ([jsonKey isEqualToString:kBookParseThumbUrlKey] ||
            [jsonKey isEqualToString:kBookParseUrlKey])
        {
            jsonValue = [BookDataParser valueFromLine:line isURL:YES];
        }
        else
        {
            jsonValue = [BookDataParser valueFromLine:line isURL:NO];
        }
        
        [tmpDict setObject:jsonValue forKey:jsonKey];
    }
    
    NSDictionary *retVal = [NSDictionary dictionaryWithDictionary:tmpDict];
    return retVal;
}

@end
