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
#import "UIImage+Resize.h"

#define PRINT_LOG   YES
#define LOG_TAG     @"AirImagePicker"

FREContext AirIPCtx = nil;

@implementation AirImagePicker

@synthesize imagePicker = _imagePicker;
@synthesize popover = _popover;
@synthesize pickedImage = _pickedImage;
@synthesize pickedImageJPEGData = _pickedImageJPEGData;

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
    return [self sharedInstance];
}

- (id)copy
{
    return self;
}

- (void)dealloc
{
    [_imagePicker release];
    [_popover release];
    [_pickedImage release];
    [_pickedImageJPEGData release];
    [super dealloc];
}

+ (void)log:(NSString *)message
{
    if (PRINT_LOG) NSLog(@"[%@] %@", LOG_TAG, message);
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"LOGGING", (const uint8_t *)[message UTF8String]);
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType crop:(BOOL)crop anchor:(CGRect)anchor
{
    UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    self.imagePicker = [[[UIImagePickerController alloc] init] autorelease];
    self.imagePicker.sourceType = sourceType;
    self.imagePicker.allowsEditing = crop;
    self.imagePicker.delegate = self;
    
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

#pragma mark - UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    // Dismiss the UI
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
    
    // Process image in background thread
    dispatch_queue_t thread = dispatch_queue_create("image processing", NULL);
    dispatch_async(thread, ^{
        
        // Retrieve image
        BOOL crop = YES;
        _pickedImage = [info objectForKey:UIImagePickerControllerEditedImage];
        if (!_pickedImage)
        {
            crop = NO;
            _pickedImage = [info objectForKey:UIImagePickerControllerOriginalImage];
        }
        
        if (!crop)
        {
            // Unedited images may have an incorrect orientation. We fix it.
            _pickedImage = [_pickedImage resizedImageWithContentMode:UIViewContentModeScaleAspectFit bounds:_pickedImage.size interpolationQuality:kCGInterpolationDefault];
        }
        else if (_pickedImage.size.width != _pickedImage.size.height)
        {
            // If image is not square (happens if the user didn't zoom enough when cropping), we add black areas around
            CGFloat longestEdge = MAX(_pickedImage.size.width, _pickedImage.size.height);
            CGRect drawRect = CGRectZero;
            drawRect.size = _pickedImage.size;
            if (_pickedImage.size.width > _pickedImage.size.height)
            {
                drawRect.origin.y = (longestEdge - _pickedImage.size.height) / 2;
            }
            else
            {
                drawRect.origin.x = (longestEdge - _pickedImage.size.width) / 2;
            }
            
            // Prepare drawing context
            CGImageRef imageRef = _pickedImage.CGImage;
            CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
            CGContextRef context = CGBitmapContextCreate(NULL, longestEdge, longestEdge, 8, 0, colorSpace, kCGImageAlphaPremultipliedLast);
            CGColorSpaceRelease(colorSpace);
            
            // Draw new image
            UIColor *black = [UIColor blackColor];
            CGContextSetFillColorWithColor(context, black.CGColor);
            CGContextFillRect(context, CGRectMake(0, 0, longestEdge, longestEdge));
            CGContextDrawImage(context, drawRect, imageRef);
            CGImageRef newImageRef = CGBitmapContextCreateImage(context);
            _pickedImage = [UIImage imageWithCGImage:newImageRef];
            
            // Clean up
            CGContextRelease(context);
            CGImageRelease(newImageRef);
        }
        
        // JPEG compression
        _pickedImageJPEGData = UIImageJPEGRepresentation(_pickedImage, 1.0);
        
        [_pickedImage retain];
        [_pickedImageJPEGData retain];
        
        FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"OK");
        
    });
    dispatch_release(thread);
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
    uint32_t cropValue;
    FREObject cropObject = argv[0];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
    
    CGRect anchor;
    if (argc > 1)
    {
        // Extract anchor properties
        FREObject anchorObject = argv[1];
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
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary crop:crop anchor:anchor];
    
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
    uint32_t cropValue;
    FREObject cropObject = argv[0];
    FREGetObjectAsBool(cropObject, &cropValue);
    BOOL crop = (cropValue != 0);
    
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera crop:crop anchor:CGRectZero];
    
    return nil;
}

DEFINE_ANE_FUNCTION(getPickedImageWidth)
{
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        CGImageRef imageRef = [pickedImage CGImage];
        NSUInteger width = CGImageGetWidth(imageRef);
        
        FREObject result;
        if (FRENewObjectFromUint32(width, &result) == FRE_OK)
        {
            return result;
        }
        else return nil;
    }
    else return nil;
}

DEFINE_ANE_FUNCTION(getPickedImageHeight)
{
    UIImage *pickedImage = [[AirImagePicker sharedInstance] pickedImage];
    
    if (pickedImage)
    {
        CGImageRef imageRef = [pickedImage CGImage];
        NSUInteger height = CGImageGetHeight(imageRef);
        
        FREObject result;
        if (FRENewObjectFromUint32(height, &result) == FRE_OK)
        {
            return result;
        }
        else return nil;
    }
    else return nil;
}

DEFINE_ANE_FUNCTION(drawPickedImageToBitmapData)
{
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
    
    return nil;
}

DEFINE_ANE_FUNCTION(getPickedImageJPEGRepresentationSize)
{
    NSData *jpegData = [[AirImagePicker sharedInstance] pickedImageJPEGData];
    
    if (jpegData)
    {
        FREObject result;
        if (FRENewObjectFromUint32(jpegData.length, &result) == FRE_OK)
        {
            return result;
        }
        else return nil;
    }
    else return nil;
}

DEFINE_ANE_FUNCTION(copyPickedImageJPEGRepresentationToByteArray)
{
    NSData *jpegData = [[AirImagePicker sharedInstance] pickedImageJPEGData];
    
    if (jpegData)
    {
        // Get the AS3 ByteArray
        FREByteArray byteArray;
        FREAcquireByteArray(argv[0], &byteArray);
        
        // Copy JPEG representation in ByteArray
        memcpy(byteArray.bytes, jpegData.bytes, jpegData.length);
        
        // Release our control over the ByteArray
        FREReleaseByteArray(argv[0]);
    }
    
    return nil;
}


// ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet)
{
    // Register the links btwn AS3 and ObjC. (dont forget to modify the nbFuntionsToLink integer if you are adding/removing functions)
    NSInteger nbFuntionsToLink = 9;
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
    
    func[7].name = (const uint8_t*) "getPickedImageJPEGRepresentationSize";
    func[7].functionData = NULL;
    func[7].function = &getPickedImageJPEGRepresentationSize;
    
    func[8].name = (const uint8_t*) "copyPickedImageJPEGRepresentationToByteArray";
    func[8].functionData = NULL;
    func[8].function = &copyPickedImageJPEGRepresentationToByteArray;
    
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