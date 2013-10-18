package com.freshplanet.ane.AirImagePicker;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ImagePickerResult implements Parcelable {
	
	public static final String MEDIA_TYPE_IMAGE = "image";
	public static final String MEDIA_TYPE_VIDEO = "video";
	public static final String SOURCE_GALLERY = "gallery";
	public static final String SOURCE_CAMERA = "camera";

	public String scheme = "airimagepicker";
	public String baseUri;
	public String mediaType;
	public String source;
	private int imageWidth;
	private int imageHeight;
	public String imagePath;
	public String videoPath;
	public String errorType;
	public String errorMessage;
	
	//This member is not serialized
	private Bitmap pickedImage;
	
	public static final Parcelable.Creator<ImagePickerResult> CREATOR = new Parcelable.Creator<ImagePickerResult>() {
		public ImagePickerResult createFromParcel(Parcel in) {
		    return new ImagePickerResult(in);
		}
		
		public ImagePickerResult[] newArray(int size) { 
		    return new ImagePickerResult[size];
		}
	};
	
	private ImagePickerResult(Parcel in) {
		scheme = in.readString();
		baseUri = in.readString();
		mediaType = in.readString();
		source = in.readString();
		imageWidth = in.readInt();
		imageHeight = in.readInt();
		imagePath = in.readString();
		videoPath = in.readString();
		errorType = in.readString();
		errorMessage = in.readString();
	}
	
	public ImagePickerResult(String scheme, String baseUri, String mediaType, String source, int imageWidth, int imageHeight) {
		this.scheme = scheme;
		this.baseUri = baseUri;
		this.mediaType = mediaType;
		this.source = source;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
	}
	
	public ImagePickerResult(String scheme, String baseUri, String mediaType) {
		this(scheme, baseUri, mediaType, null, -1, -1);
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(scheme);
		dest.writeString(baseUri);
		dest.writeString(mediaType);
		dest.writeString(source);
		dest.writeInt(imageWidth);
		dest.writeInt(imageHeight);
		dest.writeString(imagePath);
		dest.writeString(videoPath);
		dest.writeString(errorType);
		dest.writeString(errorMessage);
	}
	
	public Bitmap getPickedImage() { return pickedImage; }
	
	public void setPickedImage(Bitmap image) 
	{
		pickedImage = image;
		imageWidth = image.getWidth();
		imageHeight = image.getHeight();
	}
	
	public Uri toUri() 
	{
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(scheme);
		builder.path(baseUri);
		if(mediaType != null) {
			builder.appendQueryParameter("mediaType", mediaType);
		}
		if(source != null) {
			builder.appendQueryParameter("source", source);
		}
		if(imageWidth != -1) {
			builder.appendQueryParameter("imageWidth", String.valueOf(imageWidth));
		}
		if(imageHeight != -1) {
			builder.appendQueryParameter("imageHeight", String.valueOf(imageHeight));
		}
		if(imagePath != null) {
			builder.appendQueryParameter("imagePath", imagePath);
		}
		if(videoPath != null) {
			builder.appendQueryParameter("videoPath", videoPath);
		}
		if(errorType != null) {
			builder.appendQueryParameter("videoPath", errorType);
		}
		if(errorMessage != null) {
			builder.appendQueryParameter("videoPath", errorMessage);
		}
		return builder.build();
	}
	
}
