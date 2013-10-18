package com.freshplanet.ane.AirImagePicker;

import android.os.Parcel;
import android.os.Parcelable;

public class ImagePickerParameters implements Parcelable {
	
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
		mediaType = in.readString();
		scheme = in.readString();
		baseUri = in.readString();
		shouldCrop = in.readByte() > (byte)0 ? true : false;
		maxWidth = in.readInt();
		maxHeight = in.readInt();
		albumName = in.readString();
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
		dest.writeString(mediaType);
		dest.writeString(scheme);
		dest.writeString(baseUri);
		dest.writeByte(shouldCrop ? (byte)1 : (byte)0);
		dest.writeInt(maxWidth);
		dest.writeInt(maxHeight);
		dest.writeString(albumName);
	}

}
