//
//  ALAssetsLibrary+ALAssetsLibrary_CustomPhotoAlbum.m
//  AirImagePicker
//
//  Created by Daniel Rodriguez on 4/4/13.
//  Copyright (c) 2013 FreshPlanet. All rights reserved.
//

#import "ALAssetsLibrary+CustomPhotoAlbum.h"

@implementation ALAssetsLibrary(CustomPhotoAlbum)

-(void)saveImage:(UIImage*)image toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock
{
    //write the image data to the assets library (camera roll)
    [self writeImageToSavedPhotosAlbum:image.CGImage orientation:(ALAssetOrientation)image.imageOrientation
                       completionBlock:^(NSURL* assetURL, NSError* error) {
                           
                           //error handling
                           if (error!=nil) {
                               completionBlock(error, nil);
                               return;
                           }
                           
                           //add the asset to the custom photo album
                           [self addAssetURL: assetURL
                                     toAlbum:albumName
                         withCompletionBlock:completionBlock];
                           
                       }];
}

-(void)saveVideo:(NSURL*)videoURL toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock
{
    NSLog(@"Entering - saveVideo");
    [self writeVideoAtPathToSavedPhotosAlbum:videoURL completionBlock:^(NSURL *assetURL, NSError *error) {
        if(error != nil) {
            completionBlock(error, nil);
            return;
        }
        [self addAssetURL: assetURL toAlbum:albumName withCompletionBlock:completionBlock];
    }];
}

-(void)addAssetURL:(NSURL*)assetURL toAlbum:(NSString*)albumName withCompletionBlock:(SaveImageCompletion)completionBlock
{
    __block BOOL albumWasFound = NO;
    
    //search all photo albums in the library
    [self enumerateGroupsWithTypes:ALAssetsGroupAlbum
                        usingBlock:^(ALAssetsGroup *group, BOOL *stop) {
                            
                            //compare the names of the albums
                            if ([albumName compare: [group valueForProperty:ALAssetsGroupPropertyName]]==NSOrderedSame) {
                                
                                //target album is found
                                albumWasFound = YES;
                                
                                //get a hold of the photo's asset instance
                                [self assetForURL: assetURL
                                      resultBlock:^(ALAsset *asset) {
                                          
                                          //add photo to the target album
                                          [group addAsset: asset];
                                          
                                          //run the completion block
                                          completionBlock(nil, asset);
                                          
                                      } failureBlock: ^(NSError * error) {completionBlock(error, nil);}];
                                
                                //album was found, bail out of the method
                                return;
                            }
                            
                            if (group==nil && albumWasFound==NO) {
                                //photo albums are over, target album does not exist, thus create it
                                
                                __unsafe_unretained ALAssetsLibrary* weakSelf = self;
                                
                                //create new assets album
                                [self addAssetsGroupAlbumWithName:albumName
                                                      resultBlock:^(ALAssetsGroup *group) {
                                                          
                                                          //get the photo's instance
                                                          [weakSelf assetForURL: assetURL
                                                                    resultBlock:^(ALAsset *asset) {
                                                                        
                                                                        //add photo to the newly created album
                                                                        [group addAsset: asset];
                                                                        
                                                                        //call the completion block
                                                                        completionBlock(nil, asset);
                                                                        
                                                                    } failureBlock: ^(NSError * error) {completionBlock(error, nil);}];
                                                          
                                                      } failureBlock: ^(NSError * error) {completionBlock(error, nil);}];
                                
                                //should be the last iteration anyway, but just in case
                                return;
                            }
                            
                        } failureBlock: ^(NSError * error) {completionBlock(error, nil);} ];
    
}
@end