//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//////////////////////////////////////////////////////////////////////////////////////

#import "AirImagePicker.h"
#include <MobileCoreServices/MobileCoreServices.h>
#import <AVFoundation/AVFoundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import "UIImage+Resize.h"

#define PRINT_LOG   YES
#define LOG_TAG     @"AirImagePicker"

//#define MAX_IMAGE_SIZE 500
#define MAX_VIDEO_DURATION 30.0

FREContext AirIPCtx = nil;

@interface AirImagePicker ()
{
    UIView *_overlay;
}
@property (nonatomic, strong) UIImage *pickedImage;
@property (nonatomic, strong) UIImagePickerController *imagePicker;
@property (nonatomic, strong) UIPopoverController *popover;
@property (nonatomic, strong) NSString *customImageAlbumName;
@end

@implementation AirImagePicker

@synthesize imagePicker = _imagePicker;
@synthesize popover = _popover;
//@synthesize pickedImage = _pickedImage;
//@synthesize pickedImageJPEGData = _pickedImageJPEGData;
@synthesize customImageAlbumName = _customImageAlbumName;
@synthesize videoPath = _videoPath;

static AirImagePicker *sharedInstance = nil;
static CGSize _maxDimensions;
static BOOL _crop;

+ (AirImagePicker *)sharedInstance
{
    if (sharedInstance == nil)
    {
        sharedInstance = [[super allocWithZone:NULL] init];
    }
    return sharedInstance;
}

+ (id)allocWithZone:(NSZone *)zone
{
    return [AirImagePicker sharedInstance];
}

- (id)copy
{
    return self;
}


+ (void)log:(NSString *)message
{
    if (PRINT_LOG) NSLog(@"[%@] %@", LOG_TAG, message);
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"LOGGING", (const uint8_t *)[message UTF8String]);
}

+ (void)status:(NSString*)code level:(NSString*)level
{
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)[code UTF8String], (const uint8_t *)[level UTF8String]);
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType allowVideo:(BOOL)allowVideo crop:(BOOL)crop albumName:(NSString*)albumName anchor:(CGRect)anchor maxDimensions:(CGSize)maxDimensions
{
    UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    _maxDimensions = maxDimensions;
    _crop =crop;
    
    self.imagePicker = [[UIImagePickerController alloc] init];
    self.imagePicker.sourceType = sourceType;
    self.imagePicker.allowsEditing = crop;
    self.imagePicker.delegate = self;
    if (allowVideo == true) {
        // there are memory leaks that are not occuring if we use CoreFoundation C code
        self.imagePicker.mediaTypes = [NSArray arrayWithObjects:(NSString *)kUTTypeMovie, nil];
    }
    
    self.customImageAlbumName = albumName;
    
    // Image picker should always be presented fullscreen on iPhone and iPod Touch.
    // It should be presented fullscreen on iPad only if it's the camera. Otherwise, we use a popover.
    if (sourceType == UIImagePickerControllerSourceTypeCamera || UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone)
    {
        [rootViewController presentModalViewController:self.imagePicker animated:YES];
    }
    else
    {
        self.popover = [[UIPopoverController alloc] initWithContentViewController:self.imagePicker];
        self.popover.delegate = self;
        [self.popover presentPopoverFromRect:anchor inView:rootViewController.view
                    permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}

- (void)displayOverlay:(UIImage *)overlay
{
    // Create a final overlay including a black area instead of the status bar, if needed.
    
    UIView *rootView = [[[[UIApplication sharedApplication] keyWindow] rootViewController] view];
    
    CGRect statusBarFrame = CGRectZero;
    CGRect overlayFrame = rootView.frame;
    CGRect finalOverlayFrame = rootView.frame;
    if (![UIApplication sharedApplication].statusBarHidden)
    {
        statusBarFrame = [[UIApplication sharedApplication] statusBarFrame];
        finalOverlayFrame.origin.y -= 40;
        finalOverlayFrame.size.height += 20;
    }
    
    // Setup context
    UIGraphicsBeginImageContext(finalOverlayFrame.size);
    CGContextRef context = UIGraphicsGetCurrentContext();
    UIGraphicsPushContext(context);
    
    // Fill status bar area with black
    UIColor *blackColor = [UIColor blackColor];
    CGContextSetFillColorWithColor(context, blackColor.CGColor);
    CGContextFillRect(context, statusBarFrame);
    
    // Draw actual overlay
    [overlay drawInRect:overlayFrame];
    
    // Produce final overlay and clean up context
    UIGraphicsPopContext();
    UIImage *finalOverlay = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    // Display overlay
    _overlay = [[UIImageView alloc] initWithImage:finalOverlay];
    _overlay.frame = finalOverlayFrame;
    [rootView addSubview:_overlay];
}

- (void)removeOverlay
{
    [_overlay removeFromSuperview];
//    [_overlay release];
    _overlay = nil;
}

#pragma mark - UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    NSLog(@"Entering imagePickerController:didFinishPickingMediaWithInfo");
    
    NSString *mediaType = [info objectForKey:UIImagePickerControllerMediaType];
    
    // Apple sez: When the user taps a button in the camera interface to accept a newly captured picture or movie, or to just cancel the operation, the system notifies the delegate of the user’s choice. The system does not, however, dismiss the camera interface. The delegate must dismiss it
    if (self.popover) {
        [self.popover dismissPopoverAnimated:YES];
        self.popover = nil;
    } else {
        [self.imagePicker dismissModalViewControllerAnimated:YES];
        self.imagePicker = nil;
    }
    
    // Handle a image
    if (CFStringCompare((CFStringRef) mediaType, kUTTypeImage, 0) == kCFCompareEqualTo)
    {
        
        [self onImagePickedWithOriginalImage:[info objectForKey:UIImagePickerControllerOriginalImage]
                                 editedImage:[info objectForKey:UIImagePickerControllerEditedImage]];
    }
    
    // Handle a movie
    if (CFStringCompare((CFStringRef) mediaType, kUTTypeMovie, 0) == kCFCompareEqualTo)
    {
        [self onVideoPickedWithMediaURL:[info objectForKey:UIImagePickerControllerMediaURL]];
    }
    
    NSLog(@"Exiting imagePickerController:didFinishPickingMediaWithInfo");
}


- (void) onImagePickedWithOriginalImage:(UIImage*)originalImage editedImage:(UIImage*)editedImage
{
    // Process image in background thread
    dispatch_queue_t thread = dispatch_queue_create("image processing", NULL);
    dispatch_async(thread, ^{
        
        
        // Retrieve image
        BOOL crop = _crop;
        if (editedImage)
        {
            self.pickedImage = editedImage;
        }
        else
        {
            crop = NO;
            self.pickedImage = originalImage;
        }
        
        if (!crop)
        {
            // Unedited images may have an incorrect orientation. We fix it.
            self.pickedImage = [self.pickedImage resizedImageWithContentMode:UIViewContentModeScaleAspectFit bounds:self.pickedImage.size interpolationQuality:kCGInterpolationDefault];
        }
        else if (self.pickedImage.size.width != self.pickedImage.size.height)
        {
            // If image is not square (happens if the user didn't zoom enough when cropping), we add black areas around
            CGFloat longestEdge = MAX(_pickedImage.size.width, _pickedImage.size.height);
            CGRect drawRect = CGRectZero;
            drawRect.size = self.pickedImage.size;
            if (self.pickedImage.size.width > self.pickedImage.size.height)
            {
                drawRect.origin.y = (longestEdge - self.pickedImage.size.height) / 2;
            }
            else
            {
                drawRect.origin.x = (longestEdge - self.pickedImage.size.width) / 2;
            }
            
            // Prepare drawing context
            CGImageRef imageRef = self.pickedImage.CGImage;
            CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
            CGContextRef context = CGBitmapContextCreate(NULL, longestEdge, longestEdge, 8, 0, colorSpace, kCGImageAlphaPremultipliedLast);
            CGColorSpaceRelease(colorSpace);
            
            // Draw new image
            UIColor *black = [UIColor blackColor];
            CGContextSetFillColorWithColor(context, black.CGColor);
            CGContextFillRect(context, CGRectMake(0, 0, longestEdge, longestEdge));
            CGContextDrawImage(context, drawRect, imageRef);
            CGImageRef newImageRef = CGBitmapContextCreateImage(context);
            self.pickedImage = [UIImage imageWithCGImage:newImageRef];
            
            // Clean up
            CGContextRelease(context);
            CGImageRelease(newImageRef);
        }
        
        
        // Save Image in Custom Album
        if(self.customImageAlbumName) {
            [self saveImageToCameraRoll:self.pickedImage inAlbum:_customImageAlbumName];
            self.customImageAlbumName = nil;
        }
        self.pickedImage = [AirImagePicker resizeImage:self.pickedImage toMaxDimension:_maxDimensions];
        
        self.imagePath = [[self saveImageToTemporaryDirectory:self.pickedImage] path];
        
        
        dispatch_async(dispatch_get_main_queue(), ^{
            FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"IMAGE");
        });
        
    });
}


+ (UIImage *) resizeImage:(UIImage *)image toMaxDimension:(CGSize)maxDimensions {
    
    // make sure that the image has the correct size
    if ( (image.size.width > maxDimensions.width || image.size.height > maxDimensions.height ) &&
        maxDimensions.width > 0 && maxDimensions.height > 0)
    {
        float reductionFactor = MAX(image.size.width / maxDimensions.width, image.size.height / maxDimensions.height);
        CGSize newSize = CGSizeMake(image.size.width/reductionFactor, image.size.height/reductionFactor);
        
        image = [image resizedImage:newSize interpolationQuality:kCGInterpolationMedium];
        NSLog(@"resized image to %f x %f", newSize.width, newSize.height);
    }
    return image;
}

- (void) saveImageToCameraRoll:(UIImage *)image inAlbum:(NSString *)albumName {
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    [library saveImage:image
               toAlbum:albumName
   withCompletionBlock:^(NSError* error, ALAsset *asset){
       NSLog(@"finished saving to album: %@ with error: %@ and asset: %@", albumName, error, asset);
   }];
}


- (NSURL *) saveImageToTemporaryDirectory:(UIImage *)image {
    
    // JPEG compression
    NSData *imageJPEGData = UIImageJPEGRepresentation(image, 0.95);
    
    // Save a copy of the picked video to the app directory
    NSURL *tmpFolderURL =[[NSURL alloc] initFileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
    NSURL *toURL = [tmpFolderURL URLByAppendingPathComponent:[NSString stringWithFormat:@"%duploadImage.jpg", arc4random()]];
    
    [imageJPEGData writeToURL:toURL atomically:YES];
    NSLog(@"Saved image %@ in %@", image, toURL);
    return toURL;
}

- (void) onVideoPickedWithMediaURL:(NSURL *)originalMediaURL {
    NSLog(@"Entering - onVideoPickedWithMediaURL originalMediaURL:%@", originalMediaURL);
    
    // save the video path for later use
    self.videoPath = [originalMediaURL path];
    
    // create a thumbnail
    
    // Create or Asset Generator and task it with creating the thumbnail
    AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:originalMediaURL options:nil];
    AVAssetImageGenerator *imageGenerator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
    CMTime time = CMTimeMakeWithSeconds(0,30);
    CGSize maxSize = CGSizeMake(320, 180);
    imageGenerator.maximumSize = maxSize;
    
    // Attemp to create the CGImage of the thumbnail
    [imageGenerator generateCGImagesAsynchronouslyForTimes:[NSArray arrayWithObject:[NSValue valueWithCMTime:time]]
                                         completionHandler:
     ^(CMTime requestedTime, CGImageRef image, CMTime actualTime,
       AVAssetImageGeneratorResult result, NSError *error) {
         // Something went wrong, log the error.
         if (result != AVAssetImageGeneratorSucceeded) {
             NSLog(@"Couldn't generate thumbnail, error: %@", error);
             dispatch_async(dispatch_get_main_queue(), ^{
                 FREDispatchStatusEventAsync(AirIPCtx,
                                             (const uint8_t *)"ERROR_GENERATING_VIDEO",
                                             (const uint8_t *)[[error description] UTF8String]);
             });
         } else {
             // Success!  Store the data we will retrieve later
             self.pickedImage = [[UIImage alloc] initWithCGImage:image];
             
             self.pickedImage = [AirImagePicker resizeImage:self.pickedImage toMaxDimension:_maxDimensions];
             self.imagePath = [[self saveImageToTemporaryDirectory:self.pickedImage] path];
             
             dispatch_async(dispatch_get_main_queue(), ^{
                 NSLog(@"self.imageView.image = self.pickedImage");
                 FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"VIDEO");
             });
         }
         
     }];
    
    NSLog(@"Exiting - onVideoPickedWithMediaURL");
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    if (self.popover)
    {
        [self.popover dismissPopoverAnimated:YES];
        self.popover = nil;
    }
    else
    {
        [self.imagePicker dismissModalViewControllerAnimated:YES];
        self.imagePicker = nil;
    }
    
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_CANCEL", (const uint8_t *)"OK");
}

#pragma mark - UIPopoverControllerDelegate

- (void)popoverControllerDidDismissPopover:(UIPopoverController *)popoverController
{
    if (self.popover)
    {
        [self.popover dismissPopoverAnimated:YES];
        self.popover = nil;
    }
    else
    {
        [self.imagePicker dismissModalViewControllerAnimated:YES];
        self.imagePicker = nil;
    }
    
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_CANCEL", (const uint8_t *)"OK");
}




@end

// C API

DEFINE_ANE_FUNCTION(isImagePickerAvailable)
{
    BOOL isImagePickerAvailable = [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary];
    
    FREObject result;
    if (FRENewObjectFromBool(isImagePickerAvailable, &result) == FRE_OK)
    {
        return result;
    }
    else return nil;
}

DEFINE_ANE_FUNCTION(displayImagePicker)
{
    FREObject imageMaxWidthObj = argv[0];
    uint32_t imageMaxWidth;
    FREGetObjectAsUint32(imageMaxWidthObj, &imageMaxWidth);
    
    FREObject imageMaxHeightObj = argv[1];
    uint32_t imageMaxHeight;
    FREGetObjectAsUint32(imageMaxHeightObj, &imageMaxHeight);
    
    uint32_t allowVideoValue;
    FREObject allowVideoObj = argv[2];
    FREGetObjectAsBool(allowVideoObj, &allowVideoValue);
    BOOL allowVideo = (allowVideoValue != 0);
    
    uint32_t cropValue;
    FREObject cropObject = argv[3];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
    
    CGRect anchor;
    if (argc > 4)
    {
        // Extract anchor properties
        FREObject anchorObject = argv[4];
        FREObject anchorX, anchorY, anchorWidth, anchorHeight, thrownException;
        FREGetObjectProperty(anchorObject, (const uint8_t *)"x", &anchorX, &thrownException);
        FREGetObjectProperty(anchorObject, (const uint8_t *)"y", &anchorY, &thrownException);
        FREGetObjectProperty(anchorObject, (const uint8_t *)"width", &anchorWidth, &thrownException);
        FREGetObjectProperty(anchorObject, (const uint8_t *)"height", &anchorHeight, &thrownException);
        
        // Convert anchor properties to double
        double x, y, width, height;
        FREGetObjectAsDouble(anchorX, &x);
        FREGetObjectAsDouble(anchorY, &y);
        FREGetObjectAsDouble(anchorWidth, &width);
        FREGetObjectAsDouble(anchorHeight, &height);
        
        // Divide properties by the scale (useful for Retina Display)
        CGFloat scale = [[UIScreen mainScreen] scale];
        anchor = CGRectMake(x/scale, y/scale, width/scale, height/scale);
    }
    else
    {
        UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
        anchor = CGRectMake(rootViewController.view.bounds.size.width - 100, 0, 100, 1); // Default anchor: Top right corner
    }
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary allowVideo:allowVideo crop:crop albumName:nil anchor:anchor maxDimensions:CGSizeMake(imageMaxWidth, imageMaxHeight)];
    
    return nil;
}

DEFINE_ANE_FUNCTION(isCameraAvailable)
{
    BOOL isCameraAvailable = [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera];
    
    FREObject result;
    if (FRENewObjectFromBool(isCameraAvailable, &result) == FRE_OK)
    {
        return result;
    }
    else return nil;
}

DEFINE_ANE_FUNCTION(displayCamera)
{
    FREObject imageMaxWidthObj = argv[0];
    uint32_t imageMaxWidth;
    FREGetObjectAsUint32(imageMaxWidthObj, &imageMaxWidth);
    
    FREObject imageMaxHeightObj = argv[1];
    uint32_t imageMaxHeight;
    FREGetObjectAsUint32(imageMaxHeightObj, &imageMaxHeight);
    
    uint32_t allowVideoValue;
    FREObject allowVideoObj = argv[2];
    FREGetObjectAsBool(allowVideoObj, &allowVideoValue);
    BOOL allowVideo = (allowVideoValue != 0);
    
    uint32_t cropValue;
    FREObject cropObject = argv[3];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
 
    uint32_t stringLength;
    NSString *albumName = nil;
    const uint8_t *albumNameString;
    if (FREGetObjectAsUTF8(argv[4], &stringLength, &albumNameString) == FRE_OK)
    {
        albumName = [NSString stringWithUTF8String:(const char *)albumNameString];
    }
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera allowVideo:allowVideo crop:crop albumName:albumName anchor:CGRectZero maxDimensions:CGSizeMake(imageMaxWidth, imageMaxHeight)];
    
    return nil;
}

DEFINE_ANE_FUNCTION(getPickedImageWidth)
{
    NSLog(@"Entering getPickedImageWidth");
    
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        CGImageRef imageRef = [pickedImage CGImage];
        NSUInteger width = CGImageGetWidth(imageRef);
        
        FREObject result;
        if (FRENewObjectFromUint32(width, &result) == FRE_OK)
        {
            NSLog(@"Exiting getPickedImageWidth");
            return result;
        }
        else
        {
            NSLog(@"Exiting getPickedImageWidth");
            return nil;
        }
    }
    else
    {
      NSLog(@"Exiting getPickedImageWidth");
      return nil;
    }
}

DEFINE_ANE_FUNCTION(getPickedImageHeight)
{
    NSLog(@"Entering getPickedImageHeight");
    
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        CGImageRef imageRef = [pickedImage CGImage];
        NSUInteger height = CGImageGetHeight(imageRef);
        
        FREObject result;
        if (FRENewObjectFromUint32(height, &result) == FRE_OK)
        {
            NSLog(@"Exiting getPickedImageHeight");
            return result;
        }
        else
        {
            NSLog(@"Exiting getPickedImageHeight");
            return nil;
        }
    }
    else
    {
        NSLog(@"Exiting getPickedImageHeight");
        return nil;
    }
}

DEFINE_ANE_FUNCTION(drawPickedImageToBitmapData)
{
    NSLog(@"Entering drawPickedImageToBitmapData");
    
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        // Get the AS3 BitmapData
        FREBitmapData bitmapData;
        FREAcquireBitmapData(argv[0], &bitmapData);
        
        // Pull the raw pixels values out of the image data
        CGImageRef imageRef = [pickedImage CGImage];
        NSUInteger width = CGImageGetWidth(imageRef);
        NSUInteger height = CGImageGetHeight(imageRef);
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        unsigned char *rawData = malloc(height * width * 4);
        NSUInteger bytesPerPixel = 4;
        NSUInteger bytesPerRow = bytesPerPixel * width;
        NSUInteger bitsPerComponent = 8;
        CGContextRef context = CGBitmapContextCreate(rawData, width, height, bitsPerComponent, bytesPerRow, colorSpace, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
        CGColorSpaceRelease(colorSpace);
        CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
        CGContextRelease(context);
        
        // Pixels are now it rawData in the format RGBA8888
        // Now loop over each pixel to write them into the AS3 BitmapData memory
        int x, y;
        // There may be extra pixels in each row due to the value of lineStride32.
        // We'll skip over those as needed.
        int offset = bitmapData.lineStride32 - bitmapData.width;
        int offset2 = bytesPerRow - bitmapData.width*4;
        int byteIndex = 0;
        uint32_t *bitmapDataPixels = bitmapData.bits32;
        for (y=0; y<bitmapData.height; y++)
        {
            for (x=0; x<bitmapData.width; x++, bitmapDataPixels++, byteIndex += 4)
            {
                // Values are currently in RGBA7777, so each color value is currently a separate number.
                int red     = (rawData[byteIndex]);
                int green   = (rawData[byteIndex + 1]);
                int blue    = (rawData[byteIndex + 2]);
                int alpha   = (rawData[byteIndex + 3]);
                
                // Combine values into ARGB32
                *bitmapDataPixels = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
            
            bitmapDataPixels += offset;
            byteIndex += offset2;
        }
        
        // Free the memory we allocated
        free(rawData);
        
        // Tell Flash which region of the BitmapData changes (all of it here)
        FREInvalidateBitmapDataRect(argv[0], 0, 0, bitmapData.width, bitmapData.height);
        
        // Release our control over the BitmapData
        FREReleaseBitmapData(argv[0]);
    }
    
    NSLog(@"Exiting drawPickedImageToBitmapData");
    return nil;
}

//DEFINE_ANE_FUNCTION(getPickedImageJPEGRepresentationSize)
//{
//    NSData *jpegData = [[AirImagePicker sharedInstance] pickedImageJPEGData];
//    
//    if (jpegData)
//    {
//        FREObject result;
//        if (FRENewObjectFromUint32(jpegData.length, &result) == FRE_OK)
//        {
//            return result;
//        }
//        else return nil;
//    }
//    else return nil;
//}
//
//DEFINE_ANE_FUNCTION(copyPickedImageJPEGRepresentationToByteArray)
//{
//    NSData *jpegData = [[AirImagePicker sharedInstance] pickedImageJPEGData];
//    
//    if (jpegData)
//    {
//        // Get the AS3 ByteArray
//        FREByteArray byteArray;
//        FREAcquireByteArray(argv[0], &byteArray);
//        
//        // Copy JPEG representation in ByteArray
//        memcpy(byteArray.bytes, jpegData.bytes, jpegData.length);
//        
//        // Release our control over the ByteArray
//        FREReleaseByteArray(argv[0]);
//    }
//    
//    return nil;
//}

DEFINE_ANE_FUNCTION(displayOverlay)
{
    // Get the AS3 BitmapData
    FREBitmapData bitmapData;
    FREAcquireBitmapData(argv[0], &bitmapData);
    
    // Make data provider from buffer
    CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, bitmapData.bits32, (bitmapData.width * bitmapData.height * 4), NULL);
    
    // Setup for CGImage creation
    int                     bitsPerComponent = 8;
    int                     bitsPerPixel     = 32;
    int                     bytesPerRow      = 4 * bitmapData.width;
    CGColorSpaceRef         colorSpaceRef    = CGColorSpaceCreateDeviceRGB();
    CGBitmapInfo            bitmapInfo = kCGBitmapByteOrder32Little | (bitmapData.hasAlpha ? (bitmapData.isPremultiplied ? kCGImageAlphaPremultipliedFirst : kCGImageAlphaFirst) : kCGImageAlphaNoneSkipFirst);
    CGColorRenderingIntent  renderingIntent = kCGRenderingIntentDefault;
    
    // Create CGImage
    CGImageRef imageRef = CGImageCreate(bitmapData.width, bitmapData.height, bitsPerComponent, bitsPerPixel, bytesPerRow, colorSpaceRef, bitmapInfo, provider, NULL, NO, renderingIntent);
    
    // Create overlay from CGImage
    UIImage *overlay = [UIImage imageWithCGImage:imageRef];
    [[AirImagePicker sharedInstance] displayOverlay:overlay];
    
    // Clean up
    FREReleaseBitmapData(argv[0]);
    CFRelease(imageRef);
    CFRelease(provider);
    CFRelease(colorSpaceRef);
    
    return nil;
}

DEFINE_ANE_FUNCTION(removeOverlay)
{
    [[AirImagePicker sharedInstance] removeOverlay];
    
    return nil;
}

DEFINE_ANE_FUNCTION(getVideoPath)
{
    NSLog(@"Entering getVideoPath");
    FREObject retValue = NULL;
    
    NSString *videoPath = [[AirImagePicker sharedInstance] videoPath];
    FRENewObjectFromUTF8(strlen([videoPath UTF8String])+1,
                         (const uint8_t *)[videoPath UTF8String],
                         &retValue);
    
    NSLog(@"videoPath %@", videoPath);
    
    
    NSLog(@"Exiting getVideoPath");
    return retValue;
}

DEFINE_ANE_FUNCTION(getImagePath)
{
    NSLog(@"Entering getImagePath");
    FREObject retValue = NULL;
    
    NSString *imagePath = [[AirImagePicker sharedInstance] imagePath];
    NSLog(@"imagePath %@", imagePath);
    FRENewObjectFromUTF8(strlen([imagePath UTF8String])+1,
                         (const uint8_t *)[imagePath UTF8String],
                         &retValue);
    
    NSLog(@"Exiting getImagePath");
    return retValue;
}


// ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet)
{
    // Register the links btwn AS3 and ObjC. (dont forget to modify the nbFuntionsToLink integer if you are adding/removing functions)
    NSInteger nbFuntionsToLink = 11;
    *numFunctionsToTest = nbFuntionsToLink;
    
    FRENamedFunction* func = (FRENamedFunction*) malloc(sizeof(FRENamedFunction) * nbFuntionsToLink);
    
    func[0].name = (const uint8_t*) "isImagePickerAvailable";
    func[0].functionData = NULL;
    func[0].function = &isImagePickerAvailable;
    
    func[1].name = (const uint8_t*) "displayImagePicker";
    func[1].functionData = NULL;
    func[1].function = &displayImagePicker;
    
    func[2].name = (const uint8_t*) "isCameraAvailable";
    func[2].functionData = NULL;
    func[2].function = &isCameraAvailable;
    
    func[3].name = (const uint8_t*) "displayCamera";
    func[3].functionData = NULL;
    func[3].function = &displayCamera;
    
    func[4].name = (const uint8_t*) "getPickedImageWidth";
    func[4].functionData = NULL;
    func[4].function = &getPickedImageWidth;
    
    func[5].name = (const uint8_t*) "getPickedImageHeight";
    func[5].functionData = NULL;
    func[5].function = &getPickedImageHeight;
    
    func[6].name = (const uint8_t*) "drawPickedImageToBitmapData";
    func[6].functionData = NULL;
    func[6].function = &drawPickedImageToBitmapData;
    
//    func[7].name = (const uint8_t*) "getPickedImageJPEGRepresentationSize";
//    func[7].functionData = NULL;
//    func[7].function = &getPickedImageJPEGRepresentationSize;
//    
//    func[8].name = (const uint8_t*) "copyPickedImageJPEGRepresentationToByteArray";
//    func[8].functionData = NULL;
//    func[8].function = &copyPickedImageJPEGRepresentationToByteArray;
    
    func[7].name = (const uint8_t*) "displayOverlay";
    func[7].functionData = NULL;
    func[7].function = &displayOverlay;
    
    func[8].name = (const uint8_t*) "removeOverlay";
    func[8].functionData = NULL;
    func[8].function = &removeOverlay;
    
    func[9].name = (const uint8_t*) "getVideoPath";
    func[9].functionData = NULL;
    func[9].function = &getVideoPath;
    
    func[10].name = (const uint8_t*) "getImagePath";
    func[10].functionData = NULL;
    func[10].function = &getImagePath;
    
    *functionsToSet = func;
    
    AirIPCtx = ctx;
}

void AirImagePickerContextFinalizer(FREContext ctx) { }

void AirImagePickerInitializer(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet)
{
    *extDataToSet = NULL;
    *ctxInitializerToSet = &AirImagePickerContextInitializer;
    *ctxFinalizerToSet = &AirImagePickerContextFinalizer;
}

void AirImagePickerFinalizer(void *extData) { }