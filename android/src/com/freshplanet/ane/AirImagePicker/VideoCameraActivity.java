package com.freshplanet.ane.AirImagePicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
		File outputFile;
		
		if(parameters.albumName != null) {
			File folder = AirImagePickerUtils.getAlbumFolder(parameters.albumName);
			outputFile = new File(folder, "VID_" + String.valueOf(System.currentTimeMillis()) + ".3gp");
		} else {
			outputFile = AirImagePickerUtils.getTemporaryFile(".3gp");
		}
		
		result.videoPath = outputFile.getAbsolutePath();
		
		if (shouldUseExtraOutput())
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
		intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 180);
		intent.putExtra("android.intent.extra.sizeLimit", 20 * 1024 * 8);
		
		startActivityForResult(intent, AirImagePickerUtils.CAMERA_VIDEO_ACTION);
	}
	
	private static  boolean shouldUseExtraOutput()
	{
		// Products later than honeycomb usually respect EXTRA_OUTPUT - except Samsung Galaxy Tab,
		// which never returns a result from the camera intent if you use it. 
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) && 
				!(Build.BRAND.equals("samsung") && (Build.MODEL.startsWith("GT-P") || Build.MODEL.startsWith("SM-T")));
	}
	
	@Override
	protected void handleResult(Intent data)
	{
		Log.d(TAG, "Entering handleResultForVideoCamera");

		// EXTRA_OUTPUT doesn't work on some 2.x phones and Galaxy Tab, copy manually to the
		// desired path
		if (!shouldUseExtraOutput()) {

			FileInputStream inputstr = null;
			FileOutputStream outputstr = null;
			File outputFile = null;
			File inputFile = null;
			try {
				outputFile = new File(result.videoPath);
				inputFile = new File(data.getData().getPath());
				if(!inputFile.renameTo(outputFile)) {
					AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
					inputstr = videoAsset.createInputStream();
					outputstr = new FileOutputStream(outputFile);
					
					FileChannel inChannel = inputstr.getChannel();
				    FileChannel outChannel = outputstr.getChannel();
				    try
				    {
				        inChannel.transferTo(0, inChannel.size(), outChannel);
				    }
				    finally
				    {
				        if (inChannel != null)
				            inChannel.close();
				        if (outChannel != null)
				            outChannel.close();
				    }
					new File(data.getData().getPath()).delete();
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


