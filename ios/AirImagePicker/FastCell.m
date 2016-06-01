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

#import "FastCell.h"

// make a view class that re-routes its drawing to the superview
@interface FastCellView : UIView
@end
@implementation FastCellView
- (void)drawRect:(CGRect)r {
  id node = [self superview];
  while (node) {
    if ([node isKindOfClass:[FastCell class]]) break;
    node = [node superview];
  }
  if (node) [(FastCell *)node drawContentView:r];
}
@end

@implementation FastCell

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
  if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
		contentView = [[FastCellView alloc] initWithFrame:CGRectZero];
		contentView.opaque = YES;
		[self addSubview:contentView];
  }
  return self;
}
- (void)dealloc {
  [contentView release];
  [super dealloc];
}

- (void)setFrame:(CGRect)f {
  [super setFrame:f];
  [contentView setFrame:[self bounds]];
}

- (void)setNeedsDisplay {
  [super setNeedsDisplay];
  [contentView setNeedsDisplay];
}

- (void)drawContentView:(CGRect)r {
  // subclasses should implement this
}

@end
