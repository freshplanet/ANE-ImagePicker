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
    
    NSData *mediaData = [[NSFileManager defaultManager] contentsAtPath:[mediaURL path]];
    
    // Create a NSMutableURLRequest
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:uploadURL];
    [request setHTTPMethod:@"POST"];
    
    // Define the content type and set a boundary string
    // @check out the spec for multipart forms
    // http://www.w3.org/Protocols/rfc1341/7_2_Multipart.html
    NSString *boundary = @"b0undaryFP";
    NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@",boundary];
    [request addValue:contentType forHTTPHeaderField:@"Content-Type"];
    
    // Build the body of the HTML form.  Its important to notice the linebreaks CLRF "\r\n" and the dashes
    // before the boundary string; these must be included in order to build a good request.
    NSMutableData *body = [NSMutableData data];
    
    NSArray *variableNames = [params allKeys];
    for (int i = 0; i < [variableNames count]; i++) {
        NSString *variableName = [variableNames objectAtIndex:i];
        NSString *variableValue = [params valueForKey:variableName];
        [body appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
        [body appendData:[[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"\r\n\r\n%@", variableName, variableValue]
                          dataUsingEncoding:NSUTF8StringEncoding]];
    }
    [body appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
    [body appendData:[@"Content-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
    [body appendData:mediaData];
    // this is the final boundary:
    [body appendData:[[NSString stringWithFormat:@"\r\n--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];
    [request setHTTPBody:body];
    
    // make a connection to GCS and send the data
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
    
    NSString *response = [[[NSString alloc] initWithData:_responseData encoding:NSUTF8StringEncoding] autorelease];
    NSLog(@"Succeeded! response (NSString): %@", response);

    // Tell the native extension that we are done.
    [AirImagePicker status:@"FILE_UPLOAD_DONE" level:response];

    // Release the connection and the data object
    [connection release];
    [_responseData release];
    
    NSLog(@"Exiting connectionDidFinishLoading");
}

- (void)connection:(NSURLConnection *)connection didSendBodyData:(NSInteger)bytesWritten
                                                 totalBytesWritten:(NSInteger)totalBytesWritten
                                                 totalBytesExpectedToWrite:(NSInteger)totalBytesExpectedToWrite {
    NSLog(@"Entering connection:didSendBodyData:totalBytesWritten:totalBytesExpectedToWrite");
    
    float uploaded = (totalBytesWritten/totalBytesExpectedToWrite);
    NSString *uploadedStr = [NSString stringWithFormat:@"%f",uploaded];

    NSLog(@"bytesWritten = %d, totalBytesWritten = %d, totalBytesExpectedToWrite = %d,uploaded: %@", bytesWritten, totalBytesWritten, totalBytesExpectedToWrite, uploadedStr);
    
    [AirImagePicker status:@"FILE_UPLOAD_PROGRESS" level:uploadedStr];
    
    NSLog(@"Exiting connection:didSendBodyData:totalBytesWritten:totalBytesExpectedToWrite");
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    // The request has failed for some reason!
    // Check the error var
    NSLog(@"Entering connection:didFailWithError");
    
    NSLog(@"Error: %@",error);
    
    NSLog(@"Entering connection:didFailWithError");
}

@end
