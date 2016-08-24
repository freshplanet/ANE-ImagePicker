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

#import <UIKit/UIApplication.h>
#import "AirImagePicker.h"
#include <MobileCoreServices/MobileCoreServices.h>
#import <AVFoundation/AVFoundation.h>
#import <MediaPlayer/MediaPlayer.h>
#import "UIImage+Resize.h"


#define PRINT_LOG   YES
#define LOG_TAG     @"AirImagePicker"

//#define MAX_IMAGE_SIZE 500
//#define MAX_VIDEO_DURATION 30.0

FREContext AirIPCtx = nil;

@interface AirImagePicker ()
{
    UIView *_overlay;
@private
    NSMutableDictionary *_fetchedImages;
    
}


@property (nonatomic, strong) UIImage *pickedImage;
@property (nonatomic, strong) UIImagePickerController *imagePicker;
@property (nonatomic, strong) UIPopoverController *popover;
@property (nonatomic, strong) NSString *customImageAlbumName;
@property (nonatomic) UIImagePickerControllerCameraFlashMode *myFlashMode;

@end

@implementation AirImagePicker

@synthesize imagePicker = _imagePicker;
@synthesize popover = _popover;
//@synthesize pickedImage = _pickedImage;
//@synthesize pickedImageJPEGData = _pickedImageJPEGData;
@synthesize customImageAlbumName = _customImageAlbumName;
@synthesize videoPath = _videoPath;

static NSString * ANE_ERROR = @"ANE_ERROR";
static NSString * IMAGE_LOAD_ERROR = @"IMAGE_LOAD_ERROR";
static NSString * IMAGE_LOAD_TEMP = @"IMAGE_LOAD_TEMP";
static NSString * IMAGE_LOAD_CANCELLED = @"IMAGE_LOAD_CANCELLED";
static NSString * IMAGE_LOAD_SUCCEEDED = @"IMAGE_LOAD_SUCCEEDED";

static AirImagePicker *sharedInstance = nil;
static CGSize _maxDimensions;
static BOOL _crop;

- (id) init
{
    self = [super init];
    if(self) {
        _fetchedImages = [[NSMutableDictionary alloc] init];
    }
    return self;
}

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

- (void)storeUIImage:(PHImageRequestID)requestId image:(UIImage *)fetchedImage
{
    @synchronized (self) {
        [_fetchedImages setObject:fetchedImage forKey:[NSNumber numberWithInt:requestId]];
    }
}

- (UIImage*)retrieveUIImage:(PHImageRequestID)requestId
{
    UIImage * image = nil;
    @synchronized (self) {
        image = [_fetchedImages objectForKey:[NSNumber numberWithInt:requestId]];
        if(image != nil) {
            [_fetchedImages removeObjectForKey:[NSNumber numberWithInt:requestId]];
        }
    }
    return image;
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType allowVideo:(BOOL)allowVideo crop:(BOOL)crop albumName:(NSString*)albumName anchor:(CGRect)anchor maxDimensions:(CGSize)maxDimensions
{
    UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    NSLog(@"displayImagePickerWithSourceType allowVideo:%d crop:%d albumName:%@ anchor:%f %f %f %f maxDimensions:%f %f", allowVideo, crop, albumName, anchor.origin.x, anchor.origin.y, anchor.size.width, anchor.size.height, maxDimensions.width, maxDimensions.height);
    _maxDimensions = maxDimensions;
    _crop =crop;
    
    self.imagePicker = [[UIImagePickerController alloc] init];
    self.imagePicker.sourceType = sourceType;
    self.imagePicker.allowsEditing = crop;
    self.imagePicker.delegate = self;
    self.imagePicker.videoQuality = UIImagePickerControllerQualityTypeMedium;//recompression will be done later
    if (allowVideo == true) {
        // there are memory leaks that are not occuring if we use CoreFoundation C code
        self.imagePicker.mediaTypes = [NSArray arrayWithObjects:(NSString *)kUTTypeMovie, nil];
    }
    
    if(sourceType == UIImagePickerControllerSourceTypeCamera) {
        self.imagePicker.cameraFlashMode = [self myFlashMode];
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
    
    if(self.imagePicker.sourceType == UIImagePickerControllerSourceTypeCamera) {
        [self setMyFlashMode:[[self imagePicker] cameraFlashMode]];
    }
    
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
        
        // Save Image in Custom Album ?
        if(self.customImageAlbumName) {
            if ([ALAssetsLibrary authorizationStatus] == ALAuthorizationStatusAuthorized) {
                [self saveImageToCameraRoll:self.pickedImage inAlbum:_customImageAlbumName withCompletionBlock:^(NSError *error, ALAsset *asset) {
                }];
                [self finishImagePicked];
            } else {
                [self saveImageToCameraRoll:self.pickedImage inAlbum:_customImageAlbumName withCompletionBlock:^(NSError *error, ALAsset *asset) {
                    if (error == nil) {
                        [self finishImagePicked];
                    }
                }];
            }
            
            self.customImageAlbumName = nil;
        } else {
            [self finishImagePicked];
        }
    });
}

- (void) finishImagePicked {
    self.pickedImage = [AirImagePicker resizeImage:self.pickedImage toMaxDimension:_maxDimensions];
    
    self.imagePath = [[self saveImageToTemporaryDirectory:self.pickedImage] path];

    dispatch_async(dispatch_get_main_queue(), ^{
        if ( self.imagePath ) {
            FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"IMAGE");
        } else {
            FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"PICKING_ERROR", (const uint8_t *)"COULD_NOT_SAVE");
        }
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

- (void) saveImageToCameraRoll:(UIImage *)image inAlbum:(NSString *)albumName  withCompletionBlock:(SaveImageCompletion)completionBlock {
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    [library saveImage:image
               toAlbum:albumName
   withCompletionBlock:^(NSError* error, ALAsset *asset){
       if (error != nil) {
           NSLog(@"couldn't save to album: %@ with error: %@ and asset: %@", albumName, error, asset);
       } else {
           completionBlock(error, asset);
       }
   }];
}

- (void) saveVideoToCameraRoll:(NSURL *)videoUrl inAlbum:(NSString *)albumName {
    NSLog(@"Entering - saveVideoToCameraRoll");
    
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    
    if(! [library videoAtPathIsCompatibleWithSavedPhotosAlbum:videoUrl]) {
        NSLog(@"Video can't be saved to gallery");
        return;
    }
    [library saveVideo:videoUrl toAlbum:albumName
        withCompletionBlock:^(NSError* error, ALAsset *asset){
            NSLog(@"finished saving to album: %@ with error: %@ and asset: %@", albumName, error, asset);
}];
}

- (NSURL *) saveImageToTemporaryDirectory:(UIImage *)image {
    NSLog(@"Entering - saveImageToTemporaryDirectory");
    
    // JPEG compression
    NSData *imageJPEGData = UIImageJPEGRepresentation(image, 0.8);
    
    NSURL *tempDir = [AirImagePicker getTemporaryDirectory];
    NSError *error = nil;
    BOOL isDirectory = YES;
    BOOL tempDirExist = [[NSFileManager defaultManager] fileExistsAtPath:[tempDir path] isDirectory:&isDirectory];
    if (!tempDirExist || !isDirectory) {
        if(tempDirExist && !isDirectory) {
            NSLog(@"Removing file %@", tempDir);
            if(![[NSFileManager defaultManager] removeItemAtURL:tempDir error:&error]) {
                NSLog(@"Could not remove existing file %@, error: %@", tempDir, error);
                NSLog(@"Exiting - saveImageToTemporaryDirectory");
                return nil;
            }
        }
        NSLog(@"Creating directory %@", tempDir);
        if(![[NSFileManager defaultManager] createDirectoryAtPath:[tempDir path] withIntermediateDirectories:YES attributes:nil error:&error])
        {
            NSLog(@"Could not create directory %@, error: %@", tempDir, error);
            NSLog(@"Exiting - saveImageToTemporaryDirectory");
            return nil;
        }
    }
    
    // Save a copy of the picked video to the app tmp directory
    NSURL *toURL = [[AirImagePicker getTemporaryDirectory] URLByAppendingPathComponent:[NSString stringWithFormat:@"%duploadImage.jpg", arc4random()] isDirectory:NO];
    if([imageJPEGData writeToURL:toURL options:NSAtomicWrite error:&error]) {
        NSLog(@"Saved image %@ in %@", image, toURL);
    } else {
        NSLog(@"Could not save image %@ in %@, error: %@", image, toURL, error);
        NSLog(@"Exiting - saveImageToTemporaryDirectory");
        return nil;
    }
    NSLog(@"Exiting - saveImageToTemporaryDirectory");
    return toURL;
}

- (BOOL) cleanUpTemporaryDirectoryContent {
    NSLog(@"Entering - cleanUpTemporaryDirectoryContent");
    
    NSURL *directory = [AirImagePicker getTemporaryDirectory];
    NSError *error = nil;
    NSFileManager *fm = [NSFileManager defaultManager];
    BOOL allSuccess = YES;
    
    for (NSString *file in [fm contentsOfDirectoryAtPath:[directory path] error:&error]) {
        BOOL success = [fm removeItemAtPath:[NSString stringWithFormat:@"%@%@", directory, file] error:&error];
        if (!success || error) {
            allSuccess = NO;
            NSLog(@"cleanUpTemporaryDirectoryContent failed to remove %@", file);
        }
    }
    NSLog(@"Exiting - cleanUpTemporaryDirectoryContent");
    return allSuccess;
}

+ (NSURL *) getTemporaryDirectory {
    
    return [[[NSURL alloc] initFileURLWithPath:NSTemporaryDirectory() isDirectory:YES] URLByAppendingPathComponent:@"imagePicker" isDirectory:YES];
}

- (void) onVideoPickedWithMediaURL:(NSURL *)originalMediaURL {
    NSLog(@"Entering - onVideoPickedWithMediaURL originalMediaURL:%@", originalMediaURL);
    
    // save the video path for later use
    self.videoPath = [originalMediaURL path];
    
    
    // Save Image in Custom Album
    if(self.customImageAlbumName) {
        [self saveVideoToCameraRoll:originalMediaURL inAlbum:self.customImageAlbumName];
        self.customImageAlbumName = nil;
    }
    
    // create a thumbnail
    
    // Create or Asset Generator and task it with creating the thumbnail
    AVURLAsset *asset = [[AVURLAsset alloc] initWithURL:originalMediaURL options:nil];
    AVAssetImageGenerator *imageGenerator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
    CMTime time = CMTimeMakeWithSeconds(0,30);
    CGSize maxSize = CGSizeMake(320, 320);
    imageGenerator.maximumSize = maxSize;
    imageGenerator.appliesPreferredTrackTransform = true;
    
    
    
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
                 if ( self.imagePath ) {
                     FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"VIDEO");
                 } else {
                     FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"PICKING_ERROR", (const uint8_t *)"COULD_NOT_SAVE");
                 }
             });
         }
         
     }];
    
    NSLog(@"Exiting - onVideoPickedWithMediaURL");
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    if(picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
        [self setMyFlashMode:[[self imagePicker] cameraFlashMode]];
    }
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
    int32_t imageMaxWidth;
    FREGetObjectAsInt32(imageMaxWidthObj, &imageMaxWidth);
    
    FREObject imageMaxHeightObj = argv[1];
    int32_t imageMaxHeight;
    FREGetObjectAsInt32(imageMaxHeightObj, &imageMaxHeight);
    NSLog(@"displayImagePicker imageMaxWidth:%d imageMaxHeight:%d", imageMaxWidth, imageMaxHeight);
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
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary allowVideo:allowVideo crop:crop albumName:nil anchor:anchor maxDimensions:CGSizeMake((float)imageMaxWidth, (float)imageMaxHeight)];
    
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
    int32_t imageMaxWidth;
    FREGetObjectAsInt32(imageMaxWidthObj, &imageMaxWidth);
    
    FREObject imageMaxHeightObj = argv[1];
    int32_t imageMaxHeight;
    FREGetObjectAsInt32(imageMaxHeightObj, &imageMaxHeight);
    
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
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera allowVideo:allowVideo crop:crop albumName:albumName anchor:CGRectZero maxDimensions:CGSizeMake((float)imageMaxWidth, (float)imageMaxHeight)];
    
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

//Don't call this from a block
FREObject createBitmapData(uint32_t width, uint32_t height)
{
    FREObject widthObj;
    FRENewObjectFromInt32(width, &widthObj);
    FREObject heightObj;
    FRENewObjectFromInt32(height, &heightObj);
    FREObject transparent;
    FRENewObjectFromBool( 0, &transparent);
    FREObject fillColor;
    FRENewObjectFromUint32( 0x000000, &fillColor);
    
    FREObject params[4] = { widthObj, heightObj, transparent, fillColor };
    
    FREObject freBitmap;
    FRENewObject((uint8_t *)"flash.display.BitmapData", 4, params, &freBitmap , NULL);
    
    return freBitmap;
}

//Don't call this from a block
FREResult fillBitmapData(FREContext ctx, FREObject obj, UIImage *image)
{
    if(obj == NULL) {
        FREObject widthObj;
        FRENewObjectFromInt32(image.size.width, &widthObj);
        FREObject heightObj;
        FRENewObjectFromInt32(image.size.height, &heightObj);
        FREObject transparent;
        FRENewObjectFromBool( 0, &transparent);
        FREObject fillColor;
        FRENewObjectFromUint32( 0x000000, &fillColor);
        
        FREObject params[4] = { widthObj, heightObj, transparent, fillColor };
        
        FREObject freBitmap;
        FRENewObject((uint8_t *)"flash.display.BitmapData", 4, params, &freBitmap , NULL);
        obj = freBitmap;
    }
    
    [AirImagePicker log:@"Entering fillBitmapData"];
    FREResult result;
    FREBitmapData bitmapData;
    result = FREAcquireBitmapData(obj, &bitmapData);
    if (result != FRE_OK) {
        switch (result) {
            case FRE_ILLEGAL_STATE:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData FRE_ILLEGAL_STATE"];
                break;
            case FRE_INVALID_ARGUMENT:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData FRE_INVALID_ARGUMENT"];
                break;
            case FRE_INVALID_OBJECT:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData FRE_INVALID_OBJECT"];
                break;
            case FRE_TYPE_MISMATCH:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData FRE_TYPE_MISMATCH"];
                break;
            case FRE_WRONG_THREAD:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData FRE_WRONG_THREAD"];
                break;
            default:
                [AirImagePicker log:@"Couldn't acquire in fillBitmapData for some other reason"];
                break;
        }
        [AirImagePicker log:@"Couldn't acquire in fillBitmapData"];
        return result;
    }
    
    // Pull the raw pixels values out of the image data
    CGImageRef imageRef = [image CGImage];
    size_t width = CGImageGetWidth(imageRef);
    size_t height = CGImageGetHeight(imageRef);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    unsigned char *rawData = malloc(height * width * 4);
    size_t bytesPerPixel = 4;
    size_t bytesPerRow = bytesPerPixel * width;
    size_t bitsPerComponent = 8;
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
    size_t offset2 = bytesPerRow - bitmapData.width*4;
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
    result = FREInvalidateBitmapDataRect(obj, 0, 0, bitmapData.width, bitmapData.height);
    // Release our control over the BitmapData
    if(result == FRE_OK) {
       result = FREReleaseBitmapData(obj);
        if(result != FRE_OK) {
            [AirImagePicker log:@"Couldn't release in fillBitmapData"];
        }
    } else {
        [AirImagePicker log:@"Couldn't invalidate in fillBitmapData"];
    }
    
    
    
    return result;
}

DEFINE_ANE_FUNCTION(drawPickedImageToBitmapData)
{
    NSLog(@"Entering drawPickedImageToBitmapData");
    
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        fillBitmapData(context, argv[0], pickedImage);
    }
    
    NSLog(@"Exiting drawPickedImageToBitmapData");
    return nil;
}

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

DEFINE_ANE_FUNCTION(cleanUpTemporaryDirectoryContent)
{
    NSLog(@"EnteringcleanUpTemporaryDirectoryContent");
    FREObject retValue = NULL;
    
    BOOL success = [[AirImagePicker sharedInstance] cleanUpTemporaryDirectoryContent];
    if(success)
        NSLog(@"cleanUpTemporaryDirectoryContent success");
    else
        NSLog(@"cleanUpTemporaryDirectoryContent failed");
    
    NSLog(@"Exiting cleanUpTemporaryDirectoryContent");
    FRENewObjectFromBool(success, &retValue);
    return retValue;
}

DEFINE_ANE_FUNCTION(isCropAvailable)
{
    NSLog(@"Entering isCropAvailable");
    FREObject retValue = NULL;
    
    BOOL success = true;
    NSLog(@"Exiting isCropAvailable");
    FRENewObjectFromBool(success, &retValue);
    return retValue;
}

//don't call this from a block
NSArray<NSString *> * as3ArrayToNSStringArray (FREObject obj)
{
    NSLog(@"Entering as3ArrayToNSStringArray");
    uint32_t arrayLength;
    if(FREGetArrayLength(obj, &arrayLength) != FRE_OK) {
        [AirImagePicker log:@"Error getting array length in as3ArrayToNSStringArray"];
        return nil;
    }
    NSMutableArray<NSString *> * myArray = [[NSMutableArray alloc] initWithCapacity:arrayLength];
    for (uint32_t i = 0; i < arrayLength; ++i) {
        FREObject stringObj;
        const uint8_t * cstring;
        uint32_t strLength;
        if(FREGetArrayElementAt(obj, i, &stringObj) == FRE_OK) {
            if (FREGetObjectAsUTF8(stringObj, &strLength, &cstring) == FRE_OK) {
                NSString *objCString = [NSString stringWithUTF8String:(const char *)cstring];
                [myArray addObject:objCString];
            } else {
                [AirImagePicker log:@"Error getting UTF8 string in as3ArrayToNSStringArray"];
            }
        } else {
            [AirImagePicker log:@"Error getting array element in as3ArrayToNSStringArray"];
        }
    }
    return myArray;
}

// Return an array of localIdentifiers for the most recent photo assets
DEFINE_ANE_FUNCTION(getRecentImageIds)
{
    NSLog(@"Entering getRecentImageIds");
    
    int32_t fetchLimit;
    if(FREGetObjectAsInt32(argv[0], &fetchLimit) != FRE_OK) {
        return nil;
    };
    
    PHFetchOptions *opts = [[PHFetchOptions alloc] init];
    opts.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:NO]];
    opts.includeHiddenAssets = false;
    opts.includeAllBurstAssets = false;
    opts.includeAssetSourceTypes = PHAssetSourceTypeUserLibrary;
    opts.fetchLimit = fetchLimit; // TODO make this variable
    
    PHFetchResult *rslt = [PHAsset fetchAssetsWithMediaType:PHAssetMediaTypeImage options:opts];
    
    FREObject idsToReturn;
    FRENewObject((const uint8_t*)"Array", 0, NULL, &idsToReturn, nil);
    FRESetArrayLength(idsToReturn, (uint32_t)rslt.count);
    
    for (uint32_t i=0; i < rslt.count; ++i) {
        PHAsset * asset = [rslt objectAtIndex:i];
        NSString * assetId = asset.localIdentifier;
        const uint8_t * cAssetId = (const uint8_t *)[assetId UTF8String];
        FREObject idForAS3;
        FRENewObjectFromUTF8((uint32_t)strlen((const char *)cAssetId) + 1, cAssetId, &idForAS3);
        FRESetArrayElementAt(idsToReturn, i, idForAS3);
    }
    
    return idsToReturn;
}

// Takes an array of localIdentifier strings from AS3, and a width and height for the thumbnail
// Returns an array of request IDs to use to track download progress and completion
DEFINE_ANE_FUNCTION(fetchImages)
{
    NSLog(@"Entering fetchImages");
    NSArray<NSString *> *assetIds = as3ArrayToNSStringArray(argv[0]);
    
    if(assetIds == nil) {
        [AirImagePicker log:@"nil id array in fetchImages"];
        return nil;
    }
    
    FREObject imageMaxWidthObj = argv[1];
    int32_t imageMaxWidth;
    if(FREGetObjectAsInt32(imageMaxWidthObj, &imageMaxWidth) != FRE_OK) {
        return nil;
    };
    
    FREObject imageMaxHeightObj = argv[2];
    int32_t imageMaxHeight;
    if(FREGetObjectAsInt32(imageMaxHeightObj, &imageMaxHeight) != FRE_OK) {
        return nil;
    };
    
    uint32_t fillAll = NO;
    if(argc > 3) {
        FREObject fillAllObj = argv[3];
        FREGetObjectAsBool(fillAllObj, &fillAll);
    }
    
    //Here we handle the error or store the UIImage (can't make a BitmapData on another thread)
    void (^imageResultBlock)(UIImage * _Nullable, NSDictionary * _Nullable) =
        ^(UIImage * _Nullable result, NSDictionary * _Nullable info) {
            
            NSLog(@"Entering imageResultBlock");
            
            NSNumber *rid = [info objectForKey:PHImageResultRequestIDKey];
            PHImageRequestID requestId;
            if (rid != nil) {
                requestId = [rid intValue];
            } else {
                requestId = -1;
            }
            
            NSMutableDictionary *resultData = [[NSMutableDictionary alloc] init];
            if(rid != nil) {
                [resultData setObject:rid forKey:@"requestId"];
            }
            
            NSString * eventCode = IMAGE_LOAD_ERROR;
            NSError * error = [info valueForKey:PHImageErrorKey];
            if(error != nil) {
                [resultData setValue:[error localizedDescription] forKey:@"error"];
            } else {
                if(rid != nil && result != nil) {
                    [[AirImagePicker sharedInstance] storeUIImage:requestId image:result];
                    NSLog(@"%@Error getting image: %@", LOG_TAG, error.localizedDescription);
                    NSNumber * isDegraded = [info objectForKey:PHImageResultIsDegradedKey];
                    if([isDegraded boolValue] == YES) {
                        eventCode = IMAGE_LOAD_TEMP;
                    } else {
                        eventCode = IMAGE_LOAD_SUCCEEDED;
                        [AirImagePicker log:@"Image load succeeded"];
                    }
                    
                } else {
                    if ([info objectForKey:PHImageCancelledKey]){
                        eventCode = IMAGE_LOAD_CANCELLED;
                    }
                }
            }
            NSData * resultJSON = [NSJSONSerialization dataWithJSONObject:resultData options:nil error:nil];
            NSString * resultString = [[NSString alloc] initWithData:resultJSON encoding:NSUTF8StringEncoding];
            [AirImagePicker status:eventCode level:resultString];
        };
    
    PHImageManager *mgr = [PHImageManager defaultManager];
    PHImageContentMode mode = fillAll ? PHImageContentModeAspectFill : PHImageContentModeDefault;
    PHFetchResult *rslt = [PHAsset fetchAssetsWithLocalIdentifiers:assetIds options:nil];
    [AirImagePicker log:[NSString stringWithFormat:@"result has %lu count, input had %lu count", (unsigned long)rslt.count, (unsigned long)assetIds.count]];
    
    FREObject idsToReturn;
    FRENewObject((const uint8_t*)"Array", 0, NULL, &idsToReturn, nil);
    FRESetArrayLength(idsToReturn, (uint32_t)rslt.count);
    
    for (uint32_t i=0; i < rslt.count; ++i) {
        PHAsset * asset = [rslt objectAtIndex:i];
        PHImageRequestID requestId = [mgr requestImageForAsset:asset targetSize:CGSizeMake(imageMaxWidth, imageMaxHeight)
                                                   contentMode:mode options:nil resultHandler:imageResultBlock];
        FREObject idForAS3;
        FRENewObjectFromInt32(requestId, &idForAS3);
        FRESetArrayElementAt(idsToReturn, i, idForAS3);
    }
     NSLog(@"Exiting fetchImages");

    return idsToReturn;
}

// Takes a single request id, converts the fetched UIImage to a BitmapData, and deletes the reference to the UIImage
DEFINE_ANE_FUNCTION(retrieveFetchedImage)
{
    NSLog(@"Entering retrieveFetchedImage");
    PHImageRequestID requestId; // it's a typedef for int32_t
    if(FREGetObjectAsInt32(argv[0], &requestId) != FRE_OK) {
        return nil;
    }
    UIImage *image = [[AirImagePicker sharedInstance] retrieveUIImage:requestId];
    if(image == nil) {
        return nil;
    }
    FREObject newBitmap = createBitmapData(image.size.width, image.size.height);
    if(newBitmap == nil) {
        [AirImagePicker log:@"Couldn't create new bitmap!"];
        return nil;
    }
    
    if(fillBitmapData(context, newBitmap, image) != FRE_OK) {
        [AirImagePicker log:@"Fill bitmapdata not FRE_OK!"];
    }
    return newBitmap;
}

// Takes a single request id, converts the fetched UIImage to JPEG bytes, and deletes the reference to the UIImage
DEFINE_ANE_FUNCTION(retrieveFetchedImageAsFile)
{
    NSLog(@"Entering retrieveFetchedImageAsFile");
    PHImageRequestID requestId; // it's a typedef for int32_t
    if(FREGetObjectAsInt32(argv[0], &requestId) != FRE_OK) {
        return nil;
    }
    
    int32_t maxWidth;
    int32_t maxHeight;
    
    if(FREGetObjectAsInt32(argv[1], &maxWidth) != FRE_OK) {
        return nil;
    }
    if(FREGetObjectAsInt32(argv[2], &maxHeight) != FRE_OK) {
        return nil;
    }
    
    
    UIImage *image = [[AirImagePicker sharedInstance] retrieveUIImage:requestId];
    if(image == nil) {
        return nil;
    }
    
    image = [AirImagePicker resizeImage:image toMaxDimension:CGSizeMake((CGFloat) maxWidth, (CGFloat) maxWidth)];
    
    NSData *imageJPEGData = UIImageJPEGRepresentation(image, 0.8);
    
    FREObject newByteArray;
    if(FRENewObject((uint8_t *)"flash.utils.ByteArray", 0, NULL, &newByteArray , NULL) != FRE_OK) {
        return nil;
    };
    
    FREObject dataLength;
    FRENewObjectFromUint32((uint32_t) imageJPEGData.length, &dataLength);
    if (FRESetObjectProperty(newByteArray, (const uint8_t *)"length", dataLength, NULL) != FRE_OK) {
        return nil;
    }
    
    
    FREByteArray byteArray;
    if(FREAcquireByteArray(newByteArray, &byteArray) != FRE_OK) {
        return nil;
    };
    
    // Copy JPEG representation in ByteArray
     NSLog(@"do memcopy:");
    memcpy(byteArray.bytes, imageJPEGData.bytes, imageJPEGData.length);
     NSLog(@"did memcopy:");
    
    // Release our control over the ByteArray
    if(FREReleaseByteArray(newByteArray) != FRE_OK) {
        return nil;
    };
    
    return newByteArray;
}

//Cancel an image loading operation using a request id (or discard the image if it's loaded)
DEFINE_ANE_FUNCTION(cancelImageFetch)
{
    PHImageRequestID requestId; // it's a typedef for int32_t
    if(FREGetObjectAsInt32(argv[0], &requestId) != FRE_OK) {
        return nil;
    }
    [[AirImagePicker sharedInstance] retrieveUIImage:requestId];
    [[PHImageManager defaultManager] cancelImageRequest:requestId];
    
    return nil;
}

//Cancel an image loading operation using a request id (or discard the image if it's loaded)
DEFINE_ANE_FUNCTION(getCameraPermissionsState)
{
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    NSString * authState;
    switch (status) {
        case AVAuthorizationStatusAuthorized:
            authState = @"PERMISSION_AUTHORIZED";
            break;
        case AVAuthorizationStatusRestricted:
            authState = @"PERMISSION_RESTRICTED";
            break;
        case AVAuthorizationStatusDenied:
            authState = @"PERMISSION_DENIED";
            break;
        default:
            authState = @"PERMISSION_NOT_DETERMINED";
            break;
    }
    FREObject returnVal;
    FRENewObjectFromUTF8((uint32_t)authState.length, (const uint8_t *)[authState UTF8String], &returnVal);
    return returnVal;
}


//Cancel an image loading operation using a request id (or discard the image if it's loaded)
DEFINE_ANE_FUNCTION(getGalleryPermissionsState)
{
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    NSString * authState;
    switch (status) {
        case PHAuthorizationStatusAuthorized:
            authState = @"PERMISSION_AUTHORIZED";
            break;
        case PHAuthorizationStatusRestricted:
            authState = @"PERMISSION_RESTRICTED";
            break;
        case PHAuthorizationStatusDenied:
            authState = @"PERMISSION_DENIED";
            break;
        default:
            authState = @"PERMISSION_NOT_DETERMINED";
            break;
    }
    FREObject returnVal;
    FRENewObjectFromUTF8((uint32_t)authState.length, (const uint8_t *)[authState UTF8String], &returnVal);
    return returnVal;
}

DEFINE_ANE_FUNCTION(canOpenSettings)
{
    NSLog(@"Entering imagePickerController:canOpenSettings");
    BOOL canOpenSettings = (&UIApplicationOpenSettingsURLString != NULL);
    FREObject returnVal;
    FRENewObjectFromBool(canOpenSettings, &returnVal);
    return returnVal;
}


DEFINE_ANE_FUNCTION(tryToOpenSettings)
{
    NSLog(@"Entering tryToOpenSettings");
    BOOL canOpenSettings = (&UIApplicationOpenSettingsURLString != NULL);
    if (canOpenSettings) {
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString]];
    }
    FREObject returnVal;
    FRENewObjectFromBool(canOpenSettings, &returnVal);
    return returnVal;
}

// ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet)
{
    // Register the links btwn AS3 and ObjC. (dont forget to modify the nbFuntionsToLink integer if you are adding/removing functions)
    NSInteger nbFuntionsToLink = 22;
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
    
    func[11].name = (const uint8_t*) "cleanUpTemporaryDirectoryContent";
    func[11].functionData = NULL;
    func[11].function = &cleanUpTemporaryDirectoryContent;
    
    func[12].name = (const uint8_t*) "isCropAvailable";
    func[12].functionData = NULL;
    func[12].function = &isCropAvailable;
    
    // in-app stuff to get recent photos
    
    func[13].name = (const uint8_t*) "getRecentImageIds";
    func[13].functionData = NULL;
    func[13].function = &getRecentImageIds;
    
    func[14].name = (const uint8_t*) "fetchImages";
    func[14].functionData = NULL;
    func[14].function = &fetchImages;
    
    func[15].name = (const uint8_t*) "retrieveFetchedImage";
    func[15].functionData = NULL;
    func[15].function = &retrieveFetchedImage;
    
    func[16].name = (const uint8_t*) "retrieveFetchedImageAsFile";
    func[16].functionData = NULL;
    func[16].function = &retrieveFetchedImageAsFile;
    
    func[17].name = (const uint8_t*) "cancelImageFetch";
    func[17].functionData = NULL;
    func[17].function = &cancelImageFetch;
    
    func[18].name = (const uint8_t*) "getCameraPermissionsState";
    func[18].functionData = NULL;
    func[18].function = &getCameraPermissionsState;
    
    func[19].name = (const uint8_t*) "getGalleryPermissionsState";
    func[19].functionData = NULL;
    func[19].function = &getGalleryPermissionsState;
    
    func[20].name = (const uint8_t*) "canOpenSettings";
    func[20].functionData = NULL;
    func[20].function = &canOpenSettings;
    
    //TODO Peter try to figure out why this doesn't work:
    func[21].name = (const uint8_t*) "tryToOpenSettings";
    func[21].functionData = NULL;
    func[21].function = &tryToOpenSettings;
    
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
