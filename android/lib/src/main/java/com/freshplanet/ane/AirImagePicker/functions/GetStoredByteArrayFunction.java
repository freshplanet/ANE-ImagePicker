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
import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class GetStoredByteArrayFunction extends BaseFunction {

	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		super.call(context, args);

		String imagePath = getStringFromFREObject(args[0]);
		Bitmap bitmap = AirImagePickerExtensionContext.getStoredBitmap(imagePath);
		bitmap = AirImagePickerUtils.swapColors(bitmap);

		if(bitmap == null) {
			return null;
		}

		FREByteArray as3bytes = null;
		try {
			as3bytes = FREByteArray.newByteArray();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
			as3bytes.setProperty("length", FREObject.newObject(out.size()));
			as3bytes.acquire();
			as3bytes.getBytes().put(out.toByteArray());
			as3bytes.release();

		} catch (Exception e) {
			context.dispatchStatusEventAsync("log", "displayImagePicker error trying to create byteArray "+ e.getLocalizedMessage());
		}

		return as3bytes;


	}
}
