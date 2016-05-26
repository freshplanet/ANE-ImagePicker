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

#import "AlbumController.h"

#import "NSObject+Notifications.h"
#import "UIDevice+Detect.h"

#import "AssetPickerController.h"
#import "AlbumRowCell.h"

// private methods
@interface AlbumController()

- (void)selectionDidChange:(NSNotification *)notification;

- (void)didFinish;
- (void)didFinishExtended;

- (CGFloat)thumbnailWidth;
- (CGFloat)thumbnailSpacing;
- (NSUInteger)numberOfAssets;
- (NSUInteger)thumbnailsPerRow;

@end

@implementation AlbumController

- (id)initWithGroup:(id)inGroup {
  if ((self = [super initWithNibName:nil bundle:nil])) {
    // make a set of selected asset indices
    selectedIndices = [[NSMutableSet alloc] init];
    // listen for the selection changing
    [self observeNotificationsNamed:AlbumRowSelectionDidChangeNotification 
      selector:@selector(selectionDidChange:)];
    // set up the nav bar
    if ([inGroup isKindOfClass:[ALAssetsGroup class]]) {
      group = [inGroup retain];
      self.title = [group valueForProperty:ALAssetsGroupPropertyName];
    }
    else if ([inGroup isKindOfClass:[PHAssetCollection class]]) {
      self.title = [inGroup localizedTitle];
      group = [[PHAsset fetchAssetsInAssetCollection:inGroup options:nil] retain];
    }
    // make a button for the user to finish
    doneButton = [[UIBarButtonItem alloc] 
        initWithBarButtonSystemItem:UIBarButtonSystemItemDone 
        target:self action:@selector(didFinish)];
  }
  return(self);
}
- (void)dealloc {
  [self stopObservingNotifications];
  [group release], group = nil;
  [selectedIndices release], selectedIndices = nil;
  [doneButton release], doneButton = nil;
  [super dealloc];
}

// LAYOUT *********************************************************************

- (CGFloat)thumbnailWidth {
  // allow for larger thumbnails on iPad
  return([UIDevice hasLargeScreen] ? 144.0 : 75.0);
}
- (CGFloat)thumbnailSpacing {
  // leave more space on iPad
  return([UIDevice hasLargeScreen] ? 8.0 : 4.0);
}

- (NSUInteger)thumbnailsPerRow {
  return(floor((self.tableView.bounds.size.width + [self thumbnailSpacing]) / 
    ([self thumbnailWidth] + [self thumbnailSpacing])));
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  return([self thumbnailWidth] + [self thumbnailSpacing]);
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  // don't draw separators between cells
  self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
  // leave some space at the bottom
  self.tableView.contentInset = UIEdgeInsetsMake(
    self.tableView.contentInset.top, 
    self.tableView.contentInset.right, 
    [self thumbnailSpacing], 
    self.tableView.contentInset.left);
  // see what type of album we're showing
  if ([group isKindOfClass:[ALAssetsGroup class]]) {
    NSNumber *typeNum = [group valueForProperty:ALAssetsGroupPropertyType];
    if (typeNum) {
      NSInteger type = [typeNum integerValue];
      // if it's the camera roll, scroll to the bottom so that the newest
      //  content is the most easily accessible
      if (type & ALAssetsGroupSavedPhotos) {
        NSUInteger rows = 
          [self tableView:self.tableView numberOfRowsInSection:0];
        if (rows > 0) {
          [self.tableView 
            scrollToRowAtIndexPath:
              [NSIndexPath indexPathForRow:(rows - 1) inSection:0]
            atScrollPosition:UITableViewScrollPositionBottom animated:NO];
        }
      }
    }
  }
}
- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  // always use the iPhone screen size 
  //  (on iPad it will be inside a popover)
  [self setContentSizeForViewInPopover:CGSizeMake(320.0, 480.0)];
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
  // re-display the table for the new orientation
  [self.tableView reloadData];
}

- (void)selectionDidChange:(NSNotification *)notification {
  // only show the done button if there are assets selected
  [self.navigationItem setRightBarButtonItem:
      (selectedIndices.count > 0 ? doneButton : nil)
    animated:YES];
}

- (NSUInteger)numberOfAssets {
  if ([group isKindOfClass:[ALAssetsGroup class]]) {
    return([group numberOfAssets]);
  }
  else if ([group isKindOfClass:[PHFetchResult class]]) {
    return([group count]);
  }
  return(0);
}

// UITableViewDataSource ******************************************************
#pragma mark - UITableViewDataSource

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
  // there is one section for the whole album
  return(1);
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
  // group the thumbnails into rows
  return((NSInteger)ceil((double)[self numberOfAssets] / 
                         (double)[self thumbnailsPerRow]));
}

- (UITableViewCell *)tableView:(UITableView *)tableView 
                     cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  // get the index of the first and last thumbnail on this row
  NSUInteger columns = [self thumbnailsPerRow];
  NSUInteger firstIndex = indexPath.row * columns;
  NSUInteger lastIndex = MIN(firstIndex + columns, [self numberOfAssets]) - 1;
  // set up a reusable cell to represent this row
  static NSString *AlbumRowCellIdentifier = @"AlbumRowCell";
  AlbumRowCell *cell = (AlbumRowCell *)[self.tableView 
    dequeueReusableCellWithIdentifier:AlbumRowCellIdentifier];
  if (cell == nil) {
    cell = [[[AlbumRowCell alloc] 
      initWithStyle:UITableViewCellStyleDefault
      reuseIdentifier:AlbumRowCellIdentifier]
        autorelease];
    // the row itself can't be selected, even if its thumbnails are
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    // set up the cell's layout
    cell.thumbnailWidth = [self thumbnailWidth];
    cell.thumbnailSpacing = [self thumbnailSpacing];
  }
  // link the cell to the central store of which assets are selected
  cell.selectedIndices = selectedIndices;
  // populate the cell with thumbnails
  cell.group = group;
  cell.indices = [NSIndexSet 
    indexSetWithIndexesInRange:
      NSMakeRange(firstIndex, lastIndex - firstIndex + 1)];
  return(cell);
}

- (void)didFinish {
  // show a waiting state
  /* [self showNavigationSpinner]; */
  [self performSelector:@selector(didFinishExtended)
        withObject:nil afterDelay:0.1];
}
- (void)didFinishExtended {
  // see if any thumbnails were selected
  if (selectedIndices.count > 0) {
    // get a list of selected assets
    NSMutableArray *selectedAssets = 
      [[NSMutableArray alloc] initWithCapacity:selectedIndices.count];
    // iterate based on the library we're using
    if ([group isKindOfClass:[ALAssetsGroup class]]) {
      [group enumerateAssetsUsingBlock:
        ^(ALAsset *asset, NSUInteger index, BOOL *stop) {
          if ([selectedIndices containsObject:
              [NSNumber numberWithInteger:index]]) {
            [selectedAssets addObject:asset];
          }
        }];
    }
    else if ([group isKindOfClass:[PHFetchResult class]]) {
      [group enumerateObjectsUsingBlock:
        ^(PHAsset *asset, NSUInteger index, BOOL *stop) {
          if ([selectedIndices containsObject:
              [NSNumber numberWithInteger:index]]) {
            [selectedAssets addObject:asset];
          }
        }];
    }
    // tell the parent that assets were selected
    [(AssetPickerController *)self.navigationController 
      didSelectAssets:selectedAssets];
    [selectedAssets release];
  }
  // stop the waiting state
  self.navigationItem.titleView = nil;
}

@end
