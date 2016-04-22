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

#import <UIKit/UIKit.h>

#import "ProgressController.h"

typedef enum {
  AssetPickerTypeAlbums
} AssetPickerType;

@interface AssetPickerController : UINavigationController {
  
  // the status bar style before the picker was shown
  /* UIStatusBarStyle oldStatusBarStyle; */
  
  // a queue of assets left to process
  NSMutableArray *assetsToProcess;
  // the original number of assets to process
  NSUInteger assetCount;
  
  // a view to show the progress of asset processing
  ProgressController *progressVC;
}

- (id)initWithType:(AssetPickerType)type;

// cancel the request
- (void)cancel;
// respond to a list of assets being selected
- (void)didSelectAssets:(NSArray *)assets;

@end
