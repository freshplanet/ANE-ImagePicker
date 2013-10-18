package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class CropActivity extends AirImagePickerActivity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Set crop input
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
		Intent intent = new Intent("com.android.camera.action.CROP");
		
		String cropInputPath = result.imagePath;
		intent.setDataAndType(Uri.fromFile(new File(cropInputPath)), "image/*");

		// Set crop output
		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		String cropOutputPath = tempFile.getAbsolutePath();

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
	}

}
