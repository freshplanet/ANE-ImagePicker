package com.freshplanet.ane.AirImagePicker;

import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.content.res.Configuration;

public class AirImagePickerActivity extends Activity
{
	private static String TAG = "AirImagePicker";
	private String airPackageName;
	private String chatLink;
	private String mediaType;
	private String thumbnailPath;
	private String mediaPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onCreate");
		
		if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			chatLink = savedInstanceState.getString("chatLink");
			mediaType = savedInstanceState.getString("mediaType");
			thumbnailPath = savedInstanceState.getString("thumbnailPath");
			mediaPath = savedInstanceState.getString("mediaPath");
		}
		
		if(airPackageName == null) {                                
			airPackageName = AirImagePickerExtension.context.getActivity().getPackageName();	
			thumbnailPath = "/path/to/my/thumbnail.png";
			mediaPath = "/path/to/my/media.3gp";
		}
		
		super.onCreate(savedInstanceState);

		if(AirImagePickerExtension.context != null) {
			Log.d(TAG, "[AirImagePickerActivity] extension context exists");
			AirImagePickerExtension.context.onCreatePickerActivity(this);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] extension context is NULL !!");
		}
		Log.d(TAG, "[AirImagePickerActivity] Exiting onCreate");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onActivityResult");
		
		String action = data.getAction();
		if (action == MediaStore.ACTION_IMAGE_CAPTURE) {
			mediaType = "image";
		} else if (action == MediaStore.ACTION_VIDEO_CAPTURE) {
			mediaType = "video";
		}
		
		if(AirImagePickerExtension.context != null) {
			super.onActivityResult(requestCode, resultCode, data);
			AirImagePickerExtension.context.onPickerActivityResult(requestCode, resultCode, data);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] got result but context is gone: " + airPackageName);
			try {
				restartApp(requestCode, resultCode, data);
			} catch (URISyntaxException e) {
				Log.e(TAG, e.toString());
			}
		}
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onActivityResult");
	}
	
	private void restartApp(int requestCode, int resultCode, Intent data) throws URISyntaxException
	{
		Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(airPackageName);
		if(launchIntent != null) {
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Uri.Builder builder = new Uri.Builder();
			builder.scheme("hellopop");
			builder.path(chatLink);
			builder.appendQueryParameter("chatLink", chatLink);
			builder.appendQueryParameter("mediaType", mediaType);
			builder.appendQueryParameter("thumbnailPath", thumbnailPath);
			builder.appendQueryParameter("mediaPath", mediaPath);
			launchIntent.setData(builder.build());
	        startActivity(launchIntent);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] couldn't get intent to restart app");
		}
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
		outState.putString("airPackageName", airPackageName);
		outState.putString("chatLink", chatLink);
		outState.putString("mediaType", mediaType);
		outState.putString("thumbnailPath", thumbnailPath);
		outState.putString("mediaPath", mediaPath);
        super.onSaveInstanceState(outState);
        Log.d(TAG, "[AirImagePickerActivity] onSaveInstanceState" );

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			chatLink = savedInstanceState.getString("chatLink");
			mediaType = savedInstanceState.getString("mediaType");
			thumbnailPath = savedInstanceState.getString("thumbnailPath");
			mediaPath = savedInstanceState.getString("mediaPath");
		}
        Log.d(TAG, "[AirImagePickerActivity] onRestoreInstanceState" );
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
    	super.onConfigurationChanged(config);
    	Log.d(TAG, "[AirImagePickerActivity] onConfigurationChanged" );
    }
    
	
}
