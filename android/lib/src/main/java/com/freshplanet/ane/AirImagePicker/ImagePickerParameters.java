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

package com.freshplanet.ane.AirImagePicker;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ImagePickerParameters implements Parcelable {

	public static String SCHEME_RECENT_PHOTOS = "recentPhotosScheme";
	public static String SCHEME_GALLERY = "galleryScheme";
	public static String SCHEME_CAMERA = "cameraScheme";

	public final String MEDIA_TYPE = "mediaType";
	public final String SCHEME = "scheme";

	public final String SHOULD_CROP = "shouldCrop";
	public final String MAX_WIDTH = "maxWidth";
	public final String MAX_HEIGHT = "maxHeight";
	public final String LIMIT = "limit";

	
	public String mediaType;
	public String scheme;
	public Boolean shouldCrop;
	public int maxWidth;
	public int maxHeight;
	public int limit;


	public static final Parcelable.Creator<ImagePickerParameters> CREATOR = new Parcelable.Creator<ImagePickerParameters>() {
		public ImagePickerParameters createFromParcel(Parcel in) {
		    return new ImagePickerParameters(in);
		}
		
		public ImagePickerParameters[] newArray(int size) {
		    return new ImagePickerParameters[size];
		}
	};
	
	private ImagePickerParameters(Parcel in) {
		Bundle b = in.readBundle();
		mediaType = b.getString(MEDIA_TYPE);
		scheme = b.getString(SCHEME);
		shouldCrop = b.getBoolean(SHOULD_CROP);
		maxWidth = b.getInt(MAX_WIDTH, -1);
		maxHeight = b.getInt(MAX_HEIGHT, -1);
		limit = b.getInt(LIMIT, 1);

	}
	
	
	public ImagePickerParameters(String scheme, Boolean shouldCrop, int maxWidth, int maxHeight, int limit) {
		this.scheme = scheme;
		this.shouldCrop = shouldCrop;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.limit = limit;

	}
	

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle b = new Bundle();
		b.putString(MEDIA_TYPE, mediaType);
		b.putString(SCHEME, scheme);
		b.putBoolean(SHOULD_CROP, shouldCrop );
		b.putInt(MAX_WIDTH, maxWidth);
		b.putInt(MAX_HEIGHT, maxHeight);
		b.putInt(LIMIT, limit);
		dest.writeBundle(b);
	}

}
