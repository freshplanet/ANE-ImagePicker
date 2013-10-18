package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

public class AirImagePickerActivity extends Activity
{
	public static final String TAG = "AirImagePicker";
	
	protected String airPackageName;
	
	protected ImagePickerParameters parameters;
	protected ImagePickerResult result;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onCreate");
		if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			parameters = savedInstanceState.getParcelable("parameters");
			result = savedInstanceState.getParcelable("result");
		}
		
		if(airPackageName == null) {                                
			airPackageName = AirImagePickerExtension.context.getActivity().getPackageName();	
		}
		
		if(parameters == null) {
			parameters = this.getIntent().getParcelableExtra(this.airPackageName + ":parameters");
		}
		
		if(result == null) {
			String resultKey = airPackageName + ":result";
			if(getIntent().hasExtra(resultKey)) {
				result = getIntent().getParcelableExtra(resultKey);
			} else {
				result = new ImagePickerResult(parameters.scheme, parameters.baseUri);
			}
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
	
	@Override public void startActivityForResult(Intent intent, int requestCode) 
	{
		
		if(requestCode == AirImagePickerUtils.CAMERA_IMAGE_ACTION) {
			result.mediaType = ImagePickerResult.MEDIA_TYPE_IMAGE;
		} else if (requestCode == AirImagePickerUtils.CAMERA_VIDEO_ACTION) {
			result.mediaType = ImagePickerResult.MEDIA_TYPE_VIDEO;
		}
		result.videoPath = AirImagePickerExtension.context.getImagePath();
		super.startActivityForResult(intent, requestCode);
	}
	
	protected AirImagePickerExtensionContext getExtensionContext() {
		return AirImagePickerExtension.context;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onActivityResult");
		
		if(AirImagePickerExtension.context != null) {
			super.onActivityResult(requestCode, resultCode, data);
			AirImagePickerExtension.context.onPickerActivityResult(requestCode, resultCode, data);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] got result but context is gone: " + airPackageName);
			restartApp();
		}
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onActivityResult");
	}
	
	protected Boolean sendResultToContext(String code) 
	{
		return sendResultToContext(code, null);
	}
	
	protected Boolean sendResultToContext(String code, String level)
	{
		if(AirImagePickerExtension.context != null) {
			//TODO transfer properties of "result" to context here
			AirImagePickerExtension.context.dispatchResultEvent(code, level);
			return true;
		}
		return false;
	}
	
	protected void setFieldsOnContext() 
	{
		if(AirImagePickerExtension.context != null) {
			
		}
	}
	
	protected void restartApp()
	{
		Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(airPackageName);
		if(launchIntent != null) {
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			launchIntent.setData(result.toUri());
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
		outState.putParcelable("parameters", parameters);
		outState.putParcelable("result", result);
        super.onSaveInstanceState(outState);
        Log.d(TAG, "[AirImagePickerActivity] onSaveInstanceState" );
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			result = savedInstanceState.getParcelable("result");
		}
        Log.d(TAG, "[AirImagePickerActivity] onRestoreInstanceState" );
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
    	super.onConfigurationChanged(config);
    	Log.d(TAG, "[AirImagePickerActivity] onConfigurationChanged" );
    }
    
    protected void doCrop() {
    	if(parameters.shouldCrop && (result.imagePath != null) && AirImagePickerUtils.isCropAvailable(this)) {
	    	Intent intent = new Intent(getApplicationContext(), CropActivity.class);
	    	intent.putExtra(airPackageName + ":parameters", parameters);
	    	intent.putExtra(airPackageName = ":result", result);
			startActivity(intent);
    	}
    }
	
}
