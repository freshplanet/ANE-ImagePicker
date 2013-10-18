package com.freshplanet.ane.AirImagePicker;

import android.content.Intent;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerActivity;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public class ImageCameraActivity extends AirImagePickerActivity {
	
	private void handleResultForImageCamera(Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] entering handleResultForImageCamera");
		if (parameters.shouldCrop && AirImagePickerUtils.isCropAvailable(this))
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
				if(!sendResultToContext("DID_FINISH_PICKING", "IMAGE")) {
					restartApp();
				}
			}
		}

		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] exiting handleResultForImageCamera");
		//deleteTemporaryImageFile(_cameraOutputPath);
	}

}
