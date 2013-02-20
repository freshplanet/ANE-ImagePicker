package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

public class PickerActivity extends Activity
{
	private String cameraOutputPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		int action = this.getIntent().getIntExtra("action", AirImagePickerExtensionContext.SELECT_IMAGE);
		
		Intent intent = AirImagePickerExtension.context.getIntentForAction(action);
		
		if (action == AirImagePickerExtensionContext.TAKE_PICTURE)
		{
			File cameraOutputFile = new File(Environment.getExternalStorageDirectory(), "air-image-picker_"+String.valueOf(System.currentTimeMillis())+".jpg"); 
			cameraOutputPath = cameraOutputFile.getAbsolutePath();
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraOutputFile));
		}
		
		startActivityForResult(intent, action);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		AirImagePickerExtensionContext context = AirImagePickerExtension.context;
		
		String imagePath = null;
		if (requestCode == AirImagePickerExtensionContext.SELECT_IMAGE)
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
		else if (requestCode == AirImagePickerExtensionContext.TAKE_PICTURE)
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
				AirImagePickerExtension.log("Image path: " + imagePath);
				
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
				AirImagePickerExtension.context.pickedImage = BitmapFactory.decodeFile(imagePath, options);
				
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
		
		finish();
	}
}
