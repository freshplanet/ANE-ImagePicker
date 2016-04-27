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

#import "AlbumListController.h"

#import "AssetPickerController.h"
#import "AlbumTableCell.h"
#import "AlbumController.h"

// private methods
@interface AlbumListController()

- (void)didCancel;

- (void)addAlbumForGroup:(ALAssetsGroup *)group;

@end

@implementation AlbumListController

- (void)setup {
  // underlap the status bar
  self.wantsFullScreenLayout = YES;
  // set the title
  self.title = @"Albums";
  self.tableView.rowHeight = 65.0;
  // don't show row separators
  self.tableView.separatorStyle = UITableViewCellSeparatorStyleNone;
  // add a button to cancel the picker
  self.navigationItem.leftBarButtonItem =
    [[[UIBarButtonItem alloc] 
      initWithBarButtonSystemItem:UIBarButtonSystemItemCancel 
      target:self action:@selector(didCancel)]
        autorelease];
  // use larger rows
  // make a list for albums
  if (! albumCells) {
    albumCells = [[NSMutableArray alloc] init];
  }
  // remove all albums
  [albumCells removeAllObjects];
  // make a single section with all the albums in it
  [sections removeAllObjects];
  [sections addObject:albumCells];
  // load albums from the asset library
  [library release];
  library = [[ALAssetsLibrary alloc] init];
  [library enumerateGroupsWithTypes:ALAssetsGroupAll 
    usingBlock:^(ALAssetsGroup *group, BOOL *stop) {
      // add groups while we get them
      if (group != nil) {
        [self addAlbumForGroup:group];
      }
      // reload when we're done
      else {
        [self.tableView reloadData];
      }
    } failureBlock:^(NSError *error) {
      NSLog(@"ERROR: Failed to load albums: %@", error);
      // show a message for the user
      UIAlertView *alert = [[[UIAlertView alloc]
        initWithTitle:@"Failed to Load Albums"
        message:@"You may need to go to Settings and turn Location Services on."
        delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil]
          autorelease];
      [alert show];
    }];
}
- (void)dealloc {
  [albumCells release], albumCells = nil;
  [groups release], groups = nil;
  [library release], library = nil;
  [super dealloc];
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  // always use the iPhone screen size 
  //  (on iPad it will be inside a popover)
  [self setContentSizeForViewInPopover:CGSizeMake(320.0, 480.0)];
}

// add an album to the list
- (void)addAlbumForGroup:(ALAssetsGroup *)group {
  // add the group to the groups list
  if (! groups) groups = [[NSMutableArray alloc] init];
  [groups addObject:group];
  // make a cell for the album
  UITableViewCell *albumCell = [[[AlbumTableCell alloc] 
    initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:nil]
      autorelease];
  // get all assets in the album
  [group setAssetsFilter:[ALAssetsFilter allAssets]];
  // set it up
  albumCell.textLabel.text = [group valueForProperty:ALAssetsGroupPropertyName];
  albumCell.detailTextLabel.text = [NSString stringWithFormat:@"(%li)",
    (long)[group numberOfAssets]];
  albumCell.imageView.image = [UIImage imageWithCGImage:[group posterImage]];
  albumCell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
  // add it to the cell list
  [albumCells addObject:albumCell];
}

// respond to album selection
- (void)didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  // see if we have a group for this row
  if ((groups) && (indexPath.row < groups.count)) {
    // show the group
    AlbumController *ac = [[[AlbumController alloc] 
      initWithGroup:[groups objectAtIndex:indexPath.row]]
        autorelease];
    [self.navigationController pushViewController:ac animated:YES];
  }
}

// respond to the cancel button
- (void)didCancel {
  // find the parent navigation controller
  UINavigationController *nav = self.navigationController;
  if ([nav isKindOfClass:[AssetPickerController class]]) {
    [(AssetPickerController *)nav cancel];
  }
}

@end
