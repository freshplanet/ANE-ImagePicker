package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AirImagePickerActivity extends Activity
{
	private static String TAG = "AirImagePicker";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onCreate");
		
		super.onCreate(savedInstanceState);		
		
		AirImagePickerExtension.context.onCreatePickerActivity(this);
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onCreate");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onActivityResult");
		
		super.onActivityResult(requestCode, resultCode, data);
		
		AirImagePickerExtension.context.onPickerActivityResult(requestCode, resultCode, data);
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onActivityResult");
	}
	
	@Override
	protected void onDestroy()
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onDestroy");
		
		super.onDestroy();
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onDestroy");
	}
	
}
