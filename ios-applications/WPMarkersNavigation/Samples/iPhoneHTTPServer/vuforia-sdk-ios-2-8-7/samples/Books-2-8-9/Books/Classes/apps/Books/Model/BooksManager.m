/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import "BooksManager.h"
#import "ImagesManager.h"
#import "BooksManagerDelegateProtocol.h"
#import "BookDataParser.h"

@implementation BooksManager

@synthesize cancelNetworkOperation, networkOperationInProgress;

#define BOOKSJSONURL @"https://developer.vuforia.com/samples/cloudreco/json"

static BooksManager *sharedInstance = nil;

#pragma mark - Public

-(void)bookWithJSONFilename:(NSString *)jsonFilename withDelegate:(id <BooksManagerDelegateProtocol>)aDelegate forTrackableID:(const char *)trackableID
{
    networkOperationInProgress = YES;
    
    //  Get URL
    NSString *anURLString = [NSString stringWithFormat:@"%@/%@", BOOKSJSONURL, jsonFilename];
    NSURL *anURL = [NSURL URLWithString:anURLString];
    
    [self infoForBookAtURL:anURL withDelegate:aDelegate forTrackableID:trackableID];
}

-(void)infoForBookAtURL:(NSURL* )url withDelegate:(id <BooksManagerDelegateProtocol>)aDelegate forTrackableID:(const char*)trackable
{
    // Store the delegate
    delegate = aDelegate;
    [delegate retain];
    
    // Store the trackable ID
    thisTrackable = [[NSString alloc] initWithCString:trackable encoding:NSASCIIStringEncoding];
    
    // Download the book info
    [self asyncDownloadInfoForBookAtURL:url];
}

-(void)asyncDownloadInfoForBookAtURL:(NSURL *)url
{
    // Download the info for this book
    NSMutableURLRequest *request = [[[NSMutableURLRequest alloc] initWithURL:url] autorelease];
    [request setHTTPMethod:@"GET"];
    
    // Do not start the network operation immediately
    NSURLConnection *aConnection = [[NSURLConnection alloc] initWithRequest:request delegate:self startImmediately:NO];
    
    // Use the run loop associated with the main thread
    [aConnection scheduleInRunLoop:[NSRunLoop mainRunLoop] forMode:NSDefaultRunLoopMode];
    
    // Start the network operation
    [aConnection start];
}

-(void)addBadTargetId:(const char*)aTargetId
{
    NSString *tid = [NSString stringWithUTF8String:aTargetId];
    
    if (tid)
    {
        [badTargets addObject:tid];
    }
}

-(BOOL)isBadTarget:(const char*)aTargetId
{
    BOOL retVal = NO;
    NSString *tid = [NSString stringWithUTF8String:aTargetId];
    
    if (tid)
    {
        retVal = [badTargets containsObject:tid];
        
        if (retVal)
        {
            NSLog(@"#DEBUG bad target found");
        }
    }
    else
    {
        NSLog(@"#DEBUG error: could not convert const char * to NSString");
    }
    
    return retVal;
}

-(id)init
{
    self = [super init];
    if (self)
    {
        badTargets = [[NSMutableSet alloc] init];
    }
    return self;
}

+(BooksManager *)sharedInstance
{
	@synchronized(self)
    {
		if (sharedInstance == nil)
        {
			sharedInstance = [[self alloc] init];
		}
	}
	return sharedInstance;
}

-(BOOL)isNetworkOperationInProgress
{
    // The BooksManager or ImagesManager may have a network operation in
    // progress
    return networkOperationInProgress | [[ImagesManager sharedInstance] networkOperationInProgress] ? YES : NO;
}

-(void)cancelNetworkOperations:(BOOL)cancel
{
    // Set or clear the cancel flags, which will be checked in each network
    // callback
    
    // BooksManager (self)
    cancelNetworkOperation = cancel;
    
    // ImagesManager
    [[ImagesManager sharedInstance] setCancelNetworkOperation:cancel];
}

-(void)infoDownloadDidFinishWithBookData:(NSData *)bookData withConnection:(NSURLConnection *)connection
{
    Book *book = nil;
    
    if (bookData)
    {
        //  Given a NSData, parse the book to a dictionary and then convert it into a Book object
        NSError *anError = nil;
        NSDictionary *bookDictionary = nil;
        
        //  Find out on runtime if the device can use NSJSONSerialization (iOS5 or later)
        NSString *className = @"NSJSONSerialization";
        Class class = NSClassFromString(className);
        
        if (!class)
        {
            //  Use custom BookDataParser.
            //
            //  IMPORTANT: BookDataParser is written to parse data specific to the Books
            //  sample application and is not designed to be used in other applications.
            
            bookDictionary = [BookDataParser parseData:bookData];
            NSLog(@"#DEBUG Using custom JSONBookParser");
        }
        else
        {
            //  Use native JSON parser, NSJSONSerialization
            bookDictionary = [NSJSONSerialization JSONObjectWithData: bookData
                                                             options: NSJSONReadingMutableContainers
                                                               error: &anError];
            NSLog(@"#DEBUG Using NSJSONSerialization");
        }

        
        if (!bookDictionary)
        {
            NSLog(@"#DEBUG Error parsing JSON: %@", anError);
        }
        else
        {
            book = [[[Book alloc] initWithDictionary:bookDictionary] autorelease];
        }
    }
    
    //  Inform the delegate that the request has completed
    [delegate infoRequestDidFinishForBook:book withTrackableID:[thisTrackable cStringUsingEncoding:NSASCIIStringEncoding] byCancelling:[self cancelNetworkOperation]];
    
    if (YES == [self cancelNetworkOperation])
    {
        // Inform the ImagesManager that the network operation has already been
        // cancelled (so its network operation will not be started and therefore
        // does not need to be cancelled)
        [self cancelNetworkOperations:NO];
    }
    
    // Release objects associated with the completed network operation
    [thisTrackable release];
    thisTrackable = nil;
    
    [delegate release];
    delegate = nil;
    
    [bookInfo release];
    bookInfo = nil;
    
    //  We don't need this connection reference anymore
    [connection release];
    
    networkOperationInProgress = NO;
}

#pragma mark NSURLConnectionDelegate
// *** These delegate methods are always called on the main thread ***
-(void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    [self infoDownloadDidFinishWithBookData:nil withConnection:connection];
}

-(void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    NSData *bookData = nil;
    
    if (YES == [self cancelNetworkOperation])
    {
        // Cancel this connection
        [connection cancel];
    }
    else if (bookInfo)
    {
        bookData = [NSData dataWithData:bookInfo];
    }
    
    [self infoDownloadDidFinishWithBookData:bookData withConnection:connection];
}

-(void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data
{
    if (YES == [self cancelNetworkOperation])
    {
        // Cancel this connection
        [connection cancel];
        
        [self infoDownloadDidFinishWithBookData:nil withConnection:connection];
    }
    else
    {
        if (nil == bookInfo)
        {
            bookInfo = [[NSMutableData alloc] init];
        }

        [bookInfo appendData:data];
    }
}


#pragma mark Singleton overrides

+ (id)allocWithZone:(NSZone *)zone
{
    //  Overriding this method for singleton
    
	@synchronized(self)
    {
		if (sharedInstance == nil)
        {
			sharedInstance = [super allocWithZone:zone];
			return sharedInstance;
		}
	}
	return nil;
}

- (id)copyWithZone:(NSZone *)zone
{
    //  Overriding this method for singleton
	return self;
}

- (id)retain
{
    //  Overriding this method for singleton
    return self;
}

- (NSUInteger)retainCount
{
    //  Overriding this method for singleton
	return NSUIntegerMax;
}

- (oneway void)release
{
    //  Overriding this method for singleton
}

- (id)autorelease
{
    //  Overriding this method for singleton
	return self;
}


@end
