package com.freshplanet.ane.AirImagePicker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerActivity;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public class GalleryActivity extends AirImagePickerActivity {
	
	private void handleResultForGallery(Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering handleResultForGallery");
		
		Uri selectedImageUri = data.getData();
		
		result.imagePath = AirImagePickerUtils.getPath(this, selectedImageUri);
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] selectedImagePath = " + result.imagePath);
		
		if (AirImagePickerUtils.isPicasa(result.imagePath))
		{
			if(!sendResultToContext("PICASSA_NOT_SUPPORTED")) 
			{
				restartApp();
			}
			return;
		}
		
		Boolean isVideo = getContentResolver().getType(selectedImageUri).indexOf("video") != -1;
		
		if ( isVideo )
		{
			result.videoPath = result.imagePath; 
			result.pickedImage = AirImagePickerUtils.createThumbnailForVideo(result.imagePath);
			result.imagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(result.pickedImage);
			if(!sendResultToContext("DID_FINISH_PICKING", "VIDEO")) {
				restartApp();
			}
		}
		else
		{
			if ((result.imagePath != null) && parameters.shouldCrop && AirImagePickerUtils.isCropAvailable(this))
			{
				finish();
				doCrop();
			}
			else
			{
				SavedBitmap savedImage = AirImagePickerUtils.orientAndSaveImage(this, result.imagePath, parameters.maxWidth, parameters.maxHeight, parameters.albumName);
				
				if(savedImage != null) {
					result.pickedImage = savedImage.bitmap;
					result.imagePath = savedImage.path;
					sendResultToContext("DID_FINISH_PICKING", "IMAGE");
				}
			}
		}
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting handleResultForGallery");
	}

}




