//
//  GoogleCloudUploader.h
//  AirImagePicker
//
//  Created by Daniel Rodriguez on 5/30/13.
//  Copyright (c) 2013 FreshPlanet. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "AirImagePicker.h"

@interface GoogleCloudUploader : NSObject<NSURLConnectionDataDelegate>
{
    NSMutableData *_responseData;
}

- (void) startUpload:(NSURL*)mediaURL withUploadURL:(NSURL*)uploadURL andUploadParams:(NSDictionary*)params;

@end
