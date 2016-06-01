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

// This class implements a table view controller where all the 
//  cells are pre-loaded. It is designed for convenience, not
//  performance, so it should only be used to create small tables.
@interface PrefabTableViewController : UITableViewController {

  // an array of sections, each item of which is an array of cells
  NSMutableArray *sections;
  // an array of section titles
  NSMutableArray *sectionTitles;
  // whether a reload is required
  bool needsReload;

}

// allow the subclass to build its cells on instantiation
- (void)setup;
// allow the subclass to easily respond to row selection
- (void)didSelectRowAtIndexPath:(NSIndexPath *)indexPath;
// reload any dynamic cells that might not be sized properly
- (void)reloadIfNeeded;
- (void)setNeedsReload;

@end
