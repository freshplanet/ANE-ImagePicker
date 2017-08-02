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

package com.freshplanet.ane.AirImagePicker.functions;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;
import com.freshplanet.ane.AirImagePicker.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class LoadRecentPhotosFunction extends BaseFunction
{
	
	
	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		super.call(context, args);

		int fetchLimit = getIntFromFREObject(args[0]);
		int maxImageWidth = getIntFromFREObject(args[1]);
		int maxImageHeight = getIntFromFREObject(args[2]);

		try {
			String[] projection = new String[]{
					MediaStore.Images.ImageColumns._ID,
					MediaStore.Images.ImageColumns.DATA,
					MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
					MediaStore.Images.ImageColumns.DATE_TAKEN,
					MediaStore.Images.ImageColumns.MIME_TYPE
			};
			final Cursor cursor = context.getActivity().getContentResolver()
					.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
							null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

			int fetchCount = cursor.getCount() > fetchLimit ? fetchLimit : cursor.getCount();

			JSONObject recentPhotosResult = new JSONObject();
			JSONArray recentPhotosArray = new JSONArray();

			for (int i = 0; i < fetchCount; i++) {
				cursor.moveToPosition(i);
				String imageLocation = cursor.getString(1);
				File imageFile = new File(imageLocation);
				if (imageFile.exists()) {
					Bitmap bitmap = BitmapFactory.decodeFile(imageLocation);
					bitmap = AirImagePickerUtils.resizeImage(bitmap, maxImageWidth, maxImageHeight);
					bitmap = AirImagePickerUtils.swapColors(bitmap);
					AirImagePickerExtensionContext.storeBitmap(imageLocation, bitmap);
					recentPhotosArray.put(imageLocation);

				}
			}

			recentPhotosResult.put("imagePaths", recentPhotosArray);

			context.dispatchStatusEventAsync(Constants.recentResult, recentPhotosResult.toString());

		} catch (Exception e) {
			context.dispatchStatusEventAsync("log", "loadRecentPhotos error "+ e.getLocalizedMessage());
		}

		return null;
	}
}
