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
#import <AssetsLibrary/AssetsLibrary.h>

#import "FastCell.h"

// a notification sent whenever selection state changes
extern NSString *AlbumRowSelectionDidChangeNotification;

@interface AlbumRowCell : FastCell {
  
  // the asset group to display thumbnails from
  ALAssetsGroup *group;
  // the indices of the thumbnails to display
  NSIndexSet *indices;
  // a set that stores the indices of all selected assets
  NSMutableSet *selectedIndices;
  // the size of thumbnails (width and height)
  CGFloat thumbnailWidth;
  // the space to leave between thumbnails
  CGFloat thumbnailSpacing;
  
}

@property(nonatomic,retain) ALAssetsGroup *group;
@property(nonatomic,retain) NSIndexSet *indices;
@property(nonatomic,retain) NSMutableSet *selectedIndices;
@property(nonatomic) CGFloat thumbnailWidth;
@property(nonatomic) CGFloat thumbnailSpacing;

// a reference to an image to draw on selected cells
- (CGImageRef)selectedMarkImage;

// handle taps
- (void)handleTap:(UITapGestureRecognizer *)gesture;

@end
