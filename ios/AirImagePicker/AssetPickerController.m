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

// process the queued list of assets
- (void)processAssets;
// process the rest of the assets on the queue
- (void)processMoreAssets;
// indicate that all asset processing is finished
- (void)assetProcessingDidFinish;
// process a specific asset and return whether processing was completed
//  synchronously (returning NO indicates that some asynchronous processing
//  was required and that the processing of other assets should be delayed
//  until it completes)
- (BOOL)processAsset:(ALAsset *)libraryAsset;
// get an info dictionary based on the given asset
- (NSMutableDictionary *)infoDictionaryForAsset:(ALAsset *)asset;

@end

@implementation AssetPickerController

- (id)initWithType:(AssetPickerType)type {
  // show the user's photo albums
  AlbumListController *albums = [[[AlbumListController alloc] 
    initWithStyle:UITableViewStylePlain]
      autorelease];
  // put them in a navigation controller
  if ((self = [super initWithRootViewController:albums])) {
    // use a translucent bar style like the photo app
    self.navigationBar.barStyle = UIBarStyleBlack;
    self.navigationBar.translucent = YES;
  }
  return(self);
}
- (void)dealloc {
  [assetsToProcess release], assetsToProcess = nil;
  /* [progressVC release], progressVC = nil; */
  [super dealloc];
}

- (UIStatusBarStyle)preferredStatusBarStyle {
  return(UIStatusBarStyleBlackOpaque);
}

/*
- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  // change the status bar style to match the nav bar on iPhone
  if (! [UIDevice hasLargeScreen]) {
    oldStatusBarStyle = [UIApplication sharedApplication].statusBarStyle;
    [[UIApplication sharedApplication] 
      setStatusBarStyle:UIStatusBarStyleBlackTranslucent
      animated:animated];
  }
}
*/
- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  // always use the iPhone screen size 
  //  (on iPad it will be inside a popover)
  [self setContentSizeForViewInPopover:CGSizeMake(320.0, 480.0)];
}
/*
- (void)viewWillDisappear:(BOOL)animated {
  [super viewWillDisappear:animated];
  // change the status bar style back if we modified it
  if (! [UIDevice hasLargeScreen]) {
    [[UIApplication sharedApplication] 
      setStatusBarStyle:oldStatusBarStyle
      animated:animated];
  }
}
*/

// respond to the picking being cancelled
- (void)cancel {
  [self assetProcessingDidFinish];
}

// respond to a list of assets being selected
- (void)didSelectAssets:(NSArray *)assets {
  // save a list of assets to be processed
  [assetsToProcess release];
  assetsToProcess = [assets mutableCopy];
  // store the original number of assets
  assetCount = [assetsToProcess count];
  [self processAssets];
}

- (void)processAssets {
  // show a progress indicator if it's not already showing
  if (progressVC == nil) {
    progressVC = 
      [[ProgressController alloc]
        initWithTaskNamed:@"Processing Media"
        progressTarget:nil];
    progressVC.modalTransitionStyle = UIModalTransitionStylePartialCurl;
    [self presentModalViewController:progressVC animated:YES];
  }
  // start processing assets after a slight delay to let 
  //  the graphical transition happen
  [self performSelector:@selector(processMoreAssets) 
    withObject:nil afterDelay:0.25];
}

// process the pending list of assets
- (void)processMoreAssets {
  // if there are no assets left to process, we're done
  if ((assetsToProcess == nil) || (! ([assetsToProcess count] > 0))) {
    [self assetProcessingDidFinish];
    return;
  }
  // process the next asset
  while ([assetsToProcess count] > 0) {
    // get the next asset to process
    ALAsset *asset = [assetsToProcess objectAtIndex:0];
    // process it, using a local autorelease pool to prevent a pileup
    //  of memory as each asset is processed
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    BOOL completed = [self processAsset:asset];
    [pool release];
    // if processing completed already, remove the asset from the queue
    //  and continue to the next
    if (completed) {
      [assetsToProcess removeObjectAtIndex:0];
    }
    // if processing is deferred, return and wait for it to complete
    //  before continuing
    else {
      return;
    }
  }
  // if we get here, all assets have been processed and we're done
  [self assetProcessingDidFinish];
}

// process a single asset from the pending list
- (BOOL)processAsset:(ALAsset *)libraryAsset {
  /*
  // show which asset we're on

  if (progressVC) {
    NSInteger currentAsset = 
      MIN(MAX(1, (assetCount - [assetsToProcess count]) + 1), assetCount);
    progressVC.taskName = [NSString stringWithFormat:
      @"Processing Media (%li of %lu)", (long)currentAsset, (unsigned long)assetCount];
  }
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
    // scale the image down if it's really big
    CGSize maxSize = CGSizeMake(1280.0, 1280.0);
    if ((fullImage.size.width > maxSize.width) ||
        (fullImage.size.height > maxSize.height)) {
      fullImage = [fullImage imageFittingInSize:maxSize cropped:NO];
    }
    // package the image data
    NSMutableDictionary *info = [self infoDictionaryForAsset:libraryAsset];
    [info setObject:fullImage forKey:MediaSourceImageKey];
    // send the image to observers
    [self notifyWithName:MediaSourceDidProduceMediaNotification 
          userInfo:info];
    // we have finished synchronously
    return(YES);
  }
  // handle videos
  else if (type == ALAssetTypeVideo) {
    NSURL *inputURL = [NSURL URLWithString:
      [NSString stringWithFormat:@"%@", [rep url]]];
    // get a destination URL for the video
    NSURL *outputURL = nil; //!!! [NSURL tempFileURLWithPrefix:@"asset" extension:@"mov"];
    // get the source video as an asset
    AVAsset *avAsset = [AVAsset assetWithURL:inputURL];
    // on a cellular network, compress the video more
    NSString *qualityPreset = AVAssetExportPresetMediumQuality;
    if ([[VoiceThread server] connectivity] == ConnectivityCellular)
      qualityPreset = AVAssetExportPresetLowQuality;
    // see if the preferred preset is available
    NSArray *compatiblePresets = 
      [AVAssetExportSession exportPresetsCompatibleWithAsset:avAsset];
    if ([compatiblePresets containsObject:qualityPreset]) {
      // start an export session for the asset
      AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]
            initWithAsset:avAsset presetName:qualityPreset];
      // set the target
      exportSession.outputURL = outputURL;
      exportSession.outputFileType = AVFileTypeQuickTimeMovie;
      // show progress
      progressVC.progressTarget = exportSession;
      // run the export
      [exportSession exportAsynchronouslyWithCompletionHandler:^{
        if ([exportSession status] == AVAssetExportSessionStatusFailed) {
          NSLog(@"ERROR: Export failed: %@", exportSession.error);
        }
        else if ([exportSession status] == AVAssetExportSessionStatusCancelled) {
          NSLog(@"ERROR: Export cancelled.");
        }
        else {
          // report the processed asset on the main thread
          dispatch_async(dispatch_get_main_queue(), ^{
            NSMutableDictionary *info = 
              [self infoDictionaryForAsset:libraryAsset];
            [info setObject:outputURL forKey:MediaSourceURLKey];
            [self notifyWithName:MediaSourceDidProduceMediaNotification 
              userInfo:info];
          });
        }
        [exportSession release];
        // stop showing progress
        progressVC.progressTarget = nil;
        // take the asset off the processing queue, 
        //  since it has been processed (or failed)
        [assetsToProcess removeObjectIdenticalTo:libraryAsset];
        // continue processing the remaining assets on the main thread
        [self performSelectorOnMainThread:
          @selector(processMoreAssets) 
          withObject:nil waitUntilDone:NO];
      }];
      // tell the caller to wait for the asynchronous process to finish
      return(NO);
    }
    // if the preferred preset isn't available, use the original data
    else {
      // make a buffer to load data from the file
      NSMutableData *data = [[NSMutableData alloc] initWithLength:[rep size]];
      // load asset data
      NSError *error = nil;
      [rep getBytes:data.mutableBytes 
        fromOffset:0
        length:[rep size]
        error:&error];
      if (error) {
        NSLog(@"ERROR: Failed to extract data from an asset: %@", error);
        [data release];
        return(YES);
      }
      // save the data to it
      error = nil;
      [data writeToURL:outputURL options:0 error:&error];
      if (error) {
        NSLog(@"ERROR: Failed to write data from an asset: %@", error);
        [data release];
        return(YES);
      }
      [data release];
      // package the video
      NSMutableDictionary *info = [self infoDictionaryForAsset:libraryAsset];
      [info setObject:outputURL forKey:MediaSourceURLKey];
      [self notifyWithName:MediaSourceDidProduceMediaNotification 
            userInfo:info];
      // processing was finished synchronously
      return(YES);
    }
  }
  else {
    NSLog(@"WARNING: Unrecognized media type for asset %@: %@", 
      libraryAsset, type);
    return(YES);
  }
  */
  //!!!
  return(YES);
}

// get a basic info dictionary for an asset
- (NSMutableDictionary *)infoDictionaryForAsset:(ALAsset *)asset {
  // make a dictionary
  NSMutableDictionary *info = [[NSMutableDictionary alloc] init];
  /*
  // set the media type
  NSString *mediaType = nil;
  NSString *type = [asset valueForProperty:ALAssetPropertyType];
  if (type == ALAssetTypePhoto) mediaType = MediaSourceTypeImage;
  else if (type == ALAssetTypeVideo) mediaType = MediaSourceTypeVideo;
  // make sure we recognize the type of media
  if (! mediaType) {
    NSLog(@"WARNING: Unrecognized media type for asset: %@", asset);
    mediaType = @"";
  }
  [info setObject:mediaType forKey:MediaSourceMediaTypeKey];
  // send the asset's thumbnail (for a low-memory preview)
  [info setObject:[UIImage imageWithCGImage:[asset thumbnail]]
        forKey:MediaSourceThumbnailImageKey];
  // send a fullscreen representation of the image for the zoom-in feature
  //   and fast page previews
  [info setObject:
    [UIImage imageWithCGImage:
        [[asset defaultRepresentation] fullScreenImage]
      scale:1.0 orientation:
        [[asset valueForProperty:ALAssetPropertyOrientation] intValue]]
    forKey:MediaSourceFullscreenImageKey];
  // add duration for videos
  if (type == MediaSourceTypeVideo)
    [info setObject:[asset valueForProperty:ALAssetPropertyDuration] 
          forKey:MediaSourceDurationKey];
  */
  // return the initialized dictionary
  return([info autorelease]);
}

- (void)assetProcessingDidFinish {
  // clear the queue of assets
  [assetsToProcess release], assetsToProcess = nil;
  // dismiss the progress view
  if (progressVC) {
    [progressVC dismissModalViewControllerAnimated:YES];
    [progressVC release], progressVC = nil;
  }
  // indicate that we're finished (after a delay so the modal VC
  //  dismissals don't collide and cause a crash)
  /*
  [self performSelector:@selector(notifyWithName:) 
    withObject:MediaSourceDidFinishNotification afterDelay:0.5];
  */
}

@end
