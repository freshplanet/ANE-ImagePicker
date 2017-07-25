/*
 * Copyright 2017 FreshPlanet
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#import "FPANEUtils.h"

#pragma mark - Dispatch events

void FPANE_DispatchEvent(FREContext context, NSString* eventName) {
    
    FREDispatchStatusEventAsync(context, (const uint8_t*) [eventName UTF8String], (const uint8_t*) "");
}

void FPANE_DispatchEventWithInfo(FREContext context, NSString* eventName, NSString* eventInfo) {
    
    FREDispatchStatusEventAsync(context, (const uint8_t*) [eventName UTF8String], (const uint8_t*) [eventInfo UTF8String]);
}

void FPANE_Log(FREContext context, NSString* message) {
    
    FPANE_DispatchEventWithInfo(context, @"LOGGING", message);
}

#pragma mark - FREObject -> Obj-C

NSString* FPANE_FREObjectToNSString(FREObject object) {
    
    uint32_t stringLength;
    const uint8_t* string;
    FREGetObjectAsUTF8(object, &stringLength, &string);
    return [NSString stringWithUTF8String:(char*) string];
}

NSArray* FPANE_FREObjectToNSArrayOfNSString(FREObject object) {
    
    uint32_t arrayLength;
    FREGetArrayLength(object, &arrayLength);
    
    uint32_t stringLength;
    NSMutableArray* mutableArray = [NSMutableArray arrayWithCapacity:arrayLength];
    for (NSInteger i = 0; i < arrayLength; i++) {
        FREObject itemRaw;
        FREGetArrayElementAt(object, (uint) i, &itemRaw);
        
        // Convert item to string. Skip with warning if not possible.
        const uint8_t* itemString;
        if (FREGetObjectAsUTF8(itemRaw, &stringLength, &itemString) != FRE_OK) {
            NSLog(@"Couldn't convert FREObject to NSString at index %ld", (long) i);
            continue;
        }
        
        NSString* item = [NSString stringWithUTF8String:(char*) itemString];
        [mutableArray addObject:item];
    }
    
    return [NSArray arrayWithArray:mutableArray];
}

UIImage* FPANE_FREBitmapDataToUIImage(FREObject object) {
    
    
        FREBitmapData bitmapData;
        UIImage*    image = nil;
        // Convert item to UIImage. Skip with warning if not possible.
        if (FREAcquireBitmapData(object, &bitmapData) == FRE_OK) {
            
            // make data provider from buffer
            CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, bitmapData.bits32, (bitmapData.width * bitmapData.height * 4), NULL);
            
            // set up for CGImage creation
            int             bitsPerComponent    = 8;
            int             bitsPerPixel        = 32;
            int             bytesPerRow         = 4 * bitmapData.width;
            CGColorSpaceRef colorSpaceRef       = CGColorSpaceCreateDeviceRGB();
            CGBitmapInfo    bitmapInfo;
            
            if (!bitmapData.hasAlpha)
                bitmapInfo = kCGBitmapByteOrder32Little | kCGImageAlphaNoneSkipFirst;
            else {
                
                if (bitmapData.isPremultiplied)
                    bitmapInfo = kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst;
                else
                    bitmapInfo = kCGBitmapByteOrder32Little | kCGImageAlphaFirst;
            }
            
            CGColorRenderingIntent renderingIntent = kCGRenderingIntentDefault;
            CGImageRef imageRef = CGImageCreate(bitmapData.width, bitmapData.height, bitsPerComponent,
                                                bitsPerPixel, bytesPerRow, colorSpaceRef,
                                                bitmapInfo, provider, NULL, YES, renderingIntent);
            
            // make UIImage from CGImage
            image = [UIImage imageWithCGImage:imageRef];
            
            FREReleaseBitmapData(object);
            
            NSData* imageData = UIImagePNGRepresentation(image);
            
            image = [UIImage imageWithData:imageData];
            
        }
        else {
            NSLog(@"Couldn't convert FREObject to UIImage");
        }
    
    return image;
}

NSArray* FPANE_FREObjectToNSArrayOfUIImage(FREObject object) {
    
    uint32_t arrayLength;
    FREGetArrayLength(object, &arrayLength);
    
    NSMutableArray* mutableArray = [NSMutableArray arrayWithCapacity:arrayLength];
    for (NSInteger i = 0; i < arrayLength; i++) {
        FREObject itemRaw;
        FREGetArrayElementAt(object, (uint) i, &itemRaw);
        
        UIImage *image = FPANE_FREBitmapDataToUIImage(itemRaw);
        [mutableArray addObject:image];
    }
    
    return [NSArray arrayWithArray:mutableArray];
}

NSDictionary* FPANE_FREObjectsToNSDictionaryOfNSString(FREObject keys, FREObject values) {
    
    uint32_t numKeys, numValues;
    FREGetArrayLength(keys, &numKeys);
    FREGetArrayLength(values, &numValues);
    
    uint32_t stringLength;
    uint32_t numItems = MIN(numKeys, numValues);
    NSMutableDictionary* mutableDictionary = [NSMutableDictionary dictionaryWithCapacity:numItems];
    for (NSInteger i = 0; i < numItems; i++) {
        FREObject keyRaw, valueRaw;
        FREGetArrayElementAt(keys, (uint) i, &keyRaw);
        FREGetArrayElementAt(values, (uint) i, &valueRaw);
        
        // Convert key and value to strings. Skip with warning if not possible.
        const uint8_t* keyString, * valueString;
        if (FREGetObjectAsUTF8(keyRaw, &stringLength, &keyString) != FRE_OK || FREGetObjectAsUTF8(valueRaw, &stringLength, &valueString) != FRE_OK) {
            NSLog(@"Couldn't convert FREObject to NSString at index %ld", (long) i);
            continue;
        }
        
        NSString* key = [NSString stringWithUTF8String:(char*) keyString];
        NSString* value = [NSString stringWithUTF8String:(char*) valueString];
        [mutableDictionary setObject:value forKey:key];
    }
    
    return [NSDictionary dictionaryWithDictionary:mutableDictionary];
}

BOOL FPANE_FREObjectToBool(FREObject object) {
    
    uint32_t b;
    FREGetObjectAsBool(object, &b);
    return b != 0;
}

NSInteger FPANE_FREObjectToInt(FREObject object) {
    
    int32_t i;
    FREGetObjectAsInt32(object, &i);
    return i;
}

double FPANE_FREObjectToDouble(FREObject object) {
    
    double x;
    FREGetObjectAsDouble(object, &x);
    return x;
}

#pragma mark - Obj-C -> FREObject

FREObject FPANE_BOOLToFREObject(BOOL boolean) {
    
    FREObject result;
    FRENewObjectFromBool(boolean, &result);
    return result;
}

FREObject FPANE_IntToFREObject(NSInteger i) {
    
    FREObject result;
    FRENewObjectFromInt32((int32_t) i, &result);
    return result;
}

FREObject FPANE_DoubleToFREObject(double d) {
    
    FREObject result;
    FRENewObjectFromDouble(d, &result);
    return result;
}

FREObject FPANE_NSStringToFREObject(NSString* string) {
    
    FREObject result;
    FRENewObjectFromUTF8((int) string.length, (const uint8_t*) [string UTF8String], &result);
    return result;
}

FREObject FPANE_CreateError(NSString* error, NSInteger* id) {
    
    FREObject ret;
    FREObject errorThrown;
    
    FREObject freId;
    FRENewObjectFromInt32((int32_t) *id, &freId);
    FREObject argV[] = {
            FPANE_NSStringToFREObject(error),
            freId
    };
    FRENewObject((const uint8_t*) "Error", 2, argV, &ret, &errorThrown);
    
    return ret;
}

FREObject FPANE_UIImageToFREBitmapData(UIImage *image) {
    
    // create bitmap data
    FREObject widthObj;
    FRENewObjectFromInt32(image.size.width, &widthObj);
    FREObject heightObj;
    FRENewObjectFromInt32(image.size.height, &heightObj);
    FREObject transparent;
    FRENewObjectFromBool( 0, &transparent);
    FREObject fillColor;
    FRENewObjectFromUint32( 0x000000, &fillColor);
    
    FREObject params[4] = { widthObj, heightObj, transparent, fillColor };
    
    FREObject obj;
    FRENewObject((uint8_t *)"flash.display.BitmapData", 4, params, &obj , NULL);
    
    FREResult result;
    FREBitmapData bitmapData;
    result = FREAcquireBitmapData(obj, &bitmapData);
    if (result != FRE_OK) {
        return nil;
    }
    
    // Pull the raw pixels values out of the image data
    CGImageRef imageRef = [image CGImage];
    size_t width = CGImageGetWidth(imageRef);
    size_t height = CGImageGetHeight(imageRef);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    if(colorSpace == NULL) {
        return nil;
    }
    unsigned char *rawData = malloc(height * width * 4);
    size_t bytesPerPixel = 4;
    size_t bytesPerRow = bytesPerPixel * width;
    size_t bitsPerComponent = 8;
    CGContextRef context = CGBitmapContextCreate(rawData, width, height, bitsPerComponent, bytesPerRow, colorSpace, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    if(context == NULL) {
        return nil;
    }
    CGColorSpaceRelease(colorSpace);
    CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
    CGContextRelease(context);
    
    // Pixels are now it rawData in the format RGBA8888
    // Now loop over each pixel to write them into the AS3 BitmapData memory
    int x, y;
    // There may be extra pixels in each row due to the value of lineStride32.
    // We'll skip over those as needed.
    int offset = bitmapData.lineStride32 - bitmapData.width;
    size_t offset2 = bytesPerRow - bitmapData.width*4;
    int byteIndex = 0;
    uint32_t *bitmapDataPixels = bitmapData.bits32;
    for (y=0; y<bitmapData.height; y++)
    {
        for (x=0; x<bitmapData.width; x++, bitmapDataPixels++, byteIndex += 4)
        {
            // Values are currently in RGBA7777, so each color value is currently a separate number.
            int red     = (rawData[byteIndex]);
            int green   = (rawData[byteIndex + 1]);
            int blue    = (rawData[byteIndex + 2]);
            int alpha   = (rawData[byteIndex + 3]);
            
            // Combine values into ARGB32
            *bitmapDataPixels = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        
        bitmapDataPixels += offset;
        byteIndex += offset2;
    }
    
    // Free the memory we allocated
    free(rawData);
    // Tell Flash which region of the BitmapData changes (all of it here)
    result = FREInvalidateBitmapDataRect(obj, 0, 0, bitmapData.width, bitmapData.height);
    // Release our control over the BitmapData
    if(result == FRE_OK) {
        result = FREReleaseBitmapData(obj);
        return obj;
    } else {
        return nil;
    }
    
    return nil;
}

FREObject FPANE_UIImageToFREByteArray(UIImage *image) {
    
    NSData *imageJPEGData = UIImageJPEGRepresentation(image, 1.0);
    
    FREObject newByteArray;
    if(FRENewObject((uint8_t *)"flash.utils.ByteArray", 0, NULL, &newByteArray , NULL) != FRE_OK) {
        return nil;
    };
    
    FREObject dataLength;
    FRENewObjectFromUint32((uint32_t) imageJPEGData.length, &dataLength);
    if (FRESetObjectProperty(newByteArray, (const uint8_t *)"length", dataLength, NULL) != FRE_OK) {
        return nil;
    }
    
    
    FREByteArray byteArray;
    if(FREAcquireByteArray(newByteArray, &byteArray) != FRE_OK) {
        return nil;
    };
    
    // Copy JPEG representation in ByteArray
    NSLog(@"do memcopy:");
    memcpy(byteArray.bytes, imageJPEGData.bytes, imageJPEGData.length);
    NSLog(@"did memcopy:");
    
    // Release our control over the ByteArray
    if(FREReleaseByteArray(newByteArray) != FRE_OK) {
        return nil;
    };
    
    return newByteArray;
}


