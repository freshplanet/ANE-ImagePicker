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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

import java.io.File;
import java.util.List;

import static com.freshplanet.ane.AirImagePicker.AirImagePickerExtension.context;

public class CameraActivity extends ImagePickerActivityBase {



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
//			// Do something for lollipop and above versions
//		} else{
//			// do something for phones running an SDK before lollipop
//		}
		if(savedInstanceState != null) {
			return;
		}

		// if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
		// 	ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, AirImagePickerUtils.REQUEST_CAMERA_PERMISSION_ACTION);
		// }
		// else  {
			displayCamera();
		//}

	}

	private void displayCamera() {
		Context appContext = getApplicationContext();
		File tempFile = AirImagePickerUtils.getTemporaryFile(appContext, ".jpg");
		result.imagePath = tempFile.getAbsolutePath();

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		imageUri = FileProvider.getUriForFile(appContext,
				appContext.getPackageName() + ".provider",
				tempFile);

		intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

		startActivityForResult(intent, AirImagePickerUtils.CAMERA_IMAGE_ACTION);
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
			displayCamera();
		} else {
			// denied - do nothing
			finish();
		}

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

				if(bitmap == null) {
					AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
					finish();
					return;
				}

				bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
				bitmap = AirImagePickerUtils.swapColors(bitmap);
				AirImagePickerExtensionContext.storeBitmap(result.imagePath, bitmap);
				AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.photoChosen, result.imagePath);
				finish();
			}
		}
		else {
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
			finish();
		}


	}

}
