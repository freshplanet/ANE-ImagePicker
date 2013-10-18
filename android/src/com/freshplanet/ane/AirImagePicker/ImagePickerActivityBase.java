package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.Log;

public abstract class ImagePickerActivityBase extends Activity
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
				result = new ImagePickerResult(parameters.scheme, parameters.baseUri, parameters.mediaType);
			}
		}
		
		super.onCreate(savedInstanceState);

		if(AirImagePickerExtension.context == null) {
			Log.e(TAG, "[AirImagePickerActivity] extension context has died");
		}
		
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onCreate");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == RESULT_CANCELED) {
			this.finish();
		} else {
			handleResult(data);
		}
	}
	
	protected abstract void handleResult(Intent data);
	
	protected AirImagePickerExtensionContext getExtensionContext() {
		return AirImagePickerExtension.context;
	}
	
	
	protected void sendResultToContext(String code) 
	{
		sendResultToContext(code, null);
	}
	
	protected void sendResultToContext(String code, String level)
	{
		if(AirImagePickerExtension.context == null) {
			restartApp();
		} else if (result.getPickedImage() != null) {
			AirImagePickerExtensionContext context = AirImagePickerExtension.context;
			context.setImagePath(result.imagePath);
			context.setVideoPath(result.videoPath);
			context.setPickedImage(result.getPickedImage());
			context.dispatchResultEvent(code, level);
		}
		finish();
	}
	
	
	protected void restartApp()
	{
		Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(airPackageName);
		if(launchIntent != null) {
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(result.getPickedImage() != null) {
				launchIntent.setData(result.toUri());
			}
	        startActivity(launchIntent);
	        finish();
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

	protected SavedBitmap orientAndSaveImage(String filePath, int maxWidth, int maxHeight, String albumName )
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering processPickedImage");
		
	
		Bitmap image = AirImagePickerUtils.getOrientedSampleBitmapFromPath(filePath);
	
		if(albumName != null) {
			File savedPicture = AirImagePickerUtils.savePictureInGallery(albumName, AirImagePickerUtils.getJPEGRepresentationFromBitmap(image));
			if(savedPicture != null) {
				notifyGalleryOfNewImage(savedPicture);
			}
		}
		
		image = AirImagePickerUtils.resizeImage(image, maxWidth, maxHeight);
		String outputPath = AirImagePickerUtils.saveImageToTemporaryDirectory(image);
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting processPickedImage");
		
		return new AirImagePickerUtils.SavedBitmap(image, outputPath);
	}

	protected void notifyGalleryOfNewImage(File picture)
	{
		long current = System.currentTimeMillis();
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering notifyGalleryOfNewImage");
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, "My HelloPop Image " + current);
		values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current/1000));
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.Media.DATA, picture.getAbsolutePath());
		ContentResolver contentResolver = getContentResolver();
		Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		Uri newUri = contentResolver.insert(base, values);
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting notifyGalleryOfNewImage");
	}
	
}
