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

#import <Foundation/Foundation.h>
#import <AssetsLibrary/AssetsLibrary.h>

#import "PrefabTableViewController.h"

@interface AlbumListController : PrefabTableViewController {
  
  // a list of cells, one for each album
  NSMutableArray *albumCells;
  // a reference to the asset library being accessed
  ALAssetsLibrary *library;
  // a list of asset groups, one for each album
  NSMutableArray *groups;
  
}

@end
