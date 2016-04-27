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

#import "ProgressController.h"

// private methods
@interface ProgressController()

- (void)progressTypeDidChange;

- (void)timerDidFire;

@end

@implementation ProgressController

@synthesize progressTarget, taskName;

// attempt to use the same kind of status bar 
//  as the VC that's showing progress
- (UIStatusBarStyle)preferredStatusBarStyle {
  if (([self respondsToSelector:@selector(presentingViewController)]) &&
      ([[self presentingViewController] 
        respondsToSelector:@selector(preferredStatusBarStyle)])) {
    return([[self presentingViewController] preferredStatusBarStyle]);
  }
  if ([self.parentViewController 
        respondsToSelector:@selector(preferredStatusBarStyle)]) {
    return([self.parentViewController preferredStatusBarStyle]);
  }
  return(UIStatusBarStyleBlackOpaque);
}

- (id)initWithTaskNamed:(NSString *)inTaskName 
        progressTarget:(id)targetOrNil {
  if ((self = [super initWithNibName:nil bundle:nil])) {
    taskName = [inTaskName retain];
    progressTarget = [targetOrNil retain];
  }
  return(self);
}
- (void)dealloc {
  [taskName release], taskName = nil;
  [progressTarget release], progressTarget = nil;
  [textLabel release], textLabel = nil;
  [progressBar release], progressBar = nil;
  [spinner release], spinner = nil;
  [timer release], timer = nil;
  [super dealloc];
}

- (void)setProgressTarget:(id)newProgressTarget {
  if (newProgressTarget != progressTarget) {
    [progressTarget release];
    progressTarget = [newProgressTarget retain];
    [self performSelectorOnMainThread:
      @selector(progressTypeDidChange) 
      withObject:nil waitUntilDone:NO];
  }
}

- (void)setTaskName:(NSString *)newTaskName {
  if (newTaskName != taskName) {
    [taskName release];
    taskName = [newTaskName retain];
    [textLabel 
      performSelectorOnMainThread:@selector(setText:) 
      withObject:taskName waitUntilDone:NO];
    [self.view
      performSelectorOnMainThread:@selector(setNeedsDisplay) 
      withObject:nil waitUntilDone:NO];
  }
}

- (void)progressTypeDidChange {
  // see if we have determinate progress
  if (progressTarget != nil) {
    // show the progress bar
    [progressBar setHidden:NO];
    [spinner setHidden:YES];
    [spinner stopAnimating];
  }
  else {
    // show the spinner
    [spinner setHidden:NO];
    [spinner startAnimating];
    [progressBar setHidden:YES];
  }
  // update the display
  [self.view setNeedsDisplay];
}

- (void)loadView {
  // underlap the status bar
  self.wantsFullScreenLayout = YES;
  
  // create the main view to take up the whole screen
  self.view = [[[UIView alloc] 
    initWithFrame:[[UIScreen mainScreen] bounds]]
      autorelease];
  self.view.autoresizingMask = 
    UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  // dark, textured background
  self.view.backgroundColor = [UIColor viewFlipsideBackgroundColor];
  
  // determine the spacing between the label and indicator
  CGFloat spacing = 10.0;
  
  // add a label for the task
  if (taskName) {
    if (! textLabel) {
      textLabel = [[UILabel alloc] init];
      [self.view addSubview:textLabel];
      textLabel.font = [UIFont boldSystemFontOfSize:17.0];
      textLabel.backgroundColor = [UIColor clearColor];
      textLabel.textColor = [UIColor whiteColor];
      textLabel.shadowColor = [UIColor blackColor];
      textLabel.shadowOffset = CGSizeMake(0.0, -1.0);
      textLabel.textAlignment = NSTextAlignmentCenter;
    }
    textLabel.text = taskName;
    CGFloat lineHeight = textLabel.font.lineHeight;
    textLabel.frame = CGRectMake(
      CGRectGetMinX(self.view.bounds), 
      CGRectGetMidY(self.view.bounds) - lineHeight - (spacing / 2.0), 
      self.view.bounds.size.width, lineHeight);
    textLabel.autoresizingMask = 
      UIViewAutoresizingFlexibleLeftMargin |
      UIViewAutoresizingFlexibleRightMargin |
      UIViewAutoresizingFlexibleTopMargin |
      UIViewAutoresizingFlexibleBottomMargin;
  }
  
  //  make a determinate progress bar
  if (! progressBar) {
    progressBar = [[UIProgressView alloc]
      initWithProgressViewStyle:UIProgressViewStyleDefault];
    [self.view addSubview:progressBar];
  }
  CGFloat width = 120.0;
  progressBar.frame = CGRectMake(
    CGRectGetMidX(self.view.bounds) - (width / 2.0), 
    CGRectGetMidY(self.view.bounds) + (spacing / 2.0), 
    width, progressBar.frame.size.height);
  progressBar.autoresizingMask = 
    UIViewAutoresizingFlexibleLeftMargin |
    UIViewAutoresizingFlexibleRightMargin |
    UIViewAutoresizingFlexibleTopMargin |
    UIViewAutoresizingFlexibleBottomMargin;
  // hide it for now
  [progressBar setHidden:YES];

  // make a spinner
  if (! spinner) {
    spinner = [[UIActivityIndicatorView alloc]
      initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
    [self.view addSubview:spinner];
  }
  spinner.frame = CGRectMake(
    CGRectGetMidX(self.view.bounds) - (spinner.frame.size.width / 2.0), 
    CGRectGetMidY(self.view.bounds) + (spacing / 2.0), 
    spinner.frame.size.width, spinner.frame.size.height);
  spinner.autoresizingMask = 
    UIViewAutoresizingFlexibleLeftMargin |
    UIViewAutoresizingFlexibleRightMargin |
    UIViewAutoresizingFlexibleTopMargin |
    UIViewAutoresizingFlexibleBottomMargin;
  // hide it for now
  [spinner setHidden:YES];
  
  // show the appropriate progress indicator
  [self progressTypeDidChange];

}

// run the timer only when the view is showing
- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  // disable the idle timer, which seems to mess up some 
  //  kinds of media processing
  [UIApplication sharedApplication].idleTimerDisabled = YES;
  // start a timer to update progress
  if (timer) {
    [timer invalidate];
    [timer release], timer = nil;
  }
  timer = [[NSTimer scheduledTimerWithTimeInterval:0.1 
    target:self selector:@selector(timerDidFire) 
    userInfo:nil repeats:YES]
      retain];
}
- (void)viewDidDisappear:(BOOL)animated {
  [super viewDidDisappear:animated];
  // stop the progress timer
  [timer invalidate];
  [timer release], timer = nil;
  // turn the idle timer back on
  [UIApplication sharedApplication].idleTimerDisabled = YES;
}

- (void)timerDidFire {
  // if there's nowhere to get progress or no progress bar, 
  //  we can't do anything useful here
  if (progressTarget == nil) return;
  // get progress
  float progress = 0.0;
  if ([progressTarget respondsToSelector:@selector(progress)]) 
    progress = (float)[progressTarget progress];
  // show it on the progress bar
  [progressBar setProgress:progress];
}

- (void)viewDidUnload {
  [super viewDidUnload];
  // release retained subviews
  [textLabel release], textLabel = nil;
  [progressBar release], progressBar = nil;
  [spinner release], spinner = nil;
}

@end
