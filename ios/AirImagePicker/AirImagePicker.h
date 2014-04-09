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

#import "FlashRuntimeExtensions.h"

@interface AirImagePicker : NSObject <UINavigationControllerDelegate, UIImagePickerControllerDelegate, UIPopoverControllerDelegate>

//@property (nonatomic, retain) UIImagePickerController *imagePicker;
//@property (nonatomic, retain) UIPopoverController *popover;
//@property (nonatomic, retain) UIImage *pickedImage;
//@property (nonatomic, retain) NSData *pickedImageJPEGData;
@property (nonatomic, strong) NSString *videoPath;
@property (nonatomic, strong) NSString *imagePath;

+ (id)sharedInstance;

+ (void)log:(NSString *)message;
+ (void)status:(NSString*)code level:(NSString*)level;

//- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType allowVideo:(BOOL)allowVideo crop:(BOOL)crop albumName:(NSString*)albumName anchor:(CGRect)anchor maxDimensions:(CGSize)maxDimensions;
//
//- (void) onImagePickedWithOriginalImage:(UIImage*)originalImage editedImage:(UIImage*)editedImage;
//- (void) onVideoPickedWithMediaURL:(NSURL*)mediaURL;
//
//- (void) exportToMP4:(NSURL *)originalMediaURL withExportURL:(NSURL*)exportUrl;
//- (void) onVideoExported:(NSURL*)mediaURL;
//
//- (void)displayOverlay:(UIImage *)overlay;
//- (void)removeOverlay;

@end


// C API
DEFINE_ANE_FUNCTION(isImagePickerAvailable);
DEFINE_ANE_FUNCTION(displayImagePicker);
DEFINE_ANE_FUNCTION(isCameraAvailable);
DEFINE_ANE_FUNCTION(displayCamera);

//DEFINE_ANE_FUNCTION(setCameraFlashMode);

//DEFINE_ANE_FUNCTION(getPickedImageJPEGRepresentationSize);
//DEFINE_ANE_FUNCTION(copyPickedImageJPEGRepresentationToByteArray);

DEFINE_ANE_FUNCTION(displayOverlay);
DEFINE_ANE_FUNCTION(removeOverlay);

//image
DEFINE_ANE_FUNCTION(getImagePath);
DEFINE_ANE_FUNCTION(getPickedImageWidth);
DEFINE_ANE_FUNCTION(getPickedImageHeight);
DEFINE_ANE_FUNCTION(drawPickedImageToBitmapData);

//video
DEFINE_ANE_FUNCTION(getVideoPath);

// video thumnail
//DEFINE_ANE_FUNCTION(getImagePath);
//DEFINE_ANE_FUNCTION(getPickedImageWidth);
//DEFINE_ANE_FUNCTION(getPickedImageHeight);
//DEFINE_ANE_FUNCTION(drawPickedImageToBitmapData);
DEFINE_ANE_FUNCTION(cleanUpTemporaryDirectoryContent);
DEFINE_ANE_FUNCTION(isCropAvailable);


// ANE Setup
void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet);
void AirImagePickerContextFinalizer(FREContext ctx);
void AirImagePickerInitializer(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet);
void AirImagePickerFinalizer(void *extData);