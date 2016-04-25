//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2011-2016 VoiceThread (https://voicethread.com/)
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

#import <MobileCoreServices/MobileCoreServices.h>

#import "AssetPickerController.h"
#import "AlbumListController.h"

// private methods
@interface AssetPickerController()

// process a specific asset and return whether processing was completed
//  synchronously (returning NO indicates that some asynchronous processing
//  was required and that the processing of other assets should be delayed
//  until it completes)
- (void)processAsset:(ALAsset *)libraryAsset;
// indicate that all asset processing is finished
- (void)assetProcessingDidFinish;

@end

@implementation AssetPickerController

@synthesize assetPickerDelegate = _assetPickerDelegate;

- (id)init {
  // show the user's photo albums
  AlbumListController *albums = [[[AlbumListController alloc] 
    initWithStyle:UITableViewStylePlain]
      autorelease];
  // put them in a navigation controller
  if ((self = [super initWithRootViewController:albums])) {
    // additional customization goes here
  }
  return(self);
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  // always use the iPhone screen size 
  //  (on iPad it will be inside a popover)
  [self setContentSizeForViewInPopover:CGSizeMake(320.0, 480.0)];
}

// respond to the picking being cancelled
- (void)cancel {
  [self assetProcessingDidFinish];
}

// respond to a list of assets being selected
- (void)didSelectAssets:(NSArray *)assets {
  for (id asset in assets) {
    [self processAsset:(ALAsset *)asset];
  }
  [self assetProcessingDidFinish];
}

// process a single asset from the pending list
- (void)processAsset:(ALAsset *)libraryAsset {
  // get the main representation of the media
  ALAssetRepresentation *rep = [libraryAsset defaultRepresentation];
  // get the orientation of the media
  UIImageOrientation orientation = 
    [[libraryAsset valueForProperty:ALAssetPropertyOrientation] intValue];
  // see what type of media has been selected
  NSString *type = [libraryAsset valueForProperty:ALAssetPropertyType];
  // handle images
  if (type == ALAssetTypePhoto) {
    // make it a UIImage
    UIImage *fullImage = [UIImage imageWithCGImage:[rep fullResolutionImage] 
      scale:1.0 orientation:orientation];
    // return the image to the delegate
    [_assetPickerDelegate assetPickerController:self didPickImage:fullImage];
  }
  // handle videos
  else if (type == ALAssetTypeVideo) {
    NSURL *videoURL = [NSURL URLWithString:
      [NSString stringWithFormat:@"%@", [rep url]]];
    [_assetPickerDelegate assetPickerController:self 
      didPickVideoWithURL:videoURL];  
  }
  else {
    NSLog(@"WARNING: Unrecognized media type for asset %@: %@", 
      libraryAsset, type);
  }
}

- (void)assetProcessingDidFinish {
  [_assetPickerDelegate assetPickerControllerDidFinish:self];
}

@end
