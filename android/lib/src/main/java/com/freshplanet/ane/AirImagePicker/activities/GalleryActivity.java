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

package com.freshplanet.ane.AirImagePicker.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

public class GalleryActivity extends ImagePickerActivityBase {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AirImagePickerExtension.context.dispatchStatusEventAsync("log", "GalleryActivity");

		Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		int action = AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION;
		startActivityForResult(intent, action);
			
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			Uri selectedImageUri = data.getData();

			result.imagePath = getPath(selectedImageUri);

			if(parameters.shouldCrop) {
				finish();
				doCrop();
				return;
			}

			Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(result.imagePath);

			bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
			bitmap = AirImagePickerUtils.swapColors(bitmap);
			AirImagePickerExtensionContext.storeBitmap(result.imagePath, bitmap);
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.photoChosen, result.imagePath);
			finish();

		}
		else {
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
		}
	}


}




