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

#import "NSString+Formatters.h"

#import <CommonCrypto/CommonDigest.h>

@implementation NSString (Formatters)

+ (NSString *)stringWithSeconds:(NSTimeInterval)seconds 
              style:(TimeDisplayStyle)style {
  // add a sign if needed and treat this as a positive number of seconds
  NSString *sign = @"";
  if (seconds < 0.0) {
    sign = @"-";
    seconds = -seconds;
  }
  // get hours
  NSInteger hours = floor(seconds / 3600.0);
  seconds -= (hours * 3600.0);
  // get minutes
  NSInteger minutes = floor(seconds / 60.0);
  seconds -= (minutes * 60.0);
  // make seconds an integer
  NSInteger wholeSeconds = round(seconds);
  // handle times with an hour component
  if (hours > 0) {
    // select a format
    NSString *format = @"%@%d:%02d:%02d";
    if (style == TimeDisplayStyleHMS) {
      format = @"%@%dh %02dm %02ds";
    }
    return([NSString 
      stringWithFormat:format, sign, hours, minutes, wholeSeconds]);
  }
  // handle times without an hour component
  else {
    // select a format
    NSString *format = @"%@%d:%02d";
    if (style == TimeDisplayStyleHMS) {
      // when showing units, we don't need to show minutes
      //  if there aren't any
      if (! (minutes > 0)) {
        return([NSString stringWithFormat:@"%@%lds", sign, (long)wholeSeconds]);
      }
      // show minutes if there are some
      format = @"%@%dm %02ds";
    }
    return([NSString 
      stringWithFormat:format, sign, minutes, wholeSeconds]);
  }
}

+ (NSString *)stringWithDate:(NSDate *)date {
  return([NSDateFormatter 
    localizedStringFromDate:date 
    dateStyle:NSDateFormatterLongStyle
    timeStyle:NSDateFormatterNoStyle]);
}

- (NSUInteger)wordCount {
  NSScanner *scanner = [NSScanner scannerWithString:self];
  NSCharacterSet *whiteSpace = 
    [NSCharacterSet whitespaceAndNewlineCharacterSet];
  NSUInteger count = 0;
  while ([scanner scanUpToCharactersFromSet:whiteSpace  intoString:nil]) {
    count++;
  }
  return(count);
}

// return the ASCII hex MD5 hash of the receiver
- (NSString *)stringByComputingMD5Digest {
  // convert this string into a buffer
  const char *buffer = [self UTF8String];
  // make a buffer to recieve the digest
  unsigned char r[CC_MD5_DIGEST_LENGTH];
  CC_MD5(buffer, (unsigned int)strlen(buffer), r);
  return([NSString stringWithFormat:
    @"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
    r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], 
    r[9], r[10], r[11], r[12], r[13], r[14], r[15]]);
}

// return the string with all consecutive runs of whitespace replaced by a space
- (NSString *)stringByCondensingConsecutiveWhitespace {
  NSScanner *scanner = [[NSScanner alloc] initWithString:self];
  [scanner setCharactersToBeSkipped:nil];
  NSMutableString *output = [NSMutableString string];
  while (! [scanner isAtEnd]) {
    NSString *part = nil;
    // scan non-whitespace into the clean string
    [scanner scanUpToCharactersFromSet:
        [NSCharacterSet whitespaceAndNewlineCharacterSet] 
      intoString:&part];
    if (part) [output appendString:part];
    // replace all whitespace runs with a single space
    [scanner scanCharactersFromSet:
        [NSCharacterSet whitespaceAndNewlineCharacterSet] 
      intoString:nil];
    [output appendString:@" "];
  }
  [scanner release], scanner = nil;
  return(output);
}

@end
