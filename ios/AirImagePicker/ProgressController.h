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
#import <AVFoundation/AVFoundation.h>

@protocol ProgressTarget <NSObject>
  - (float) progress;
@end

@interface ProgressController : UIViewController {

  // a target to query for progress information 
  //  (using the 'progress' selector) or nil if 
  //  the ETA is indeterminate
  id<ProgressTarget> progressTarget;
  
  // a description of the task being performed
  NSString *taskName;
  // a label to describe the task
  UILabel *textLabel;
  // a view to show determinate progress
  UIProgressView *progressBar;
  // a view to show indeterminate progress
  UIActivityIndicatorView *spinner;

  // a timer for updating progress
  NSTimer *timer;

}

@property(nonatomic,retain) id progressTarget;
@property(nonatomic,retain) NSString *taskName;

- (id)initWithTaskNamed:(NSString *)inTaskName 
        progressTarget:(id)targetOrNil;

@end
