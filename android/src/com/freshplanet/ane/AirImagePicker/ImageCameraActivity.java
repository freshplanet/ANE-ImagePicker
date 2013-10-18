package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerActivity;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public class ImageCameraActivity extends AirImagePickerActivity {
	
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
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
				if(sendResultToContext("DID_FINISH_PICKING", "IMAGE")) {
					super.onActivityResult(requestCode, resultCode, data);
				} else {
					restartApp();
				}
			}
		}

		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] exiting handleResultForImageCamera");
		//deleteTemporaryImageFile(_cameraOutputPath);
	}

}
