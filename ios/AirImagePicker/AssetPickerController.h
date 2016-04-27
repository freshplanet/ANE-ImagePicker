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

@protocol AssetPickerControllerDelegate <NSObject>
  - (void)assetPickerController:(id)picker didPickMediaWithURL:(NSURL *)url;
  - (void)assetPickerControllerDidFinish:(id)picker;
@end

@interface AssetPickerController : UINavigationController <ProgressTarget> {

  // a view to show the progress of assets being processed
  ProgressController *progressVC;
  // the number of asset bytes to be copied
  NSUInteger totalBytes;
  // the number of asset bytes copied so far
  NSUInteger processedBytes;
}

// the progress on the current asset being processed
@property (nonatomic, readonly) float progress;

// cancel the request
- (void)cancel;
// respond to a list of assets being selected
- (void)didSelectAssets:(NSArray *)assets;

@end
