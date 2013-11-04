package com.freshplanet.ane.AirImagePicker;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ImagePickerParameters implements Parcelable {
	
	public final String MEDIA_TYPE = "mediaType";
	public final String SCHEME = "scheme";
	public final String BASE_URI = "baseUri";
	public final String SHOULD_CROP = "shouldCrop";
	public final String MAX_WIDTH = "maxWidth";
	public final String MAX_HEIGHT = "maxHeight";
	public final String ALBUM_NAME = "albumName";
	
	public String mediaType;
	public String scheme;
	public String baseUri;
	public Boolean shouldCrop;
	public int maxWidth;
	public int maxHeight;
	public String albumName;

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
		baseUri = b.getString(BASE_URI);
		shouldCrop = b.getBoolean(SHOULD_CROP);
		maxWidth = b.getInt(MAX_WIDTH, -1);
		maxHeight = b.getInt(MAX_HEIGHT, -1);
		albumName = b.getString(ALBUM_NAME);
	}
	
	
	public ImagePickerParameters(String scheme, String baseUri, Boolean shouldCrop, int maxWidth, int maxHeight, String albumName) {
		this.scheme = scheme;
		this.baseUri = baseUri;
		this.shouldCrop = shouldCrop;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.albumName = albumName;
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
		b.putString(BASE_URI, baseUri);
		b.putBoolean(SHOULD_CROP, shouldCrop );
		b.putInt(MAX_WIDTH, maxWidth);
		b.putInt(MAX_HEIGHT, maxHeight);
		b.putString(ALBUM_NAME, albumName);
		dest.writeBundle(b);
	}

}
