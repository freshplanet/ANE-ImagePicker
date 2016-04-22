//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//
//  Copyright 2016 VoiceThread (https://voicethread.com/)
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//    http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//////////////////////////////////////////////////////////////////////////////////////


package com.freshplanet.ane.AirImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ClipData;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.os.Build;

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayCameraFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayImagePickerFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayOverlayFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsCameraAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsImagePickerAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveOverlayFunction;

public class AirImagePickerExtensionContext extends FREContext 
{
	private static String TAG = "AirImagePicker";
	
	@Override
	public void dispose() 
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering dispose");
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Setting AirImagePickerExtension.context to null.");

		AirImagePickerExtension.context = null;
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting dispose");
	}

	//-----------------------------------------------------//
	//					EXTENSION API					   //
	//-----------------------------------------------------//

	@Override
	public Map<String, FREFunction> getFunctions() 
	{
		Map<String, FREFunction> functions = new HashMap<String, FREFunction>();

		functions.put("isImagePickerAvailable", new IsImagePickerAvailableFunction());
		functions.put("displayImagePicker", new DisplayImagePickerFunction());
		functions.put("isCameraAvailable", new IsCameraAvailableFunction());
		functions.put("displayCamera", new DisplayCameraFunction());
		functions.put("displayOverlay", new DisplayOverlayFunction()); // not implemented
		functions.put("removeOverlay", new RemoveOverlayFunction()); // not implemented

		return functions;	
	}
	
	public Boolean isImagePickerAvailable()
	{
		return isActionAvailable(GALLERY_IMAGES_AND_VIDEOS_ACTION);
	}

	public void displayImagePicker(Boolean videosAllowed, Boolean allowMultiple, Boolean crop)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering displayImagePicker");
		_shouldCrop = crop;
		_allowMultiple = allowMultiple;
		if (videosAllowed)
		{
			startPickerActivityForAction(GALLERY_IMAGES_AND_VIDEOS_ACTION);
		} 
		else
		{
			startPickerActivityForAction(GALLERY_IMAGES_ONLY_ACTION);
		}
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting displayImagePicker");
	}

	public Boolean isCameraAvailable()
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering isCameraAvailable");
		
		Boolean hasCameraFeature = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
		Boolean hasFrontCameraFeature = getActivity().getPackageManager().hasSystemFeature("android.hardware.camera.front");
		Boolean isAvailable = (hasFrontCameraFeature || hasCameraFeature) && (isActionAvailable(CAMERA_IMAGE_ACTION) || isActionAvailable(CAMERA_VIDEO_ACTION));

		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting isCameraAvailable");
		return isAvailable;
	}

	public void displayCamera(Boolean allowVideoCaptures,Boolean crop)
	{
		_shouldCrop = crop;
		
		if (allowVideoCaptures)
		{
			startPickerActivityForAction(CAMERA_VIDEO_ACTION);
		}
		else
		{
			startPickerActivityForAction(CAMERA_IMAGE_ACTION);
		}
	}

	//-----------------------------------------------------//
	//						ANE EVENTS					   //
	//-----------------------------------------------------//

	/** 
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL"
	 * 
	 * @param message Extra information you want to pass to the actionscript side
	 * of the native extension.  Usually you want to pass "OK".  In this case it 
	 * is better to use dispatchResultEvent( String ). 
	 * */
	private void dispatchResultEvent(String eventName, String message)
	{
		_currentAction = NO_ACTION;
		if (_pickerActivity != null)
		{
			_pickerActivity.finish();
		}

		dispatchStatusEventAsync(eventName, message);
	}
	
	/**
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL"
	 */
	private void dispatchResultEvent(String eventName)
	{
		dispatchResultEvent(eventName, "OK");
	}


	//-----------------------------------------------------//
	//					INTENTS AND ACTIONS				   //
	//-----------------------------------------------------//
	
	private Boolean _allowMultiple = false;

	public static final int NO_ACTION = -1;
	public static final int GALLERY_IMAGES_ONLY_ACTION = 0;
	public static final int GALLERY_IMAGES_AND_VIDEOS_ACTION = 1;
	public static final int CAMERA_IMAGE_ACTION = 2;
	public static final int CAMERA_VIDEO_ACTION = 3;
	public static final int CROP_ACTION = 4;

	private int _currentAction = NO_ACTION;

	private Boolean isActionAvailable(int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering isActionAvailable");
		
		final PackageManager packageManager = getActivity().getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(
		  getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting isActionAvailable");
		
		return list.size() > 0;
	}

	private Intent getIntentForAction(int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering getIntentForAction");
		Intent intent;
		switch (action)
		{
		case GALLERY_IMAGES_ONLY_ACTION:
		case GALLERY_IMAGES_AND_VIDEOS_ACTION:
			intent = new Intent();
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.setType((action == GALLERY_IMAGES_ONLY_ACTION) ? 
			  "image/*" : "image/*|video/*");
			// prevent the user from selecting from a Picasa album if possible
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
      }
      // allow multiselect if requested and supported
      if ((_allowMultiple) && 
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)) {
      	intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      }
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return intent;
			
		case CAMERA_IMAGE_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
		case CAMERA_VIDEO_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			
		case CROP_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent("com.android.camera.action.CROP");
		
		default:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return null;
		}
	}

	private void prepareIntentForAction(Intent intent, int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering prepareIntentForAction");
		
		if (action == CAMERA_IMAGE_ACTION)
		{
			prepareIntentForPictureCamera(intent);
		}
		if (action == CAMERA_VIDEO_ACTION)
		{
			prepareIntentForVideoCamera(intent);
		}
		else if (action == CROP_ACTION)
		{
			prepareIntentForCrop(intent);
		}
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting prepareIntentForAction");
	}

	private void handleResultForAction(Intent data, int action)
	{
		if ((action == GALLERY_IMAGES_ONLY_ACTION) || 
		    (action == GALLERY_IMAGES_AND_VIDEOS_ACTION))
		{
			handleResultForGallery(data);
		}
		else if (action == CAMERA_IMAGE_ACTION )
		{
			handleResultForImageCamera(data);
		}
		else if (action == CAMERA_VIDEO_ACTION)
		{
			handleResultForVideoCamera(data);
		}
		else if (action == CROP_ACTION)
		{
			handleResultForCrop(data);
		}
	}


	//-----------------------------------------------------//
	//					PICKER ACTIVITY					   //
	//-----------------------------------------------------//

	AirImagePickerActivity _pickerActivity;

	private void startPickerActivityForAction(int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering startPickerActivityForAction");
		
		_currentAction = action;
		Intent intent = new Intent(getActivity().getApplicationContext(), AirImagePickerActivity.class);
		getActivity().startActivity(intent);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting startPickerActivityForAction");
	}

	public void onCreatePickerActivity(AirImagePickerActivity pickerActivity)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering onCreatePickerActivity");
		
		if (_currentAction != NO_ACTION)
		{
			Intent intent = getIntentForAction(_currentAction);
			prepareIntentForAction(intent, _currentAction);
			_pickerActivity = pickerActivity;
			_pickerActivity.startActivityForResult(intent, _currentAction);
		}
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting onCreatePickerActivity");
	}

	public void onPickerActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering onPickerActivityResult");
		
		debugActivityResult(requestCode, resultCode, data);
		
		if (requestCode == _currentAction && resultCode == Activity.RESULT_OK)
		{
			handleResultForAction(data, _currentAction);
		}
		else
		{
			dispatchResultEvent("DID_CANCEL");
		}
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting onPickerActivityResult");
	}
	
	private void debugActivityResult( int requestCode, int resultCode, Intent data )
	{
		String requestCodeStr;
		if ( requestCode == NO_ACTION )	requestCodeStr = "NO_ACTION";
		else if ( requestCode == GALLERY_IMAGES_ONLY_ACTION )	requestCodeStr = "GALLERY_IMAGES_ONLY_ACTION";
		else if ( requestCode == GALLERY_IMAGES_AND_VIDEOS_ACTION )	requestCodeStr = "GALLERY_IMAGES_AND_VIDEOS_ACTION";
		else if ( requestCode == CAMERA_IMAGE_ACTION )	requestCodeStr = "CAMERA_IMAGE_ACTION";
		else if ( requestCode == CAMERA_VIDEO_ACTION )	requestCodeStr = "CAMERA_VIDEO_ACTION";
		else if ( requestCode == CROP_ACTION )	requestCodeStr = "CROP_ACTION";
		else requestCodeStr = "[unknown]";
		
		String resultCodeStr;
		if (resultCode == Activity.RESULT_OK)	resultCodeStr = "Activity.RESULT_OK";
		else if (resultCode == Activity.RESULT_CANCELED)	resultCodeStr = "Activity.RESULT_CANCELED";
		else if (resultCode == Activity.RESULT_FIRST_USER)	resultCodeStr = "Activity.RESULT_FIRST_USER";
		else resultCodeStr = "[unknown]";
		
		AirImagePickerExtension.log("onPickerActivityResult - requestCode = "+requestCodeStr+" - resultCode = "+resultCodeStr+" - data = "+data);
	}

	//-----------------------------------------------------//
	//						 GALLERY					   //
	//-----------------------------------------------------//

	private String selectedImagePath;
	private String selectedVideoPath;
	
	private void handleResultForGallery(Intent data)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering handleResultForGallery");
		
		// handle a single selected item
		if (data.getData() != null) {
		  handleGalleryItem(data.getData());
		}
		// handle multiple selected items
		else if (data.getClipData() != null) {
		  ClipData clipData = data.getClipData();
		  for (int i = 0; i < clipData.getItemCount(); i++) {
        handleGalleryItem(clipData.getItemAt(i).getUri());
      }
		}
		else {
		  Log.d(TAG, "[AirImagePickerExtensionContext] Unable to find picked items: data = "+data.toString());
		}
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting handleResultForGallery");
	}
	private void handleGalleryItem(Uri uri)
	{
	  Log.d(TAG, "[AirImagePickerExtensionContext] Entering handleGalleryItem");

		// convert to a real file path
		String path = getFilesystemPathForUri(uri);

	  // bail if we have no path to avoid crashing below
		if (path == null) {
		  Log.d(TAG, "[AirImagePickerExtensionContext] No content path found!");
		  return;
		}
		
		// try and detect if the file is a video using the content resolver,
		//  falling back on common video file extensions
		ContentResolver resolver = getActivity().getContentResolver();
		String contentType = resolver.getType(uri);
		if (((contentType != null) && (contentType.startsWith("video"))) || 
		    (path.matches("[.](3gp|asf|wmv|avi|flv|m[0-9ko]v||mp[0-9eg]+|ogg)$")))
		{
			dispatchResultEvent("DID_FINISH_PICKING", path);
		}
		// assume it's an image if we can't identify it as a video
		else
		{
			if (_shouldCrop)
			{
				// stop previous activity
				if (_currentAction == CAMERA_IMAGE_ACTION || _currentAction == GALLERY_IMAGES_ONLY_ACTION || _currentAction == GALLERY_IMAGES_AND_VIDEOS_ACTION ) {
					if (_pickerActivity != null) {
						_pickerActivity.finish();
					}
				}
				_cropInputPath = path;
				startPickerActivityForAction(CROP_ACTION);
			}
			else
			{
				dispatchResultEvent("DID_FINISH_PICKING", path);
			}
		}
	  
	  Log.d(TAG, "[AirImagePickerExtensionContext] Exiting handleGalleryItem");
	}
	
	private String getFilesystemPathForUri(Uri uri)
	{
		ContentResolver resolver = getActivity().getContentResolver();
		InputStream inStream = null;
		OutputStream outStream = null;
		// open a stream to the data
		try {
  		inStream = resolver.openInputStream(uri);
    }
    catch (FileNotFoundException e) {
      Log.d(TAG, "[AirImagePickerExtensionContext] Failed to get an input stream for URI "+
        uri.toString()+" ("+e.getMessage()+")");
      return(null);
    }
    // get a temporary file to store the media in (hopefully preserving the filename as a suffix)
    File tempFile = getTemporaryImageFile(uri.getLastPathSegment().replaceAll("[^.\\w]+", ""));
    try {
      outStream = new FileOutputStream(tempFile);
    }
    catch (FileNotFoundException e) {
      Log.d(TAG, "[AirImagePickerExtensionContext] Failed to get an output stream for file "+
        tempFile.toString()+" ("+e.getMessage()+")");
      return(null);
    }
    // write the data to the temp file 
    try {
      byte[] buffer = new byte[8 * 1024];
      int bytesRead;
      while ((bytesRead = inStream.read(buffer)) != -1) {
        outStream.write(buffer, 0, bytesRead);
      }
      inStream.close();
      outStream.close();
		}
		catch (IOException e) {
      Log.d(TAG, "[AirImagePickerExtensionContext] IOException saving stream: "+e.getMessage());
      return(null);
    }
    catch (Exception e) {
      Log.d(TAG, "[AirImagePickerExtensionContext] Failed to save stream: "+e.getMessage());
      e.printStackTrace();
      return(null);
    }
    return(tempFile.getAbsolutePath());
	}


	//-----------------------------------------------------//
	//						 CAMERA						   //
	//-----------------------------------------------------//

	private String _cameraOutputPath;

	private void prepareIntentForPictureCamera(Intent intent)
	{
		File tempFile = getTemporaryImageFile(".jpg");
		_cameraOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
	}
	
	private void prepareIntentForVideoCamera(Intent intent)
	{
		File tempFile = getTemporaryImageFile(".3gp");
		_cameraOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
	}

	private void handleResultForImageCamera(Intent data)
	{
		if (_shouldCrop)
		{
			// stop previous activity
			if (_currentAction == CAMERA_IMAGE_ACTION || _currentAction == GALLERY_IMAGES_ONLY_ACTION || _currentAction == GALLERY_IMAGES_AND_VIDEOS_ACTION ) {
				if (_pickerActivity != null) {
					_pickerActivity.finish();
				}
			}
			_cropInputPath = _cameraOutputPath;
			startPickerActivityForAction(CROP_ACTION);
		}
		else
		{
			dispatchResultEvent("DID_FINISH_PICKING", _cameraOutputPath);
		}
	}
	
	private void handleResultForVideoCamera(Intent data)
	{
		Log.d(TAG, "Entering handleResultForVideoCamera");
		
		dispatchResultEvent("DID_FINISH_PICKING", _cameraOutputPath);
		
		Log.d(TAG, "Exiting handleResultForVideoCamera");
	}

	//-----------------------------------------------------//
	//						  CROP						   //
	//-----------------------------------------------------//

	private Boolean _shouldCrop = false;
	private String _cropInputPath;
	private String _cropOutputPath;

	private void prepareIntentForCrop(Intent intent)
	{
		// Set crop input
		intent.setDataAndType(Uri.fromFile(new File(_cropInputPath)), "image/*");

		// Set crop output
		File tempFile = getTemporaryImageFile(".jpg");
		_cropOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

		// Cropped image should be square (aspect ratio 1:1)
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);

		// Set crop output size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(_cropInputPath, options);
		int smallestEdge = Math.min(options.outWidth, options.outHeight);
		intent.putExtra("outputX", smallestEdge);
		intent.putExtra("outputY", smallestEdge);
	}

	private void handleResultForCrop(Intent data)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering handleResultForCrop");
		
		dispatchResultEvent("DID_FINISH_PICKING", _cropOutputPath);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting handleResultForCrop");
	}


	//-----------------------------------------------------//
	//				   	IMAGE PROCESSING			   	   //
	//-----------------------------------------------------//

	private static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB

	private File getTemporaryImageFile( String extension )
	{
	  File f = null;
	  try {
	    f = File.createTempFile("airImagePicker-", 
	      extension, getActivity().getCacheDir());
	  }
	  catch (IOException e) {
	    Log.d(TAG, "[AirImagePickerExtensionContext] Failed to create temp file");
	  }
	  return(f);
	}
	
	private void deleteTemporaryImageFile(String filePath)
	{
		new File(filePath).delete();
	}
	
}
