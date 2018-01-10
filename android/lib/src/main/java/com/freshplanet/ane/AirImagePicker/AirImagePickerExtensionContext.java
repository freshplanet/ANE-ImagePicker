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

package com.freshplanet.ane.AirImagePicker;

import android.graphics.Bitmap;
import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayCameraFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayImagePickerFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetCameraPermissionStatus;
import com.freshplanet.ane.AirImagePicker.functions.GetGalleryPermissionStatus;
import com.freshplanet.ane.AirImagePicker.functions.GetStoredBitmapDataFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetStoredByteArrayFunction;
import com.freshplanet.ane.AirImagePicker.functions.LoadRecentPhotosFunction;
import com.freshplanet.ane.AirImagePicker.functions.OpenSettingsFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveStoredImageFunction;

import java.util.HashMap;
import java.util.Map;

public class AirImagePickerExtensionContext extends FREContext {

	private static Map<String, Bitmap> _storedImages = new HashMap<String, Bitmap>();

	public static void storeBitmap(String imagePath, Bitmap bitmap) {
		_storedImages.put(imagePath, bitmap);
	}

	public static Bitmap getStoredBitmap(String imagePath) {
		Bitmap bitmap = _storedImages.get(imagePath);
		return bitmap;
	}

	public static void removeStoredBitmap(String imagePath) {
		_storedImages.remove(imagePath);
	}

	@Override
	public void dispose() {
		AirImagePickerExtension.context = null;
		_storedImages = null;

	}

	@Override
	public Map<String, FREFunction> getFunctions() {
		Map<String, FREFunction> functions = new HashMap<String, FREFunction>();
		
		functions.put("displayImagePicker", new DisplayImagePickerFunction());
		functions.put("displayCamera", new DisplayCameraFunction());
		functions.put("loadRecentImages", new LoadRecentPhotosFunction());
		functions.put("openSettings", new OpenSettingsFunction());
		functions.put("internalGetChosenPhotoBitmapData", new GetStoredBitmapDataFunction());
		functions.put("internalGetChosenPhotoByteArray", new GetStoredByteArrayFunction());
		functions.put("internalRemoveStoredImage", new RemoveStoredImageFunction());
		functions.put("getCameraPermissionStatus", new GetCameraPermissionStatus());
		functions.put("getGalleryPermissionStatus", new GetGalleryPermissionStatus());
		return functions;
	}


}