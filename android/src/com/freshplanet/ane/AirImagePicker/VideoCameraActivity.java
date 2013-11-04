package com.freshplanet.ane.AirImagePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

public class VideoCameraActivity extends ImagePickerActivityBase {
	
	
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
	protected void handleResult(Intent data)
	{
		Log.d(TAG, "Entering handleResultForVideoCamera");

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
				Log.e(TAG, e.getMessage());
				sendErrorToContext("ERROR_GENERATING_VIDEO", e.getMessage());
			} finally {
				try {
					if (inputstr != null)
						inputstr.close();
					if (outputstr != null)
						outputstr.close();
				} catch (Exception e2) {
					Log.e(TAG, e2.getMessage());
					sendErrorToContext("ERROR_GENERATING_VIDEO", e2.getMessage());
				}
			}
		}
		
		Log.d(TAG, "_cameraOutputPath = "+ result.videoPath);
		try {
			Bitmap thumbnail = AirImagePickerUtils.createThumbnailForVideo(result.videoPath);
			if(thumbnail.getWidth() > parameters.maxWidth || thumbnail.getHeight() > parameters.maxHeight) {
				thumbnail =  AirImagePickerUtils.resizeImage(thumbnail, parameters.maxWidth, parameters.maxHeight);
				
			}
			result.setPickedImage(thumbnail);
			
			result.imagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(result.getPickedImage());
			if((result.videoPath != null) && (result.imagePath != null) && (result.getPickedImage() != null)) {
				sendResultToContext("DID_FINISH_PICKING", "VIDEO");
			} else {
				sendErrorToContext("ERROR_GENERATING_VIDEO", "Result of taking video was missing a path or thumbnail");
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			sendErrorToContext("ERROR_GENERATING_VIDEO", "Error taking video: " + e.getMessage());
		}
		

		Log.d(TAG, "Exiting handleResultForVideoCamera");
	}

}


