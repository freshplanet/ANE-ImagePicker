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

#import "AlbumTableCell.h"


@implementation AlbumTableCell

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
  if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
    
    // do style setup
    self.textLabel.font = [UIFont boldSystemFontOfSize:17.0];
    self.detailTextLabel.font = [UIFont systemFontOfSize:self.textLabel.font.pointSize];
    
  }
  return(self);
}

- (void)layoutSubviews {
  [super layoutSubviews];
  
  // get the space to leave between elements
  CGFloat spacing = 10.0;
  // get the width available for all the text
  CGFloat availableWidth = self.contentView.bounds.size.width - 
    self.imageView.frame.size.width - spacing;
  // get the width of the main text
  CGSize textSize = 
    [self.textLabel.text sizeWithFont:self.textLabel.font];
  // get the width of the detail text
  CGSize detailSize = 
    [self.detailTextLabel.text sizeWithFont:self.detailTextLabel.font];
  
  // show as much of the text as we can
  self.textLabel.frame = CGRectMake(
    CGRectGetMaxX(self.imageView.frame) + spacing, 
    CGRectGetMinY(self.contentView.bounds),
    MIN(textSize.width, availableWidth - detailSize.width - spacing),
    self.contentView.bounds.size.height);
  // put the detail right after it
  self.detailTextLabel.frame = CGRectMake(
    CGRectGetMaxX(self.textLabel.frame) + 10.0,
    self.textLabel.frame.origin.y, 
    detailSize.width,
    self.textLabel.frame.size.height);
}

@end
