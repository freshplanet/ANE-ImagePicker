package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

public class PickerActivity extends Activity
{
	private static final String CURRENT_ACTION = "currentAction";
	private static final String CAMERA_OUTPUT_PATH = "cameraOutputPath";
	
	private int currentAction = AirImagePickerExtensionContext.NO_ACTION;
	private String cameraOutputPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			cameraOutputPath = savedInstanceState.getString(CAMERA_OUTPUT_PATH);
			currentAction = savedInstanceState.getInt(CURRENT_ACTION);
		}
		
		if (currentAction == AirImagePickerExtensionContext.NO_ACTION)
		{
			currentAction = this.getIntent().getIntExtra("action", AirImagePickerExtensionContext.SELECT_IMAGE_ACTION);
			
			Intent intent = AirImagePickerExtension.context.getIntentForAction(currentAction);
			
			if (currentAction == AirImagePickerExtensionContext.TAKE_PICTURE_ACTION)
			{
				// Get or create folder for camera pictures
				File cameroOutputFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
				if (!cameroOutputFolder.exists())
				{
					cameroOutputFolder.mkdir();
					try
					{
						new File(cameroOutputFolder, ".nomedia").createNewFile();
					}
					catch (Exception e)
					{
						// Do nothing
					}
				}
				
				// Create camera output path
				File cameraOutputFile = new File(cameroOutputFolder, String.valueOf(System.currentTimeMillis())+".jpg");
				cameraOutputPath = cameraOutputFile.getAbsolutePath();
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraOutputFile));
			}
			
			startActivityForResult(intent, currentAction);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		savedInstanceState.putInt(CURRENT_ACTION, currentAction);
		savedInstanceState.putString(CAMERA_OUTPUT_PATH, cameraOutputPath);
		
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		AirImagePickerExtensionContext context = AirImagePickerExtension.context;
		
		String imagePath = null;
		if (requestCode == AirImagePickerExtensionContext.SELECT_IMAGE_ACTION)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				Uri imageUri = data.getData();
				
				// This converts the resulting Uri into an absolute file path
				String[] proj = { MediaStore.Images.Media.DATA };
		        Cursor cursor = getContentResolver().query(imageUri, proj, null, null, null);
		        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		        cursor.moveToFirst();
		        imagePath = cursor.getString(column_index);
			}
		}
		else if (requestCode == AirImagePickerExtensionContext.TAKE_PICTURE_ACTION)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				imagePath = cameraOutputPath;
			}
		}
		
		if (imagePath != null)
		{
			try
			{
				// Choose a sample size with an arbitrary memory limit
				int memoryLimit = 5 * 1024 * 1024; // 5MB
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(imagePath, options);
				int sampleSize = 1;
				while(options.outWidth/sampleSize * options.outHeight/sampleSize * 4 > memoryLimit)
					sampleSize *= 2;
				
				// Decode the image
				options.inJustDecodeBounds = false;
				options.inSampleSize = sampleSize;
				Bitmap rawImage = BitmapFactory.decodeFile(imagePath, options);
				
				// Fix image orientation
				ExifInterface exif = new ExifInterface(imagePath);
				int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
				int rotate = 0;
				switch (exifOrientation)
				{
		          case ExifInterface.ORIENTATION_ROTATE_90:
		        	  rotate = 90;
		        	  break; 
		          
		          case ExifInterface.ORIENTATION_ROTATE_180:
		        	  rotate = 180;
		        	  break;

		          case ExifInterface.ORIENTATION_ROTATE_270:
		        	  rotate = 270;
		        	  break;
				}
				if (rotate != 0)
				{
					int w = rawImage.getWidth();
					int h = rawImage.getHeight();
					
					Matrix mtx = new Matrix();
					mtx.preRotate(rotate);
					
					AirImagePickerExtension.context.pickedImage = Bitmap.createBitmap(rawImage, 0, 0, w, h, mtx, true);
				}
				else
				{
					AirImagePickerExtension.context.pickedImage = rawImage;
				}
				
				// Delete original file if it was taken by camera
				if (requestCode == AirImagePickerExtensionContext.TAKE_PICTURE_ACTION)
				{
					File originalFile = new File(imagePath);
					originalFile.delete();
				}
				
				// Dispatch finish event
				context.dispatchStatusEventAsync("DID_FINISH_PICKING", "OK");
			}
			catch (Exception exception)
			{
				AirImagePickerExtension.log(exception.getMessage());
				context.dispatchStatusEventAsync("DID_CANCEL", "OK");
			}
		}
		else
		{
			context.dispatchStatusEventAsync("DID_CANCEL", "OK");
		}
		
		currentAction = AirImagePickerExtensionContext.NO_ACTION;
		finish();
	}
}
