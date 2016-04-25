//
//  NSURL+Rewriters.m
//  VoiceThread
//
//  Created by Jesse Crossen on 5/5/11.
//  Copyright 2011 VoiceThread. All rights reserved.
//

#import "NSURL+Rewriters.h"

@implementation NSURL (Rewriters)

// get a URL for a unique temporary file
+ (NSURL *)tempFileURLWithPrefix:(NSString *)type extension:(NSString *)extension {
  return([NSURL fileURLWithPath:
    [NSString stringWithFormat:@"%@%@_%08x_%08x.%@",
      NSTemporaryDirectory(),
      type,
      (uint32_t)arc4random(), 
      (uint32_t)floor([[NSDate date] timeIntervalSince1970]),
      extension]]);
}

@end
