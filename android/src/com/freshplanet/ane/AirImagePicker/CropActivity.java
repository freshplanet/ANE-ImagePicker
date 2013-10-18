package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class CropActivity extends ImagePickerActivityBase {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Set crop input
		
		Log.d(TAG, "[CropActivity] Exiting getIntentForAction");
		Intent intent = new Intent("com.android.camera.action.CROP");
		
		String cropInputPath = result.imagePath;
		intent.setDataAndType(Uri.fromFile(new File(cropInputPath)), "image/*");

		// Set crop output
		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		result.imagePath = tempFile.getAbsolutePath();

		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

		// Cropped image should be square (aspect ratio 1:1)
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);

		// Set crop output size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(cropInputPath, options);
		int smallestEdge = Math.min(options.outWidth, options.outHeight);
		intent.putExtra("outputX", smallestEdge);
		intent.putExtra("outputY", smallestEdge);
		startActivityForResult(intent, AirImagePickerUtils.CROP_ACTION);
	}
	
	@Override
	protected void handleResult(Intent data)
	{
		Log.d(TAG, "[CropActivity] Entering handleResultForCrop");
		
		if(data.hasExtra(MediaStore.EXTRA_OUTPUT)) {
			Uri extraImageUri = (Uri)data.getExtras().get(MediaStore.EXTRA_OUTPUT);
			if(extraImageUri != null) {
				result.imagePath = extraImageUri.getPath();
				Log.d(TAG, "[CropActivity] changing selectedImagePath to: " + result.imagePath);
			}
		}
		
		parameters.albumName = null;
		
		SavedBitmap savedImage = orientAndSaveImage(result.imagePath, parameters.maxWidth, parameters.maxHeight, parameters.albumName);
		
		if(savedImage != null) {
			result.setPickedImage(savedImage.bitmap);
			result.imagePath = savedImage.path;
			sendResultToContext("DID_FINISH_PICKING", "IMAGE");
		} else {
			sendErrorToContext("PICKING_ERROR", "Failed to crop image");
		}

		Log.d(TAG, "[CropActivity] Exiting handleResultForCrop");
	}

}
