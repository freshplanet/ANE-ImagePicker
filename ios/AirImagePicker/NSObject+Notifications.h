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


@interface NSObject (Notifications)

// route all notifications with the given name to a selector on the receiver
- (void)observeNotificationsNamed:(NSString *)name selector:(SEL)selector;

// route all notifications from an object to a selector on the receiver
- (void)observeNotificationsFromObject:(id)object selector:(SEL)selector;

// route all notifications from an object with a particular name
//  to a selector on the receiver
- (void)observeNotificationsFromObject:(id)object named:(NSString *)name 
        selector:(SEL)selector;

// stop observing notifications from an object
- (void)stopObservingNotificationsFromObject:(id)object;

// remove this object as an observer for all notifications
- (void)stopObservingNotifications;

// post a notification with the given name, and possibly with an info dictionary
- (void)notifyWithName:(NSString *)name;
- (void)notifyWithName:(NSString *)name userInfo:(NSDictionary *)userInfo;

@end
