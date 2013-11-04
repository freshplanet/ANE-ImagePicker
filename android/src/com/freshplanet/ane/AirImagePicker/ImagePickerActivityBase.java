package com.freshplanet.ane.AirImagePicker;

import java.io.File;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;

public abstract class ImagePickerActivityBase extends Activity
{
	public static final String TAG = "AirImagePicker";
	public static final String PARAMETERS = ":parameters";
	public static final String RESULT = ":result";
	
	protected String airPackageName;
	
	protected ImagePickerParameters parameters;
	protected ImagePickerResult result;
	
	protected int currentAction = AirImagePickerUtils.NO_ACTION;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "[ImagePickerActivityBase] Entering onCreate");
		if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			savedInstanceState.setClassLoader(ImagePickerParameters.class.getClassLoader());
			parameters = savedInstanceState.getParcelable(PARAMETERS);
			savedInstanceState.setClassLoader(ImagePickerResult.class.getClassLoader());
			result = savedInstanceState.getParcelable(RESULT);
		}
		
		if(airPackageName == null) {
			airPackageName = this.getPackageName();
			Log.d(TAG, "[ImagePickerActivityBase] my package name:" + getPackageName());
		}
		
		if(parameters == null) {
			Bundle b = this.getIntent().getBundleExtra(airPackageName + PARAMETERS);
			b.setClassLoader(ImagePickerParameters.class.getClassLoader());
			parameters = (ImagePickerParameters)b.getParcelable(PARAMETERS);
		}
		
		if(result == null) {
			String resultKey = airPackageName + RESULT;
			if(getIntent().hasExtra(resultKey)) {
				Bundle b = getIntent().getBundleExtra(airPackageName + RESULT);
				b.setClassLoader(ImagePickerResult.class.getClassLoader());
				result = (ImagePickerResult)b.getParcelable(RESULT);
			} else {
				result = new ImagePickerResult(parameters.scheme, parameters.baseUri, parameters.mediaType);
			}
		}
		
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "[ImagePickerActivityBase] Exiting onCreate");
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == Activity.RESULT_OK)
		{
			handleResult(data);
		}
		else
		{
			sendResultToContext("DID_CANCEL", "");
		}
		
		if(!this.isFinishing()) {
			this.finish();
		}
		
	}
	
	@Override
	public void startActivityForResult(Intent intent, int requestCode) 
	{
		currentAction = requestCode;
		super.startActivityForResult(intent, requestCode);
	}
	
	protected abstract void handleResult(Intent data);
	
	protected AirImagePickerExtensionContext getExtensionContext() {
		return AirImagePickerExtension.context;
	}
	
	protected void sendErrorToContext(String code) 
	{
		sendErrorToContext(code, "");
	}
	
	
	protected void sendResultToContext(String code) 
	{
		sendResultToContext(code, "");
	}
	
	protected void sendErrorToContext(String code, String level) 
	{
		result.errorType = code;
		result.errorMessage = level;
		sendResultToContext(code, level);
	}
	
	protected void sendResultToContext(String code, String level)
	{
		if(AirImagePickerExtension.context == null) {
			restartApp();
		} else {
			AirImagePickerExtensionContext context = AirImagePickerExtension.context;
			if(result.getPickedImage() != null) {
				context.setImagePath(result.imagePath);
				context.setVideoPath(result.videoPath);
				context.setPickedImage(result.getPickedImage());
			}
			context.dispatchResultEvent(code, level);
		}
	}
	
	
	protected void restartApp()
	{
		Intent launchIntent = this.getPackageManager().getLaunchIntentForPackage(airPackageName);
		if(launchIntent != null) {
			launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(result.getPickedImage() != null || result.errorType != null) {
				launchIntent.setData(result.toUri());
			}
	        startActivity(launchIntent);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] couldn't get intent to restart app");
		}
	}

	@Override
    protected void onSaveInstanceState(Bundle outState) {
		outState.putString("airPackageName", airPackageName);
		outState.putParcelable(PARAMETERS, parameters);
		outState.putParcelable(RESULT, result);
        super.onSaveInstanceState(outState);
        Log.d(TAG, "[ImagePickerActivityBase] onSaveInstanceState" );
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
			airPackageName = savedInstanceState.getString("airPackageName");
			
			if(savedInstanceState.containsKey(RESULT)) {
				savedInstanceState.setClassLoader(ImagePickerResult.class.getClassLoader());
				result = savedInstanceState.getParcelable(RESULT);
			}
			if(savedInstanceState.containsKey(PARAMETERS)) {
				savedInstanceState.setClassLoader(ImagePickerParameters.class.getClassLoader());
				parameters = savedInstanceState.getParcelable(PARAMETERS);
			}
			
			
		}
        Log.d(TAG, "[ImagePickerActivityBase] onRestoreInstanceState" );
    }

    
    protected void doCrop() {
    	if(parameters.shouldCrop && (result.imagePath != null) && AirImagePickerUtils.isCropAvailable(this)) {
	    	Intent intent = new Intent(getApplicationContext(), CropActivity.class);
	    	Bundle b = new Bundle();
	    	b.putParcelable(PARAMETERS, parameters);
	    	intent.putExtra(airPackageName + PARAMETERS, b);
	    	b = new Bundle();
	    	b.putParcelable(RESULT, result);
	    	intent.putExtra(airPackageName + RESULT, b);
			startActivity(intent);
    	}
    }

	protected SavedBitmap orientAndSaveImage(String filePath, int maxWidth, int maxHeight, String albumName )
	{
		Log.d(TAG, "[ImagePickerActivityBase] Entering orientAndSaveImage");
		
	
		Bitmap image = AirImagePickerUtils.getOrientedSampleBitmapFromPath(filePath);
		if(image == null) {
			Log.e(TAG, "[ImagePickerActivityBase] getOrientedSampleBitmapFromPath() returned null for " + filePath);
			return null;
		}
		
		if(albumName != null) {
			File savedPicture = AirImagePickerUtils.savePictureInGallery(albumName, AirImagePickerUtils.getJPEGRepresentationFromBitmap(image));
			if(savedPicture != null) {
				notifyGalleryOfNewImage(savedPicture);
			}
		}
		
		image = AirImagePickerUtils.resizeImage(image, maxWidth, maxHeight);
		String outputPath = AirImagePickerUtils.saveImageToTemporaryDirectory(image);
		
		Log.d(TAG, "[ImagePickerActivityBase] Exiting orientAndSaveImage");
		
		return new SavedBitmap(image, outputPath);
	}

	protected void notifyGalleryOfNewImage(File picture)
	{
		long current = System.currentTimeMillis();
		Log.d(TAG, "[ImagePickerActivityBase] Entering notifyGalleryOfNewImage");
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, "My HelloPop Image " + current);
		values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current/1000));
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.Media.DATA, picture.getAbsolutePath());
		ContentResolver contentResolver = getContentResolver();
		Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		Uri newUri = contentResolver.insert(base, values);
		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
		Log.d(TAG, "[ImagePickerActivityBase] Exiting notifyGalleryOfNewImage");
	}
	
}
