package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.ImagePickerActivityBase;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public class GalleryActivity extends ImagePickerActivityBase {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		Intent intent;
		int action;
		if(result.mediaType.equals(ImagePickerResult.MEDIA_TYPE_IMAGE)) {
			intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			action = AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION;
		} else {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*");
			action = AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION;
		}
		startActivityForResult(intent, action);
			
	}
	
	
	@Override
	protected void handleResult(Intent data)
	{
		Log.d(TAG, "[GalleryActivity] Entering handleResultForGallery");
		
		Uri selectedImageUri = data.getData();
		
		result.imagePath = getPath(selectedImageUri);
		
		Log.d(TAG, "[GalleryActivity] selectedImagePath = " + result.imagePath);
		
		if (AirImagePickerUtils.isPicasa(result.imagePath))
		{
			sendErrorToContext("PICASSA_NOT_SUPPORTED");
			return;
		}
		
		Boolean isVideo = getContentResolver().getType(selectedImageUri).indexOf("video") != -1;
		
		if ( isVideo )
		{
			try {
				result.videoPath = result.imagePath; 
				result.setPickedImage(AirImagePickerUtils.createThumbnailForVideo(result.imagePath));
				result.imagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(result.getPickedImage());
				if((result.videoPath != null) && (result.imagePath != null) && (result.getPickedImage() != null)) {
					sendResultToContext("DID_FINISH_PICKING", "VIDEO");
				} else {
					sendErrorToContext("PICKING_ERROR", "Error picking video: path or thumbnail wasn't set");
				}
			} catch (Exception e) {
				Log.e(TAG, "[GalleryActivity] error picking video: " + e.getMessage());
				e.printStackTrace();
				sendErrorToContext("PICKING_ERROR", "Error picking video: " + e.getMessage() );
			}
			
		}
		else
		{
			if (result.imagePath != null) {
				if(parameters.shouldCrop && AirImagePickerUtils.isCropAvailable(this)) {
					finish();
					doCrop();
				}
				else
				{
					SavedBitmap savedImage = orientAndSaveImage(result.imagePath, parameters.maxWidth, parameters.maxHeight, parameters.albumName);
					
					if(savedImage != null) {
						result.setPickedImage(savedImage.bitmap);
						result.imagePath = savedImage.path;
						sendResultToContext("DID_FINISH_PICKING", "IMAGE");
					} else {
						sendErrorToContext("PICKING_ERROR", "Failed to crop image");
					}
				}
			} else {
				sendErrorToContext("PICKING_ERROR", "Image picker didn't return a path");
			}
		}
		
		Log.d(TAG, "[GalleryActivity] Exiting handleResultForGallery");
	}


	private String getPath(Uri selectedImage)
	{
		final String[] filePathColumn = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
		Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
		
		// Some devices return an URI of com.android instead of com.google.android
		if (selectedImage.toString().startsWith("content://com.android.gallery3d.provider"))
		{
			selectedImage = Uri.parse( selectedImage.toString().replace("com.android.gallery3d", "com.google.android.gallery3d") );
		}
	
		if (cursor != null)
		{
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(MediaColumns.DATA);
			
			// if it is a picassa image on newer devices with OS 3.0 and up
			if (AirImagePickerUtils.isPicasa(selectedImage.toString()))
			{
				columnIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
				return selectedImage.toString();
			}
			else
			{
				return cursor.getString(columnIndex);
			}
		}
		else if ( selectedImage != null && selectedImage.toString().length() > 0 )
		{
			return selectedImage.toString();
		}
		else return null;
	}

}




