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

#import "AlbumRowCell.h"
#import "NSString+Formatters.h"
#import "NSObject+Notifications.h"
#import "UIDevice+Detect.h"

NSString *AlbumRowSelectionDidChangeNotification =  
  @"AlbumRowSelectionDidChangeNotification";

@implementation AlbumRowCell

@synthesize group, indices, selectedIndices;
@synthesize thumbnailWidth, thumbnailSpacing;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
  if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
    // handle taps on the cell
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc]
      initWithTarget:self action:@selector(handleTap:)];
    [self addGestureRecognizer:tap];
    [tap release];
  }
  return(self);
}
- (void)dealloc {
  [group release], group = nil;
  [indices release], indices = nil;
  [super dealloc];
}

- (void)setGroup:(ALAssetsGroup *)newGroup {
  if (newGroup != group) {
    [group release];
    group = [newGroup retain];
    [contentView setNeedsDisplay];
  }
}
- (void)setIndices:(NSIndexSet *)newIndices {
  if (newIndices != indices) {
    [indices release];
    indices = [newIndices retain];
    [contentView setNeedsDisplay];
  }
}
- (void)setSelectedIndices:(NSMutableSet *)newSet {
  if (newSet != selectedIndices) {
    [selectedIndices release];
    selectedIndices = [newSet retain];
    [contentView setNeedsDisplay];
  }
}

- (CGImageRef)selectedMarkImage {
  return([UIImage imageNamed:@"CellSelected"].CGImage);
}

- (void)drawContentView:(CGRect)r {
  // make sure we have a group and assets to draw
  if ((! group) || (! indices)) return;
  // get the context to draw into
  CGContextRef context = UIGraphicsGetCurrentContext();
  // fill with white
  CGContextSetFillColorWithColor(context, [UIColor whiteColor].CGColor);
  CGContextFillRect(context, r);
  // leave space at the top and left
  CGContextTranslateCTM(context, thumbnailSpacing, thumbnailSpacing);
  // draw thumbnails
  [group enumerateAssetsAtIndexes:indices 
    options:0 
    usingBlock:^(ALAsset *asset, NSUInteger index, BOOL *stop) {
      if (index != NSNotFound) {
        // get the current screen scale
        CGFloat scale = [UIScreen mainScreen].scale;
        // flip the image vertically (it's using UIKit-style coordinates)
        CGContextSaveGState(context);
        CGContextScaleCTM(context, 1.0, - 1.0);
        CGContextTranslateCTM(context, 0.0, - thumbnailWidth);
        // get the location of the thumbnail
        CGRect thumbRect = 
          CGRectMake(0.0, 0.0, thumbnailWidth, thumbnailWidth);
        // draw the thumbnail
        CGContextDrawImage(context, thumbRect, [asset thumbnail]);
        CGContextRestoreGState(context);
        // if the asset is a video, show some info about it
        if ([asset valueForProperty:ALAssetPropertyType] == ALAssetTypeVideo) {
          // make a dark strip at the bottom
          CGFloat infoHeight = 18.0;
          CGRect infoRect = CGRectMake(
            0.0, thumbnailWidth - infoHeight, 
            thumbnailWidth, infoHeight);
          CGContextSetFillColorWithColor(context, 
            [UIColor colorWithWhite:0.0 alpha:0.5].CGColor);
          CGContextFillRect(context, infoRect);
          // make a light one-pixel separator line above it
          CGRect borderRect = infoRect;
          borderRect.size.height = 1.0 / scale;
          CGContextSetFillColorWithColor(context, 
            [UIColor colorWithWhite:1.0 alpha:0.25].CGColor);
          CGContextFillRect(context, borderRect);
          // draw a video icon on the left
          CGImageRef videoIcon = [UIImage imageNamed:@"AssetVideo"].CGImage;
          CGRect iconRect = infoRect;
          iconRect.size.width = iconRect.size.height;
          CGContextDrawImage(context, iconRect, videoIcon);
          // get the video duration
          NSString *duration = [NSString stringWithSeconds:
            [[asset valueForProperty:ALAssetPropertyDuration] doubleValue]
            style:TimeDisplayStyleColonSeparated];
          // show it
          CGContextSetFillColorWithColor(context, 
            [UIColor whiteColor].CGColor);
          CGRect durationRect = infoRect;
          durationRect.origin.x += iconRect.size.width;
          durationRect.size.width -= iconRect.size.width;
          durationRect = CGRectInset(durationRect, 5.0, 1.5);
          [duration drawInRect:durationRect 
            withFont:[UIFont boldSystemFontOfSize:12.0]
            lineBreakMode:NSLineBreakByClipping
            alignment:NSTextAlignmentRight];
        }
        // see if the thumbnail is selected
        if ([selectedIndices containsObject:
              [NSNumber numberWithInteger:index]]) {
          CGContextSaveGState(context);
          // draw a white overlay on the image
          CGContextSetFillColorWithColor(context, 
            [UIColor colorWithRed:1.0 green:1.0 blue:1.0 alpha:
              [UIDevice hasFlatInterface] ? 0.25 : 0.66].CGColor);
          CGContextFillRect(context, thumbRect);
          // draw an icon to show the thumbnail is selected
          CGImageRef mark = [self selectedMarkImage];
          CGFloat markWidth = CGImageGetWidth(mark) / scale;
          CGFloat markHeight = CGImageGetHeight(mark) / scale;
          CGContextScaleCTM(context, 1.0, - 1.0);
          CGContextTranslateCTM(context, 
            thumbnailWidth - markWidth, - thumbnailWidth);
          CGRect markRect = CGRectMake(0.0, 0.0, markWidth, markHeight);
          CGContextDrawImage(context, markRect, mark);
          CGContextRestoreGState(context);
        }
        // move to the next location
        CGContextTranslateCTM(context, thumbnailWidth + thumbnailSpacing, 0.0);
      }
    }];
}

// handle taps on the row
- (void)handleTap:(UITapGestureRecognizer *)gesture {
  // get the location of the tap as an asset index
  CGPoint p = [gesture locationInView:self];
  NSUInteger index = floor((p.x - thumbnailSpacing) / 
    (thumbnailWidth + thumbnailSpacing));
  index += [indices firstIndex];
  // make sure we have an asset for that index
  if (! ([indices containsIndex:index])) return;
  // see if the asset is already selected
  NSNumber *indexNum = [NSNumber numberWithUnsignedLong:index];
  if ([selectedIndices containsObject:indexNum]) {
    // deselect the asset
    [selectedIndices removeObject:indexNum];
  }
  else {
    // select the asset
    [selectedIndices addObject:indexNum];
  }
  // indicate the selection changed
  [self notifyWithName:AlbumRowSelectionDidChangeNotification];
  // redraw to show the selection change
  [contentView setNeedsDisplay];
}

@end
