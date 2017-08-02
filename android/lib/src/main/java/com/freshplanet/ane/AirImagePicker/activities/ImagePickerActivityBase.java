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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.ImagePickerParameters;
import com.freshplanet.ane.AirImagePicker.ImagePickerResult;


public abstract class ImagePickerActivityBase extends Activity {
	public static final String TAG = "AirImagePicker";
	public static final String PARAMETERS = ":parameters";
	public static final String RESULT = ":result";
	
	protected String airPackageName;
	
	protected ImagePickerParameters parameters;
	protected ImagePickerResult result;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "[ImagePickerActivityBase] Entering onCreate");
		if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			savedInstanceState.setClassLoader(ImagePickerParameters.class.getClassLoader());
			parameters = savedInstanceState.getParcelable(PARAMETERS);
			savedInstanceState.setClassLoader(ImagePickerResult.class.getClassLoader());
			result = savedInstanceState.getParcelable(RESULT);
		}
		
		if(airPackageName == null) {
			airPackageName = this.getPackageName();
			Log.d(TAG, "[ImagePickerActivityBase] my package name:" + getPackageName());
		}
		
		if(parameters == null) {
			Bundle b = this.getIntent().getBundleExtra(airPackageName + PARAMETERS);
			b.setClassLoader(ImagePickerParameters.class.getClassLoader());
			parameters = (ImagePickerParameters)b.getParcelable(PARAMETERS);
		}
		
		if(result == null) {
			String resultKey = airPackageName + RESULT;
			if(getIntent().hasExtra(resultKey)) {
				Bundle b = getIntent().getBundleExtra(airPackageName + RESULT);
				b.setClassLoader(ImagePickerResult.class.getClassLoader());
				result = (ImagePickerResult)b.getParcelable(RESULT);
			} else {
				result = new ImagePickerResult(parameters.scheme, parameters.mediaType);
			}
		}
		
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "[ImagePickerActivityBase] Exiting onCreate");
	}

    protected void doCrop() {
    	if(parameters.shouldCrop && (result.imagePath != null)) {
	    	Intent intent = new Intent(getApplicationContext(), CropActivity.class);
	    	Bundle b = new Bundle();
	    	b.putParcelable(PARAMETERS, parameters);
	    	intent.putExtra(airPackageName + PARAMETERS, b);
	    	b = new Bundle();
	    	b.putParcelable(RESULT, result);
	    	intent.putExtra(airPackageName + RESULT, b);
			startActivity(intent);
    	}
    }

	protected String getPath(Uri selectedImage) {
		final String[] filePathColumn = { MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME };
		Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

		// Some devices return an URI of com.android instead of com.google.android
		if (selectedImage.toString().startsWith("content://com.android.gallery3d.provider")) {
			selectedImage = Uri.parse( selectedImage.toString().replace("com.android.gallery3d", "com.google.android.gallery3d") );
		}

		if (cursor != null) {
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

			// if it is a picassa image on newer devices with OS 3.0 and up
			if (AirImagePickerUtils.isPicasa(selectedImage.toString()))
			{
				columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
				return selectedImage.toString();
			}
			else
			{
				return cursor.getString(columnIndex);
			}
		}
		else if ( selectedImage != null && selectedImage.toString().length() > 0 ) {
			return selectedImage.toString();
		}
		else return null;
	}


	
}
