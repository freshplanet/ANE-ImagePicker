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

#define PRINT_LOG   YES
#define LOG_TAG     @"AirImagePicker"

FREContext AirIPCtx = nil;

@implementation AirImagePicker

@synthesize imagePicker = _imagePicker;
@synthesize popover = _popover;
@synthesize pickedImage = _pickedImage;

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
    [super dealloc];
}

+ (void)log:(NSString *)message
{
    if (PRINT_LOG) NSLog(@"[%@] %@", LOG_TAG, message);
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"LOGGING", (const uint8_t *)[message UTF8String]);
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType
{
    UIViewController *rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    self.imagePicker = [[[UIImagePickerController alloc] init] autorelease];
    self.imagePicker.sourceType = sourceType;
    self.imagePicker.allowsEditing = YES;
    self.imagePicker.delegate = self;
    
    // Image picker should always be presented fullscreen on iPhone and iPod Touch.
    // It should be presented fullscreen on iPad only if it's the camera. Otherwise, we use a popover.
    if (sourceType == UIImagePickerControllerSourceTypeCamera || UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone)
    {
        [rootViewController presentViewController:self.imagePicker animated:YES completion:NULL];
    }
    else
    {
        self.popover = [[[UIPopoverController alloc] initWithContentViewController:self.imagePicker] autorelease];
        [self.popover presentPopoverFromRect:rootViewController.view.bounds inView:rootViewController.view
                    permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}

#pragma mark - UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info
{
    _pickedImage = [(UIImage *)[info objectForKey:UIImagePickerControllerEditedImage] retain];
    
    if (self.popover)
    {
        [self.popover dismissPopoverAnimated:YES];
        self.popover = nil;
    }
    else
    {
        [self.imagePicker dismissViewControllerAnimated:YES completion:NULL];
        self.imagePicker = nil;
    }
    
    FREDispatchStatusEventAsync(AirIPCtx, (const uint8_t *)"DID_FINISH_PICKING", (const uint8_t *)"OK");
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
        [self.imagePicker dismissViewControllerAnimated:YES completion:NULL];
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
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary];
    
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
    [[AirImagePicker sharedInstance] displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera];
    
    return nil;
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


// ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet)
{
    // Register the links btwn AS3 and ObjC. (dont forget to modify the nbFuntionsToLink integer if you are adding/removing functions)
    NSInteger nbFuntionsToLink = 4;
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
    
    func[4].name = (const uint8_t*) "drawPickedImageToBitmapData";
    func[4].functionData = NULL;
    func[4].function = &drawPickedImageToBitmapData;
    
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