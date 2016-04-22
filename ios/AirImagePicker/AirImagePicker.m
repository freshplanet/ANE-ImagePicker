//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//
//  Copyright 2016 VoiceThread (https://voicethread.com/)
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
#import <QuartzCore/QuartzCore.h>
#import <OpenGLES/ES1/gl.h>
#import <OpenGLES/ES1/glext.h>
#import "UIImage+Resize.h"

#import "AssetPickerController.h"

#define PRINT_LOG   YES
#define LOG_TAG     @"AirImagePicker"

FREContext AirIPCtx = nil;

@interface AirImagePicker ()
{
    UIView *_overlay;
}
@end

@implementation AirImagePicker

@synthesize imagePicker = _imagePicker;
@synthesize popover = _popover;

static AirImagePicker *sharedInstance = nil;

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

- (void)dealloc
{
    [_imagePicker release];
    [_popover release];
    [_overlay release];
    [super dealloc];
}

+ (void)log:(NSString *)message
{
    if (PRINT_LOG) NSLog(@"[%@] %@", LOG_TAG, message);
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"LOGGING", (const uint8_t *)[message UTF8String]);
}

- (NSURL *)tempFileURLWithPrefix:(NSString *)type extension:(NSString *)extension; {
  return([NSURL fileURLWithPath:
    [NSString stringWithFormat:@"%@%@_%08x_%08x.%@",
      NSTemporaryDirectory(),
      type,
      (uint32_t)arc4random(), 
      (uint32_t)floor([[NSDate date] timeIntervalSince1970]),
      extension]]);
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType 
          allowVideo:(BOOL)allowVideo allowMultiple:(BOOL)allowMultiple crop:(BOOL)crop  
          anchor:(CGRect)anchor
{
    UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    self.imagePicker = [[[UIImagePickerController alloc] init] autorelease];
    self.imagePicker.sourceType = sourceType;
    self.imagePicker.allowsEditing = crop;
    self.imagePicker.delegate = self;
    if (allowVideo == true) {
        // there are memory leaks that are not occuring if we use CoreFoundation C code
        CFStringRef mTypes[2] = { kUTTypeImage, kUTTypeMovie };
        CFArrayRef mTypesArray = CFArrayCreate(CFAllocatorGetDefault(), (const void **)mTypes, 2, &kCFTypeArrayCallBacks);
        self.imagePicker.mediaTypes = (NSArray*) mTypesArray;
        CFRelease(mTypesArray);
    }
    
    // Image picker should always be presented fullscreen on iPhone and iPod Touch.
    // It should be presented fullscreen on iPad only if it's the camera. Otherwise, we use a popover.
    if (sourceType == UIImagePickerControllerSourceTypeCamera || UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone)
    {
        [rootViewController presentModalViewController:self.imagePicker animated:YES];
    }
    else
    {
        self.popover = [[[UIPopoverController alloc] initWithContentViewController:self.imagePicker] autorelease];
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
    [_overlay release];
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

        UIImage *pickedImage = nil;
        
        // Retrieve image
        BOOL crop = YES;
        if (editedImage)
        {
            pickedImage = editedImage;
        }
        else
        {
            crop = NO;
            pickedImage = originalImage;
        }
        
        if (!crop)
        {
            // Unedited images may have an incorrect orientation. We fix it.
            pickedImage = [pickedImage resizedImageWithContentMode:UIViewContentModeScaleAspectFit bounds:pickedImage.size interpolationQuality:kCGInterpolationDefault];
        }
        else if (pickedImage.size.width != pickedImage.size.height)
        {
            // If image is not square (happens if the user didn't zoom enough when cropping), we add black areas around
            CGFloat longestEdge = MAX(pickedImage.size.width, pickedImage.size.height);
            CGRect drawRect = CGRectZero;
            drawRect.size = pickedImage.size;
            if (pickedImage.size.width > pickedImage.size.height)
            {
                drawRect.origin.y = (longestEdge - pickedImage.size.height) / 2;
            }
            else
            {
                drawRect.origin.x = (longestEdge - pickedImage.size.width) / 2;
            }
            
            // Prepare drawing context
            CGImageRef imageRef = pickedImage.CGImage;
            CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
            CGContextRef context = CGBitmapContextCreate(NULL, longestEdge, longestEdge, 8, 0, colorSpace, kCGImageAlphaPremultipliedLast);
            CGColorSpaceRelease(colorSpace);
            
            // Draw new image
            UIColor *black = [UIColor blackColor];
            CGContextSetFillColorWithColor(context, black.CGColor);
            CGContextFillRect(context, CGRectMake(0, 0, longestEdge, longestEdge));
            CGContextDrawImage(context, drawRect, imageRef);
            CGImageRef newImageRef = CGBitmapContextCreateImage(context);
            pickedImage = [UIImage imageWithCGImage:newImageRef];
            
            // Clean up
            CGContextRelease(context);
            CGImageRelease(newImageRef);
        }
        
        // JPEG compression
        NSData *data = UIImageJPEGRepresentation(pickedImage, 1.0);
        
        // Save image to a temporary path on disk
        NSURL *toURL = [self tempFileURLWithPrefix:@"image" extension:@"jpg"];
        NSError *error = nil;
        [data writeToURL:toURL options:0 error:&error];
        if (error != nil) {
          NSLog(@"AirImagePicker:  Error while saving to temp file: %@", 
            [error description]);
          FREDispatchStatusEventAsync(AirIPCtx,
            (const uint8_t *)"ERROR_GENERATING_VIDEO",
            (const uint8_t *)[[error description] UTF8String]);
        }
        else {
          [self returnMediaURL:toURL];
        }
    });
    dispatch_release(thread);

}

- (void) onVideoPickedWithMediaURL:(NSURL*)mediaURL
{
    NSLog(@"Entering onVideoPickedWithVideoPath");
    
    // For some weird reason, returning the path from the video stored on the
    // camera roll is not working.  My solution is to copy the video to the
    // app folder.
    
    // copying the video and generating the thumbnail can take time,
    // this is why we do it on another thread, so we do not block execution.
    dispatch_queue_t thread = dispatch_queue_create("video processing", NULL);
    dispatch_async(thread, ^{
        
        // Save a copy of the picked video to the temp directory
        NSURL *toURL = [self tempFileURLWithPrefix:@"movie" extension:@"mov"];

        NSError *fileError;
        NSFileManager *fileManager = [[NSFileManager alloc] init];
        
        // Check if file exists, if it does, delete it.
        if ( [toURL checkResourceIsReachableAndReturnError:&fileError] == YES )
        {
            [fileManager removeItemAtPath:[toURL path] error:NULL];
        }
        
        fileError = NULL;
        
        // Attempt the copy, and if it fails, log the error.
        if ( !([fileManager copyItemAtURL:mediaURL toURL:toURL error:&fileError]) )
        {
            NSLog(@"Couldn't copy video, error: %@", fileError);
            [fileManager release];  // we are done with the file manager, release it.
            dispatch_async(dispatch_get_main_queue(), ^{
                FREDispatchStatusEventAsync(AirIPCtx,
                                            (const uint8_t *)"ERROR_GENERATING_VIDEO",
                                            (const uint8_t *)[[fileError description] UTF8String]);
            });
        }
        else
        {
            [self returnMediaURL:toURL];
        }
    });
    dispatch_release(thread);
    
    NSLog(@"Exiting onVideoPickedWithVideoPath");
}

// Let the native extension know that we are done with the picking
- (void) returnMediaURL:(NSURL*)mediaURL {
  dispatch_async(dispatch_get_main_queue(), ^{
      FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", 
        (const uint8_t *)[[mediaURL path] UTF8String]);
  });
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
    uint32_t allowVideoValue;
    FREObject allowVideoObj = argv[0];
    FREGetObjectAsBool(allowVideoObj, &allowVideoValue);
    BOOL allowVideo = (allowVideoValue != 0);
    
    uint32_t allowMultipleValue;
    FREObject allowMultipleObject = argv[1];
    FREGetObjectAsBool(allowMultipleObject, &allowMultipleValue);
    BOOL allowMultiple = (allowMultipleValue != 0);
    
    uint32_t cropValue;
    FREObject cropObject = argv[2];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
    
    CGRect anchor;
    if (argc > 3)
    {
        // Extract anchor properties
        FREObject anchorObject = argv[3];
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
    
    [[AirImagePicker sharedInstance] 
      displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary 
        allowVideo:allowVideo allowMultiple:allowMultiple crop:crop 
        anchor:anchor];
    
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
    uint32_t allowVideoValue;
    FREObject allowVideoObj = argv[0];
    FREGetObjectAsBool(allowVideoObj, &allowVideoValue);
    BOOL allowVideo = (allowVideoValue != 0);
    
    uint32_t cropValue;
    FREObject cropObject = argv[1];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
    
    [[AirImagePicker sharedInstance] 
      displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera 
      allowVideo:allowVideo allowMultiple:NO crop:crop anchor:CGRectZero];
    
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

// ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet)
{
    // Register the links btwn AS3 and ObjC. (dont forget to modify the nbFuntionsToLink integer if you are adding/removing functions)
    NSInteger nbFuntionsToLink = 6;
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
    
    func[4].name = (const uint8_t*) "displayOverlay";
    func[4].functionData = NULL;
    func[4].function = &displayOverlay;
    
    func[5].name = (const uint8_t*) "removeOverlay";
    func[5].functionData = NULL;
    func[5].function = &removeOverlay;
    
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
