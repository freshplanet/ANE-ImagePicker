/*
 * Copyright 2017 FreshPlanet
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "AirImagePicker.h"
#import "Constants.h"
#import "UIImage+Resize.h"

AirImagePicker* GetAirImagePickerContextNativeData(FREContext context) {
    
    CFTypeRef controller;
    FREGetContextNativeData(context, (void**)&controller);
    return (__bridge AirImagePicker*)controller;
}

@interface AirImagePicker ()
    @property (nonatomic, readonly) FREContext context;
@end


@implementation AirImagePicker


- (instancetype)initWithContext:(FREContext)extensionContext {
    
    if ((self = [super init])) {
        
        _context = extensionContext;
        _storedImages = [[NSMutableDictionary alloc] init];
    }
    
    return self;
}

- (void) sendLog:(NSString*)log {
    [self sendEvent:@"log" level:log];
}

- (void) sendEvent:(NSString*)code {
    [self sendEvent:code level:@""];
}

- (void) sendEvent:(NSString*)code level:(NSString*)level {
    FREDispatchStatusEventAsync(_context, (const uint8_t*)[code UTF8String], (const uint8_t*)[level UTF8String]);
}

- (void) storeImage:(NSString*)imageName image:(UIImage *)image
{
    [_storedImages setObject:image forKey:imageName];
}

- (UIImage *) getStoredImage:(NSString*)imageName{
    UIImage *image = [_storedImages valueForKey:imageName];
    return image;
}

- (void) removeStoredImage:(NSString*)imageName{
    [_storedImages removeObjectForKey:imageName];
    
}

- (NSString*) dictionaryToNSString:(NSDictionary*)dictionary {
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictionary
                                                       options:NSJSONWritingPrettyPrinted // Pass 0 if you don't care about the readability of the generated string
                                                         error:&error];
    if (! jsonData) {
        return @"";
    } else {
        return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
}

- (UIImage *) resizeImage:(UIImage *)image toMaxDimension:(CGSize)maxDimensions forceSquare:(BOOL)fSquare {
    
    if (fSquare && image.size.width != image.size.height) {
        
        CGFloat screenWidth = [UIScreen mainScreen].bounds.size.width;
        // If image is not square (happens if the user didn't zoom enough when cropping), we add black areas around
        CGFloat longestEdge;
        
        // hack for iPad issue on iOS 11 - remove when the issue is fixed
        if(UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
            longestEdge = MIN(image.size.width, image.size.height);
        }
        else {
            longestEdge = MAX(image.size.width, image.size.height);
        }
        
        
        
        CGRect drawRect = CGRectZero;
        drawRect.size = image.size;
        if (image.size.width > image.size.height){
            drawRect.origin.y = (longestEdge - image.size.height) / 2;
        }
        else{
            drawRect.origin.x = (longestEdge - image.size.width) / 2;
        }
        
        // Prepare drawing context
        CGImageRef imageRef = image.CGImage;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        CGContextRef context = CGBitmapContextCreate(NULL, longestEdge, longestEdge, 8, 0, colorSpace, kCGImageAlphaPremultipliedLast);
        CGColorSpaceRelease(colorSpace);
        
        // Draw new image
        UIColor *black = [UIColor blackColor];
        CGContextSetFillColorWithColor(context, black.CGColor);
        CGContextFillRect(context, CGRectMake(0, 0, longestEdge, longestEdge));
        CGContextDrawImage(context, drawRect, imageRef);
        CGImageRef newImageRef = CGBitmapContextCreateImage(context);
        image = [UIImage imageWithCGImage:newImageRef];
        
        // Clean up
        CGContextRelease(context);
        CGImageRelease(newImageRef);
    }
    
    // make sure that the image has the correct size
    if ( (image.size.width > maxDimensions.width || image.size.height > maxDimensions.height ) &&
        maxDimensions.width > 0 && maxDimensions.height > 0) {
        float reductionFactor = MAX(image.size.width / maxDimensions.width, image.size.height / maxDimensions.height);
        CGSize newSize = CGSizeMake(image.size.width/reductionFactor, image.size.height/reductionFactor);
        
        image = [image resizedImage:newSize interpolationQuality:kCGInterpolationMedium];
        
    }
    return image;
}

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType crop:(BOOL)crop anchor:(CGRect)anchor maxDimensions:(CGSize)maxDimensions {
    
    _maxDimensions = maxDimensions;
    UIViewController* rootViewController = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.delegate = self;
    picker.allowsEditing = crop;
    picker.sourceType = sourceType;
    if (sourceType == UIImagePickerControllerSourceTypePhotoLibrary && UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad && anchor.size.width > 0 && anchor.size.height > 0) {
        picker.modalPresentationStyle = UIModalPresentationPopover;
        picker.popoverPresentationController.sourceView = rootViewController.view;
        picker.popoverPresentationController.delegate = self;
        picker.popoverPresentationController.sourceRect = anchor;
        [picker.popoverPresentationController setPermittedArrowDirections:UIPopoverArrowDirectionLeft];
        
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [rootViewController presentViewController:picker animated:true completion:nil];
    });
    
    
}

- (void)getRecentImages:(CGSize)maxDimensions fetchLimit:(NSInteger)fetchLimit  {
    
    
    PHFetchOptions *opts = [[PHFetchOptions alloc] init];
    opts.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:NO]];
    opts.includeHiddenAssets = false;
    opts.includeAllBurstAssets = false;
    opts.includeAssetSourceTypes = PHAssetSourceTypeUserLibrary;
    opts.fetchLimit = fetchLimit;
    
    PHImageRequestOptions *requestOpts =  [[PHImageRequestOptions alloc] init];
    [requestOpts setSynchronous:true];// same order
    
    PHImageManager *manager = [PHImageManager defaultManager];
    PHFetchResult *rslt = [PHAsset fetchAssetsWithMediaType:PHAssetMediaTypeImage options:opts];
    
    __block NSInteger loadedCount = 0;
    
    NSMutableDictionary *imagePathsResult = [[NSMutableDictionary alloc] init];
    NSMutableArray *imagePaths = [[NSMutableArray alloc] init];
    
    
    for (int i=0; i < rslt.count; ++i) {
        PHAsset *asset = [rslt objectAtIndex:i];
        NSString *assetId = asset.localIdentifier;
        [manager requestImageForAsset:asset targetSize:PHImageManagerMaximumSize contentMode:PHImageContentModeDefault options:requestOpts resultHandler:^(UIImage * _Nullable result, NSDictionary * _Nullable info) {
            
            loadedCount = loadedCount + 1;
            if (result != nil) {
                
                NSString *imagePath = [[NSUUID UUID] UUIDString];
                [imagePaths addObject:imagePath];
                
                result = [result resizedImageWithContentMode:UIViewContentModeScaleAspectFit bounds:result.size interpolationQuality:kCGInterpolationDefault];
                result = [self resizeImage:result toMaxDimension:maxDimensions forceSquare:false];
                [self storeImage:imagePath image:result];
                
                if (loadedCount == rslt.count) {
                    // finished
                    [imagePathsResult setValue:imagePaths forKey:@"imagePaths"];
                    [self sendEvent:kRecentResult level:[self dictionaryToNSString:imagePathsResult]];
                    
                    
                }
            }
            
        }];
        
    }
    
}

#pragma mark - UIImagePickerControllerDelegate

-(void) imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info {

    [picker dismissViewControllerAnimated:true completion:nil];
    
    UIImage *chosenImage = info[UIImagePickerControllerOriginalImage];
    UIImage *chosenEditedImage = info[UIImagePickerControllerEditedImage];
    NSURL *referenceURL = info[UIImagePickerControllerReferenceURL];
    NSString *imagePath = referenceURL == nil ? [[NSUUID UUID] UUIDString] : referenceURL.path ;
    
    if (chosenImage != nil) {
        chosenImage = [chosenImage resizedImageWithContentMode:UIViewContentModeScaleAspectFit bounds:chosenImage.size interpolationQuality:kCGInterpolationDefault];
        chosenImage = [self resizeImage:chosenImage toMaxDimension:_maxDimensions forceSquare:false];
    }
   
    if (chosenEditedImage != nil) {
        chosenEditedImage = [self resizeImage:chosenEditedImage toMaxDimension:_maxDimensions forceSquare:true];
    }
    
    
    [self storeImage:imagePath image:chosenEditedImage ? chosenEditedImage : chosenImage];
    [self sendEvent:kPhotoChosen level:imagePath];
    
}

-(void) imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [picker dismissViewControllerAnimated:true completion:nil];
    [self sendEvent:kAirImagePickerDataEvent_cancelled];
    
}

#pragma mark - UIPopoverPresentationControllerDelegate

-(void)popoverPresentationControllerDidDismissPopover:(UIPopoverPresentationController *)popoverPresentationController {
    [self sendEvent:kAirImagePickerDataEvent_cancelled];
}

@end



DEFINE_ANE_FUNCTION(displayImagePicker) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    @try {
        NSInteger maxWidth = AirImagePicker_FPANE_FREObjectToInt(argv[0]);
        NSInteger maxHeight = AirImagePicker_FPANE_FREObjectToInt(argv[1]);
        BOOL crop = AirImagePicker_FPANE_FREObjectToBool(argv[2]);
        CGRect anchor;
        
        if (argc > 3) {
            anchor = CGRectMake(AirImagePicker_FPANE_FREObjectToDouble(argv[3]), AirImagePicker_FPANE_FREObjectToDouble(argv[4]), AirImagePicker_FPANE_FREObjectToDouble(argv[5]), AirImagePicker_FPANE_FREObjectToDouble(argv[6]));
        }
        
        
        PHAuthorizationStatus  status = [PHPhotoLibrary authorizationStatus];
        if (status == PHAuthorizationStatusNotDetermined) {
            
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                
                if (status == PHAuthorizationStatusAuthorized) {
                    // Access has been granted.
                    
                    [controller displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary crop:crop anchor:anchor maxDimensions:CGSizeMake((float)maxWidth, (float)maxHeight)];
                        
                }
                else {
                    // Access has not been granted.
                    [controller sendEvent:kAirImagePickerErrorEvent_galleryPermissionError];
                }
                
                
            }];
        }
        else if (status != PHAuthorizationStatusAuthorized) {
            [controller sendEvent:kAirImagePickerErrorEvent_galleryPermissionError];
        }
        else {
            [controller displayImagePickerWithSourceType:UIImagePickerControllerSourceTypePhotoLibrary crop:crop anchor:anchor maxDimensions:CGSizeMake((float)maxWidth, (float)maxHeight)];
        }
        
        
        

    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to displayImagePicker: " stringByAppendingString:exception.reason]];
        
    }

    return nil;
}

DEFINE_ANE_FUNCTION(displayCamera) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    @try {
        NSInteger maxWidth = AirImagePicker_FPANE_FREObjectToInt(argv[0]);
        NSInteger maxHeight = AirImagePicker_FPANE_FREObjectToInt(argv[1]);
        BOOL crop = AirImagePicker_FPANE_FREObjectToBool(argv[2]);
        
        AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
        if (status == AVAuthorizationStatusNotDetermined) {
            
            [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted) {
                if (granted) {
                    [controller displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera crop:crop anchor:CGRectZero maxDimensions:CGSizeMake((float)maxWidth, (float)maxHeight)];
                } else {
                    [controller sendEvent:kAirImagePickerErrorEvent_cameraPermissionError];
                }
            }];
          
        }
        else if (status != AVAuthorizationStatusAuthorized) {
            [controller sendEvent:kAirImagePickerErrorEvent_cameraPermissionError];
        }
        else {
            [controller displayImagePickerWithSourceType:UIImagePickerControllerSourceTypeCamera crop:crop anchor:CGRectZero maxDimensions:CGSizeMake((float)maxWidth, (float)maxHeight)];
        }
        
        
        
    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to displayImagePicker: " stringByAppendingString:exception.reason]];
        
    }
    
    return nil;
}

DEFINE_ANE_FUNCTION(loadRecentImages) {
    
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    
    @try {
        NSInteger fetchLimit = AirImagePicker_FPANE_FREObjectToInt(argv[0]);
        NSInteger maxWidth = AirImagePicker_FPANE_FREObjectToInt(argv[1]);
        NSInteger maxHeight = AirImagePicker_FPANE_FREObjectToInt(argv[2]);
        
        PHAuthorizationStatus  status = [PHPhotoLibrary authorizationStatus];
        if (status == PHAuthorizationStatusNotDetermined) {
        
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                
                if (status == PHAuthorizationStatusAuthorized) {
                    // Access has been granted.
                    [controller getRecentImages:CGSizeMake(maxWidth, maxHeight) fetchLimit:fetchLimit];
                }
               else {
                    // Access has not been granted.
                   [controller sendEvent:kAirImagePickerRecentImagesEvent_onPremissionError];
                }
                
                
            }];
        }
        else if (status != PHAuthorizationStatusAuthorized) {
            [controller sendEvent:kAirImagePickerRecentImagesEvent_onPremissionError];
        }
        else {
            [controller getRecentImages:CGSizeMake(maxWidth, maxHeight) fetchLimit:fetchLimit];
        }
        
    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to displayImagePicker: " stringByAppendingString:exception.reason]];
        
    }
    
    return nil;
}

DEFINE_ANE_FUNCTION(getCameraPermissionStatus) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    NSString * authState;
    switch (status) {
        case AVAuthorizationStatusAuthorized:
            authState = @"authorized";
            break;
        case AVAuthorizationStatusRestricted:
            authState = @"restricted";
            break;
        case AVAuthorizationStatusDenied:
            authState = @"denied";
            break;
        default:
            authState = @"not_determined";
            break;
    }
    
    return AirImagePicker_FPANE_NSStringToFREObject(authState);
}

DEFINE_ANE_FUNCTION(getGalleryPermissionStatus) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    NSString * authState;
    switch (status) {
        case PHAuthorizationStatusAuthorized:
            authState = @"authorized";
            break;
        case PHAuthorizationStatusRestricted:
            authState = @"restricted";
            break;
        case PHAuthorizationStatusDenied:
            authState = @"denied";
            break;
        default:
            authState = @"not_determined";
            break;
    }
    return AirImagePicker_FPANE_NSStringToFREObject(authState);
}

DEFINE_ANE_FUNCTION(openSettings) {
    
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:UIApplicationOpenSettingsURLString]];
    
    return nil;
}

DEFINE_ANE_FUNCTION(internalGetChosenPhotoBitmapData) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    @try {
        NSString *imagePath = AirImagePicker_FPANE_FREObjectToNSString((argv[0]));
        UIImage *image= [controller getStoredImage:imagePath];
        return AirImagePicker_FPANE_UIImageToFREBitmapData(image);
    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to internalGetChosenPhoto: " stringByAppendingString:exception.reason]];
        
    }
    return nil;
}

DEFINE_ANE_FUNCTION(internalGetChosenPhotoByteArray) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    @try {
        NSString *imagePath = AirImagePicker_FPANE_FREObjectToNSString((argv[0]));
        UIImage *image= [controller getStoredImage:imagePath];
        return AirImagePicker_FPANE_UIImageToFREByteArray(image);
    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to internalGetChosenPhotoByteArray: " stringByAppendingString:exception.reason]];
        
    }
    return nil;
}

DEFINE_ANE_FUNCTION(internalRemoveStoredImage) {
    
    AirImagePicker* controller = GetAirImagePickerContextNativeData(context);
    
    if (!controller)
        return AirImagePicker_FPANE_CreateError(@"context's AirImagePicker is null", 0);
    
    @try {
        NSString *imagePath = AirImagePicker_FPANE_FREObjectToNSString((argv[0]));
        [controller removeStoredImage:imagePath];
        
    }
    @catch (NSException *exception) {
        [controller sendLog:[@"Exception occured while trying to internalGetChosenPhotoByteArray: " stringByAppendingString:exception.reason]];
        
    }
    return nil;
}

#pragma mark - ANE setup

void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx,
                                 uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet) {
    
    AirImagePicker* controller = [[AirImagePicker alloc] initWithContext:ctx];
    FRESetContextNativeData(ctx, (void*)CFBridgingRetain(controller));
    
    static FRENamedFunction functions[] = {
        MAP_FUNCTION(displayImagePicker, NULL),
        MAP_FUNCTION(displayCamera, NULL),
        MAP_FUNCTION(loadRecentImages, NULL),
        MAP_FUNCTION(getCameraPermissionStatus, NULL),
        MAP_FUNCTION(getGalleryPermissionStatus, NULL),
        MAP_FUNCTION(openSettings, NULL),
        MAP_FUNCTION(internalGetChosenPhotoBitmapData, NULL),
        MAP_FUNCTION(internalGetChosenPhotoByteArray, NULL),
        MAP_FUNCTION(internalRemoveStoredImage, NULL),
    };
    
    *numFunctionsToTest = sizeof(functions) / sizeof(FRENamedFunction);
    *functionsToSet = functions;
    
}

void AirImagePickerContextFinalizer(FREContext ctx) {
    CFTypeRef controller;
    FREGetContextNativeData(ctx, (void **)&controller);
    CFBridgingRelease(controller);
}

void AirImagePickerInitializer(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet ) {
    *extDataToSet = NULL;
    *ctxInitializerToSet = &AirImagePickerContextInitializer;
    *ctxFinalizerToSet = &AirImagePickerContextFinalizer;
}

void AirImagePickerFinalizer(void *extData) {}
