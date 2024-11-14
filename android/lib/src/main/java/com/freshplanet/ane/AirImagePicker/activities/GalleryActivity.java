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

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

public class GalleryActivity extends ImagePickerActivityBase {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		displayImagePicker();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		int permissionCheck = PackageManager.PERMISSION_GRANTED;
		for (int permission : grantResults) {
			permissionCheck = permissionCheck + permission;
		}
		if ((grantResults.length > 0) && permissionCheck == PackageManager.PERMISSION_GRANTED) {
			// granted
			displayImagePicker();
		} else {
			// denied - do nothing
			AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, "Permission denied in GalleryActivity");
			finish();
		}

	}

	private void displayImagePicker() {
		try{
			ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
					registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), handleImagePicked);

			pickMedia.launch(new PickVisualMediaRequest.Builder()
					.setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
					.build());
		}
		catch (Exception e) {
			AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, e.getLocalizedMessage());
			finish();
		}
	}


	private final ActivityResultCallback<Uri> handleImagePicked = uri -> {
		if (uri != null) {
			try {
				result.imagePath = getPathFromInputStreamUri(AirImagePickerExtension.context.getActivity().getApplicationContext(), uri);
				imageUri = uri;

				if(parameters.shouldCrop) {

					doCrop();
					finish();
				}
				else {
					Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(result.imagePath);
					if(bitmap == null) {
						AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, "Something went wrong while trying to get the photo");
						finish();
						return;
					}

					bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
					bitmap = AirImagePickerUtils.swapColors(bitmap);
					AirImagePickerExtensionContext.storeBitmap(result.imagePath, bitmap);
					AirImagePickerExtension.dispatchEvent(Constants.photoChosen, result.imagePath);
					finish();
				}
			}
			catch (Exception e) {
				AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, e.getLocalizedMessage());
				finish();
			}

		}
		else {
			AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerDataEvent_cancelled, "");
			finish();
		}
	};
}




