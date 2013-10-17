package com.freshplanet.ane.AirImagePicker;

import android.os.Parcel;
import android.os.Parcelable;

public class ImagePickerParameters implements Parcelable {
	
	public Boolean shouldCrop;
	public int maxWidth;
	public int maxHeight;
	public String baseUri;
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
		shouldCrop = in.readInt() > 0 ? true : false;
		maxWidth = in.readInt();
		maxHeight = in.readInt();
		baseUri = in.readString();
		albumName = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(shouldCrop ? 1 : 0);
		dest.writeInt(maxWidth);
		dest.writeInt(maxHeight);
		dest.writeString(baseUri);
		dest.writeString(albumName);
	}

}
