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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class RecentPhotosActivity extends ImagePickerActivityBase {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			return;
		}

		if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AirImagePickerUtils.REQUEST_GALLERY_PERMISSION_ACTION);
		}
		else  {
			loadRecentPhotos();
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
			loadRecentPhotos();
		} else {
			// denied - do nothing
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerDataEvent_cancelled, "");
			finish();
		}

	}

	private void loadRecentPhotos() {

		try {
			String[] projection = new String[]{
					MediaStore.Images.ImageColumns._ID,
					MediaStore.Images.ImageColumns.DATA,
					MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
					MediaStore.Images.ImageColumns.DATE_TAKEN,
					MediaStore.Images.ImageColumns.MIME_TYPE
			};
			final Cursor cursor = AirImagePickerExtension.context.getActivity().getContentResolver()
					.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
							null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

			int fetchCount = cursor.getCount() > parameters.limit ? parameters.limit : cursor.getCount();

			JSONObject recentPhotosResult = new JSONObject();
			JSONArray recentPhotosArray = new JSONArray();

			for (int i = 0; i < fetchCount; i++) {
				cursor.moveToPosition(i);
				String imageLocation = cursor.getString(1);
				File imageFile = new File(imageLocation);
				if (imageFile.exists()) {
					Bitmap bitmap = AirImagePickerUtils.getOrientedSampleBitmapFromPath(imageLocation);
					if(bitmap == null) {
						AirImagePickerExtension.dispatchEvent(Constants.AirImagePickerErrorEvent_error, "Error occurred loading recent photos bitmap!");
						finish();
						return;
					}
					bitmap = AirImagePickerUtils.resizeImage(bitmap, parameters.maxWidth, parameters.maxHeight);
					bitmap = AirImagePickerUtils.swapColors(bitmap);
					AirImagePickerExtensionContext.storeBitmap(imageLocation, bitmap);
					recentPhotosArray.put(imageLocation);

				}
			}

			recentPhotosResult.put("imagePaths", recentPhotosArray);

			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.recentResult, recentPhotosResult.toString());
			finish();

		} catch (Exception e) {
			AirImagePickerExtension.context.dispatchStatusEventAsync(Constants.AirImagePickerErrorEvent_error, e.getLocalizedMessage());
			finish();
		}
	}



}




