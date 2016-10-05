//
//  ALAssetsLibrary+ALAssetsLibrary_CustomPhotoAlbum.h
//  AirImagePicker
//
//  Created by Daniel Rodriguez on 4/4/13.
//  Copyright (c) 2013 FreshPlanet. All rights reserved.
//

#import <AssetsLibrary/AssetsLibrary.h>

typedef void(^SaveImageCompletion)(NSError* error, ALAsset *asset);

@interface ALAssetsLibrary (CustomPhotoAlbum)

-(void)saveImage:(UIImage*)image toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock;
-(void)addAssetURL:(NSURL*)assetURL toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock;
-(void)saveVideo:(NSURL*)videoURL toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock;

@end
