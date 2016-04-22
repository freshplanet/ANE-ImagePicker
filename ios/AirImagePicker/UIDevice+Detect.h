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


@interface UIDevice (Detect)

// return whether the device has a large screen (i.e. is an iPad)
//  (the usual way of testing this is extremely ugly and cumbersome)
+ (BOOL)hasLargeScreen;

// return whether the device prefers a flat interface as introduced in iOS 7.0
+ (BOOL)hasFlatInterface;

@end
