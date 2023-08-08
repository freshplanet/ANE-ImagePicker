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

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class CropActivity extends ImagePickerActivityBase {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			return;
		}

		try {
			Uri imageUri = (Uri) getIntent().getExtras().get("imageUri");
			File tempFile = AirImagePickerUtils.getTemporaryFile(getApplicationContext(), ".jpg");
			UCrop uCrop = UCrop.of(imageUri, Uri.fromFile(tempFile));
			uCrop.withAspectRatio(1,1);
			uCrop.start(CropActivity.this);
		}
		catch (Exception e) {
			AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, e.getLocalizedMessage());
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == UCrop.REQUEST_CROP) {
			if(resultCode == RESULT_CANCELED) {
				AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerDataEvent_cancelled, "");
			}
			else if (resultCode == RESULT_OK) {
				final Uri resultUri = UCrop.getOutput(data);
				Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(resultUri.getPath());
				bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
				bitmap = AirImagePickerUtils.swapColors(bitmap);
				AirImagePickerExtensionContext.storeBitmap(result.imagePath, bitmap);
				AirImagePickerExtension.dispatchEvent(Constants.photoChosen, result.imagePath);
			}
			else if(resultCode == UCrop.RESULT_ERROR) {
				Throwable cropError = UCrop.getError(data);
				AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, cropError.toString());
			}
		}
		AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, "Something went wrong");
		finish();




	}


}
