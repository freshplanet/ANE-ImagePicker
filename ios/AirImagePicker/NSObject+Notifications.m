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

#import "NSObject+Notifications.h"


@implementation NSObject (Notifications)

- (void)observeNotificationsNamed:(NSString *)name selector:(SEL)selector {
  if (name != nil)
    [[NSNotificationCenter defaultCenter] 
      addObserver:self selector:selector name:name object:nil];
}

- (void)observeNotificationsFromObject:(id)object selector:(SEL)selector {
  if (object != nil)
    [[NSNotificationCenter defaultCenter] 
      addObserver:self selector:selector name:nil object:object];
}

- (void)observeNotificationsFromObject:(id)object named:(NSString *)name 
        selector:(SEL)selector {
  if ((object != nil) && (name != nil))
    [[NSNotificationCenter defaultCenter] 
      addObserver:self selector:selector name:name object:object];
}

- (void)stopObservingNotificationsFromObject:(id)object {
  // only do this if the object is not nil, or we'd remove all notifications
  if (object != nil)
    [[NSNotificationCenter defaultCenter] 
      removeObserver:self name:nil object:object];
}

- (void)stopObservingNotifications {
  [[NSNotificationCenter defaultCenter]
    removeObserver:self];
}

- (void)notifyWithName:(NSString *)name {
  [[NSNotificationCenter defaultCenter]
    postNotificationName:name object:self];
}

- (void)notifyWithName:(NSString *)name userInfo:(NSDictionary *)userInfo {
  [[NSNotificationCenter defaultCenter]
    postNotificationName:name object:self
    userInfo:userInfo];
}

@end
