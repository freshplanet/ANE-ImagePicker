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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

import java.io.File;
import java.util.List;

public class CropActivity extends ImagePickerActivityBase {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			return;
		}

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


		Context appContext = getApplicationContext();

		String cropInputPath = result.imagePath;
		Uri imageUri = (Uri)getIntent().getExtras().get("imageUri");

		intent.setDataAndType(imageUri, "image/*");

		// Set crop output
		File tempFile = AirImagePickerUtils.getTemporaryFile(appContext, ".jpg");

		result.imagePath = tempFile.getAbsolutePath();


		Uri uri = FileProvider.getUriForFile(this,
				appContext.getPackageName() + ".provider",
				tempFile);
		List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo resolveInfo : resInfoList) {
			String packageName = resolveInfo.activityInfo.packageName;
			grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);


		// Set crop output size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(cropInputPath, options);
		int smallestEdge = Math.min(options.outWidth, options.outHeight);

		// Cropped image should be square (aspect ratio 1:1)
		intent.putExtra("outputX", smallestEdge);
		intent.putExtra("outputY", smallestEdge);
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);
		intent.putExtra("return-data", true);

		startActivityForResult(intent, AirImagePickerUtils.CROP_ACTION);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {

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
		else {
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
			finish();
		}


	}


}
