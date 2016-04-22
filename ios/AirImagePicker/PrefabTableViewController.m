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

#import "PrefabTableViewController.h"

@implementation PrefabTableViewController

// SETUP **********************************************************************
#pragma mark -
#pragma mark Setup

- (id)init {
  return([self initWithStyle:UITableViewStyleGrouped]);
}
- (id)initWithStyle:(UITableViewStyle)style {
  if ((self = [super initWithStyle:style])) {
    // set up the cell storage
    sections = [[NSMutableArray alloc] init];
    sectionTitles = [[NSMutableArray alloc] init];
    [self setup];
  }
  return(self);
}
- (void)dealloc {
  [NSObject cancelPreviousPerformRequestsWithTarget:self];
  [sections release];
  [sectionTitles release];
  // manually clear weak references to this object
  //  (UIKit seems not to do it for some reason)
  self.tableView.delegate = nil;
  self.tableView.dataSource = nil;
  [super dealloc];
}

// this should be subclassed to build 
//  the lists of sections and cells
- (void)setup { }

// DATA SOURCE ****************************************************************
#pragma mark -
#pragma mark Data Source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
  return(MAX(sections.count, sectionTitles.count));
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
  if ((section >= 0) && (section < sections.count)) {
    NSArray *cells = [sections objectAtIndex:section];
    return(cells.count);
  }
  else {
    return(0);
  }
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
  // if the section is in range, return its title
  if ((section >= 0) && (section < sectionTitles.count)) {
    return([sectionTitles objectAtIndex:section]);
  }
  else {
    return(nil);
  }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  // get the array of cells for the section
  NSArray *cells = [sections objectAtIndex:indexPath.section];
  // get the cell
  UITableViewCell *cell = [cells objectAtIndex:indexPath.row];
  return(cell);
}

// INTERACTION ****************************************************************
#pragma mark -
#pragma mark Interaction

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  [self didSelectRowAtIndexPath:indexPath];
}

// this should be subclassed if the table is active
- (void)didSelectRowAtIndexPath:(NSIndexPath *)indexPath { }

// DYNAMIC ROW HEIGHT *********************************************************
#pragma mark -
#pragma mark Dynamic Row Height

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  // return cell heights
  UITableViewCell *cell = [self tableView:tableView cellForRowAtIndexPath:indexPath];
  // if the cell defines a custom height, use it
  if ([cell conformsToProtocol:@protocol(DynamicHeightCell)]) {
    UITableViewCell<DynamicHeightCell> *dCell = 
      (UITableViewCell<DynamicHeightCell> *)cell;
    return([dCell rowHeight]);
  }
  // otherwise fall back on the default row height
  else {
    return(tableView.rowHeight);
  }
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  [self reloadDynamicCellsIfNeeded];
}

// check that dynamic cells are properly sized
- (void)reloadDynamicCellsIfNeeded {
  // assume we will not need to reload
  BOOL needsReload = NO;
  // check all cells
  for (NSArray *section in sections) {
    for (UITableViewCell *cell in section) {
      // look for dynamic cells
      if ([cell conformsToProtocol:@protocol(DynamicHeightCell)]) {
        UITableViewCell<DynamicHeightCell> *dCell = 
          (UITableViewCell<DynamicHeightCell> *)cell;
        // if any cells need to be reloaded, reload everything
        if ([dCell needsReload]) {
          needsReload = YES;
          break;
        }
      }
    }
  }
  // reload if needed (with a small delay because otherwise 
  //  UITableView sometimes won't do it)
  if (needsReload) {
    [self.tableView performSelector:@selector(reloadData)
      withObject:nil afterDelay:0.1];
  }
}

@end

