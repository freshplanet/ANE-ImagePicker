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
import android.provider.MediaStore;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;
import com.freshplanet.ane.AirImagePicker.Constants;

import java.io.File;

public class CameraActivity extends ImagePickerActivityBase {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		result.imagePath = tempFile.getAbsolutePath();
		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
		
		startActivityForResult(intent, AirImagePickerUtils.CAMERA_IMAGE_ACTION);
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_OK) {
			if (parameters.shouldCrop) {
				finish();
				doCrop();
			}
			else {
				Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(result.imagePath);

				bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
				bitmap = AirImagePickerUtils.swapColors(bitmap);
				AirImagePickerExtensionContext.storeBitmap(result.imagePath, bitmap);
				AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.photoChosen, result.imagePath);
				finish();

			}
		}
		else {
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
		}


	}

}
