package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.ImagePickerActivityBase;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public class ImageCameraActivity extends ImagePickerActivityBase {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		
		
		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		result.imagePath = tempFile.getAbsolutePath();
		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
		
		startActivityForResult(intent, AirImagePickerUtils.CAMERA_IMAGE_ACTION);
		
	}
	
	
	@Override
	protected void handleResult(Intent data)
	{
		Log.d(TAG, "[ImageCameraActivity] entering handleResult");
		if (parameters.shouldCrop && AirImagePickerUtils.isCropAvailable(this))
		{
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
				sendErrorToContext("PICKING_ERROR", "Failed to load image");
			}
		}

		Log.d(TAG, "[ImageCameraActivity] exiting handleResultForImageCamera");
		//deleteTemporaryImageFile(_cameraOutputPath);
	}

}
