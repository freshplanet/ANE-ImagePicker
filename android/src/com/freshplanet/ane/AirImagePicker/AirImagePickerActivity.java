package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.res.Configuration;

public class AirImagePickerActivity extends Activity
{
	private static String TAG = "AirImagePicker";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onCreate");
		
		super.onCreate(savedInstanceState);

		if(AirImagePickerExtension.context != null) {
			Log.d(TAG, "[AirImagePickerActivity] extension context exists");
		} else {
			Log.e(TAG, "[AirImagePickerActivity] extension context is NULL !!");
		}
		
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

	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "[AirImagePickerActivity] onSaveInstanceState" );

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "[AirImagePickerActivity] onRestoreInstanceState" );
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
    	Log.d(TAG, "[AirImagePickerActivity] onConfigurationChanged" );
    }
	
}
