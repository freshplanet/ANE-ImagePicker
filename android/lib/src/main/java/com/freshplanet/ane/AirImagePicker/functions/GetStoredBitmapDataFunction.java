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

package com.freshplanet.ane.AirImagePicker.functions;

import android.graphics.Bitmap;
import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;

public class GetStoredBitmapDataFunction extends BaseFunction {
	
	
	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		super.call(context, args);

		String imagePath = getStringFromFREObject(args[0]);
		Bitmap bitmap = AirImagePickerExtensionContext.getStoredBitmap(imagePath);
		if(bitmap == null) {
			return null;
		}

		Byte color[] = {0, 0, 0, 0};
		FREBitmapData as3BitmapData = null;

		try {
			as3BitmapData = FREBitmapData.newBitmapData(bitmap.getWidth(),
					bitmap.getHeight(), false, color);
			as3BitmapData.acquire();
			bitmap.copyPixelsToBuffer(as3BitmapData.getBits());
			as3BitmapData.release();
		} catch (Exception e) {
			context.dispatchStatusEventAsync("log", "displayImagePicker error trying to create bitmapdata " + e.getLocalizedMessage());
		}

		return as3BitmapData;

	}
}
