package com.freshplanet.ane.AirImagePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class VideoCameraActivity extends AirImagePickerActivity {
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		
		File tempFile = AirImagePickerUtils.getTemporaryFile(".3gp");
		result.videoPath = tempFile.getAbsolutePath();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

		startActivityForResult(intent, AirImagePickerUtils.CAMERA_VIDEO_ACTION);
		
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "Entering handleResultForVideoCamera");

		// EXTRA_OUTPUT doesn't work on some 2.x phones, copy manually to the
		// desired path
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			FileInputStream inputstr = null;
			FileOutputStream outputstr = null;
			File outputFile = null;
			try {
				outputFile = new File(result.videoPath);
				AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
				inputstr = videoAsset.createInputStream();
				outputstr = new FileOutputStream(outputFile);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = inputstr.read(buffer)) > 0) {
					outputstr.write(buffer, 0, length);
				}
			} catch (IOException e) {
				// TODO: handle error
				Log.e(AirImagePickerUtils.TAG, e.getMessage());
			} finally {
				try {
					if (inputstr != null)
						inputstr.close();
					if (outputstr != null)
						outputstr.close();
				} catch (Exception e2) {
					// TODO: handle exception
					Log.e(AirImagePickerUtils.TAG, e2.getMessage());
				}
			}
		}
		
		Log.d(AirImagePickerUtils.TAG, "_cameraOutputPath = "+ result.videoPath);

		result.pickedImage = AirImagePickerUtils.createThumbnailForVideo(result.videoPath);
		result.imagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(result.pickedImage);
		
		if(sendResultToContext("DID_FINISH_PICKING", "VIDEO")) {
			super.onActivityResult(requestCode, resultCode, data);
		} else {
			restartApp();
		}

		Log.d(AirImagePickerUtils.TAG, "Exiting handleResultForVideoCamera");
	}

}


