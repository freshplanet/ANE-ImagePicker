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

#import "FlashRuntimeExtensions.h"

// whether to always use the native pickers (no multiselect support)
#define FORCE_NATIVE_PICKER NO

#if ! FORCE_NATIVE_PICKER
  #import "AssetPickerController.h"
#endif

@interface AirImagePicker : NSObject <
  #if ! FORCE_NATIVE_PICKER
    AssetPickerControllerDelegate,
  #endif
    UINavigationControllerDelegate, 
    UIImagePickerControllerDelegate, 
    UIPopoverControllerDelegate,
    UIDocumentMenuDelegate,
    UIDocumentPickerDelegate
  > {
 
  UIImagePickerControllerSourceType sourceType;
  CGRect anchor;
  
}

@property (nonatomic, retain) UIViewController *picker;
@property (nonatomic, retain) UIPopoverController *popover;
@property (nonatomic, retain) UIPopoverPresentationController *popoverPresentation;

+ (id)sharedInstance;

+ (void)log:(NSString *)message;

- (void)displayImagePickerWithSourceType:(UIImagePickerControllerSourceType)sourceType 
          allowVideo:(BOOL)allowVideo allowDocument:(BOOL)allowDocument allowMultiple:(BOOL)allowMultiple 
          crop:(BOOL)crop anchor:(CGRect)anchor;

- (void) onImagePickedWithOriginalImage:(UIImage*)originalImage editedImage:(UIImage*)editedImage;
- (void) onVideoPickedWithMediaURL:(NSURL*)mediaURL;

- (void) returnMediaURL:(NSURL*)mediaURL;
- (void) presentPicker;
- (void) dismissPicker;

- (void)documentMenu:(UIDocumentMenuViewController *)documentMenu
          didPickDocumentPicker:(UIDocumentPickerViewController *)documentPicker;
- (void)documentMenuWasCancelled:(UIDocumentMenuViewController *)documentMenu;

- (void)documentPicker:(UIDocumentPickerViewController *)controller
  didPickDocumentAtURL:(NSURL *)url;
- (void)documentPickerWasCancelled:(UIDocumentPickerViewController *)controller;

- (void)displayOverlay:(UIImage *)overlay;
- (void)removeOverlay;

@end


// C API
DEFINE_ANE_FUNCTION(isImagePickerAvailable);
DEFINE_ANE_FUNCTION(displayImagePicker);
DEFINE_ANE_FUNCTION(isCameraAvailable);
DEFINE_ANE_FUNCTION(displayCamera);
DEFINE_ANE_FUNCTION(displayOverlay);
DEFINE_ANE_FUNCTION(removeOverlay);


// ANE Setup
void AirImagePickerContextInitializer(void* extData, const uint8_t* ctxType, FREContext ctx, uint32_t* numFunctionsToTest, const FRENamedFunction** functionsToSet);
void AirImagePickerContextFinalizer(FREContext ctx);
void AirImagePickerInitializer(void** extDataToSet, FREContextInitializer* ctxInitializerToSet, FREContextFinalizer* ctxFinalizerToSet);
void AirImagePickerFinalizer(void *extData);
