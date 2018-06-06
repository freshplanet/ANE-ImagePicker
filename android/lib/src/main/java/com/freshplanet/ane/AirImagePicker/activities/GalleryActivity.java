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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

import java.io.File;

public class GalleryActivity extends ImagePickerActivityBase {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			return;
		}

		if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AirImagePickerUtils.REQUEST_GALLERY_PERMISSION_ACTION);
		}
		else  {
			displayImagePicker();
		}
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
			Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			int action = AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION;
			startActivityForResult(intent, action);
		}
		catch (Exception e) {
			AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, e.getLocalizedMessage());
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			try {
				Uri selectedImageUri = data.getData();

				result.imagePath = getPath(selectedImageUri);
				if(result.imagePath == null)
					result.imagePath = getPathFromInputStreamUri(AirImagePickerExtension.context.getActivity().getApplicationContext(), selectedImageUri);
				imageUri = selectedImageUri;

				if(parameters.shouldCrop) {

					// save to temp file and then do crop thingy
					Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(result.imagePath);
					File imageFile = AirImagePickerUtils.saveToTemporaryFile(getApplicationContext() ,".jpg", bitmap );

					imageUri = FileProvider.getUriForFile(this,
							getApplicationContext().getPackageName() + ".provider",
							imageFile);
					result.imagePath = imageFile.getAbsolutePath();

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
	}




}




