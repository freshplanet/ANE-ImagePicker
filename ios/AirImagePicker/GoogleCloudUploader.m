//
//  GoogleCloudUploader.m
//  AirImagePicker
//
//  Created by Daniel Rodriguez on 5/30/13.
//  Copyright (c) 2013 FreshPlanet. All rights reserved.
//

#import "GoogleCloudUploader.h"

@implementation GoogleCloudUploader

- (void) startUpload:(NSURL*)mediaURL withUploadURL:(NSURL*)uploadURL andUploadParams:(NSDictionary*)params
{
    NSLog(@"Entering startUpload:withUploadURL:andUploadParams");
    
    // Obtain the NSData of the file we want
    NSData *mediaData = [[NSFileManager defaultManager] contentsAtPath:[mediaURL path]];
    
    // Build a NSData containing the http post data object
    NSArray *myDictKeys = [params allKeys];
    NSMutableData *myData = [NSMutableData dataWithCapacity:1];
    NSData *boundary = [@"" dataUsingEncoding:NSUTF8StringEncoding];
    NSData *lineBreak = [@"\r\n" dataUsingEncoding:NSUTF8StringEncoding];
    
    for (int i =0; i < [myDictKeys count]; i++) {
        id myValue = [params valueForKey:[myDictKeys objectAtIndex:i]];
        [myData appendData:boundary];
        [myData appendData:lineBreak];
        [myData appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"", [myDictKeys objectAtIndex:i]] dataUsingEncoding:NSUTF8StringEncoding]];
        [myData appendData:lineBreak];
        [myData appendData:lineBreak];
        [myData appendData:[[NSString stringWithFormat:@"%@", myValue] dataUsingEncoding:NSUTF8StringEncoding]];
        [myData appendData:lineBreak];
    }
    
    [myData appendData:boundary];
    [myData appendData:lineBreak];
    [myData appendData:[@"Content-Disposition: form-data; name=\"file\"; filename=\"file\"" dataUsingEncoding:NSUTF8StringEncoding]];
    [myData appendData:lineBreak];
    [myData appendData:lineBreak];
    [myData appendData:mediaData];
    [myData appendData:lineBreak];
    [myData appendData:boundary];
    
    // closing boundary
    [myData appendData:boundary];
    [myData appendData:[@"--" dataUsingEncoding:NSUTF8StringEncoding]];
    
    // connect to Google Cloud Storage
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:uploadURL];
    [request setHTTPMethod:@"POST"];
    [request setHTTPBody:myData];
    
    [[NSURLConnection alloc] initWithRequest:request delegate:self];
    
    NSLog(@"Exiting startUpload:withUploadURL:andUploadParams");
}

#pragma mark NSURLConnection Delegate Methods

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    // A response has been received, this is where we initialize the instance var you created
    // so that we can append data to it in the didReceiveData method
    // Furthermore, this method is called each time there is a redirect so reinitializing it
    // also serves to clear it
    NSLog(@"Entering connection:didReceiveResponse");
    _responseData = [[NSMutableData alloc] init];
    NSLog(@"Exiting connection:didReceiveResponse");
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    // Append the new data to the instance variable you declared
    NSLog(@"Entering connection:didReceiveData");
    [_responseData appendData:data];
    NSLog(@"Exiting connection:didReceiveData");
}

- (NSCachedURLResponse *)connection:(NSURLConnection *)connection
                  willCacheResponse:(NSCachedURLResponse*)cachedResponse {
    // Return nil to indicate not necessary to store a cached response for this connection
    NSLog(@"Entering connection:willCacheResponse");
    return nil;
    NSLog(@"Exiting connection:willCacheResponse");
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    // The request is complete and data has been received
    // You can parse the stuff in your instance variable now
    NSLog(@"Entering connectionDidFinishLoading");
    
    [AirImagePicker status:@"FILE_UPLOAD_DONE" level:@"OK"];
    
    NSLog(@"Exiting connectionDidFinishLoading");
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    // The request has failed for some reason!
    // Check the error var
    NSLog(@"Entering connection:didFailWithError");
    
    NSLog(@"Error: %@",error);
    
    NSLog(@"Entering connection:didFailWithError");
}

@end
