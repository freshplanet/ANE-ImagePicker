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

#import "NSURL+Rewriters.h"

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
@synthesize progress = _progress;

- (id)init {
  // show the user's photo albums
  AlbumListController *albums = [[[AlbumListController alloc] 
    initWithStyle:UITableViewStylePlain]
      autorelease];
  // put them in a navigation controller
  if ((self = [super initWithRootViewController:albums])) {
    _progress = 0.0;
  }
  return(self);
}
- (void)dealloc {
  if (progressVC) {
    [progressVC release], progressVC = nil;
  }
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
  // show a view to let the user know assets are being processed
  if (progressVC == nil) {
    progressVC = [[ProgressController alloc]
      initWithTaskNamed:@"Processing Media"
      progressTarget:self];
    [self presentModalViewController:progressVC animated:NO];
  }
  // get the total size of all assets
  totalBytes = 0;
  for (id asset in assets) {
    totalBytes += [[asset defaultRepresentation] size];
  }
  processedBytes = 0;
  // process assets on another thread
  dispatch_queue_t thread = dispatch_queue_create("asset copy", NULL);
  dispatch_async(thread, ^{
    for (id asset in assets) {
      // process the next asset
      [self processAsset:(ALAsset *)asset];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
      [self assetProcessingDidFinish];
    });
  });
  dispatch_release(thread);
}

// process a single asset from the pending list
- (void)processAsset:(ALAsset *)libraryAsset {
  // get the main representation of the media
  ALAssetRepresentation *rep = [libraryAsset defaultRepresentation];
  // get a temp file location to move the data to
  NSURL *toURL = [NSURL tempFileURLWithPrefix:@"airImagePicker" extension:@"tmp"];
  // copy the asset data to the temp file in chunks
  long long offset = 0;
  NSUInteger size = [rep size];
  NSUInteger bytesRead = 0;
  uint8_t buffer[16 * 1024]; // 16K data buffer
  NSError *error = nil;
  // create the temp file (or we can't open for writing below)
  if (! [[NSFileManager defaultManager] fileExistsAtPath:[toURL path]]) {
    BOOL created = [[NSFileManager defaultManager] 
                      createFileAtPath:[toURL path] 
                      contents:nil attributes:nil];
    if (! created) {
      NSLog(@"AirImagePicker:  Error creating temp file");
      return;
    }
  }
  // open the file for writing
  NSFileHandle *fileHandle = 
    [NSFileHandle fileHandleForWritingToURL:toURL error:&error];
  if (fileHandle == nil) {
    NSLog(@"AirImagePicker:  Error opening temp file for writing: %@", 
            [error description]);
    return;
  }
  NSInteger fd = [fileHandle fileDescriptor];
  do {
      // read a chunk
      bytesRead = [rep getBytes:buffer fromOffset:offset length:sizeof(buffer) 
                   error:&error];
      if (error != nil) {
        NSLog(@"AirImagePicker:  Error writing to temp file: %@", 
            [error description]);
        return;
      }
      offset += bytesRead;
      processedBytes += bytesRead;
      // write a chunk
      write(fd, buffer, bytesRead);
      // update progress
      _progress = (float)processedBytes / (float)totalBytes;
  }
  while (offset < [rep size]);
  // make sure the file is completely written to disk
  [fileHandle synchronizeFile];
  [fileHandle closeFile];
  // return the URL to the client
  dispatch_async(dispatch_get_main_queue(), ^{
    [_assetPickerDelegate assetPickerController:self didPickMediaWithURL:toURL];
  });
}

- (void)assetProcessingDidFinish {
  [_assetPickerDelegate assetPickerControllerDidFinish:self];
}

@end
