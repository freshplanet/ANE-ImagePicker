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
import java.io.FileInputStream;
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
import java.util.ArrayList;
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
	private void log(String message) {
	  Log. d(TAG, "[AirImagePickerExtensionContext] "+message);
	}
	
	@Override
	public void dispose() 
	{
		log("Entering dispose");
		
		cleanupTempFiles();
		
		log("Setting AirImagePickerExtension.context to null.");

		AirImagePickerExtension.context = null;
		
		log("Exiting dispose");
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
		log("Entering displayImagePicker");
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
		log("Exiting displayImagePicker");
	}

	public Boolean isCameraAvailable()
	{
		log("Entering isCameraAvailable");
		
		Boolean hasCameraFeature = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
		Boolean hasFrontCameraFeature = getActivity().getPackageManager().hasSystemFeature("android.hardware.camera.front");
		Boolean isAvailable = (hasFrontCameraFeature || hasCameraFeature) && (isActionAvailable(CAMERA_IMAGE_ACTION) || isActionAvailable(CAMERA_VIDEO_ACTION));

		log("Exiting isCameraAvailable");
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
	
	/**
	 * @param path The absolute path to a file with the picked media.
	 */
	private void returnPickedFile(File file) {
	  // avoid deleting this file since we're passing it back to the client
	  String path = file.getAbsolutePath();
	  protectTempPath(path);
	  dispatchResultEvent("DID_FINISH_PICKING", path);
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
		log("Entering isActionAvailable");
		
		final PackageManager packageManager = getActivity().getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(
		  getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
		
		log("Exiting isActionAvailable");
		
		return list.size() > 0;
	}

	private Intent getIntentForAction(int action)
	{
		log("Entering getIntentForAction");
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
			log("Exiting getIntentForAction");
			return intent;
			
		case CAMERA_IMAGE_ACTION:
			log("Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
		case CAMERA_VIDEO_ACTION:
			log("Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			
		case CROP_ACTION:
			log("Exiting getIntentForAction");
			return new Intent("com.android.camera.action.CROP");
		
		default:
			log("Exiting getIntentForAction");
			return null;
		}
	}

	private void prepareIntentForAction(Intent intent, int action)
	{
		log("Entering prepareIntentForAction");
		
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
		log("Exiting prepareIntentForAction");
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
		log("Entering startPickerActivityForAction");
		
		_currentAction = action;
		Intent intent = new Intent(getActivity().getApplicationContext(), AirImagePickerActivity.class);
		getActivity().startActivity(intent);
		
		log("Exiting startPickerActivityForAction");
	}

	public void onCreatePickerActivity(AirImagePickerActivity pickerActivity)
	{
		log("Entering onCreatePickerActivity");
		
		if (_currentAction != NO_ACTION)
		{
			Intent intent = getIntentForAction(_currentAction);
			prepareIntentForAction(intent, _currentAction);
			_pickerActivity = pickerActivity;
			_pickerActivity.startActivityForResult(intent, _currentAction);
		}
		
		log("Exiting onCreatePickerActivity");
	}

	public void onPickerActivityResult(int requestCode, int resultCode, Intent data)
	{
		log("Entering onPickerActivityResult");
		
		debugActivityResult(requestCode, resultCode, data);
		
		if (requestCode == _currentAction && resultCode == Activity.RESULT_OK)
		{
			handleResultForAction(data, _currentAction);
		}
		else
		{
			dispatchResultEvent("DID_CANCEL");
		}
		
		log("Exiting onPickerActivityResult");
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
		log("Entering handleResultForGallery");
		
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
		  log("Unable to find picked items: data = "+data.toString());
		}
		
		// clean up temp files unless we're cropping, in which case 
		//  they should be cleaned up when that finishes
		if (! _shouldCrop) cleanupTempFiles();
		
		log("Exiting handleResultForGallery");
	}
	private void handleGalleryItem(Uri uri)
	{
	  log("Entering handleGalleryItem");

    // try and detect if the file is a video using the content resolver,
		//  falling back on common video file extensions
		ContentResolver resolver = getActivity().getContentResolver();
		String contentType = resolver.getType(uri);
		Boolean isVideo = 
		  (((contentType != null) && (contentType.startsWith("video"))) || 
		   (uri.getPath().matches("[.](3gp|asf|wmv|avi|flv|m[0-9ko]v||mp[0-9eg]+|ogg)$")));
		
		// convert to a real file path
		File file = getFileForUri(uri);
		
		// if we're cropping, pass to the crop activity
		if ((_shouldCrop) && (! isVideo)) {
		  // stop previous activity
				if ((_currentAction == CAMERA_IMAGE_ACTION) || 
				    (_currentAction == GALLERY_IMAGES_ONLY_ACTION) || 
				    (_currentAction == GALLERY_IMAGES_AND_VIDEOS_ACTION)) {
					if (_pickerActivity != null) {
						_pickerActivity.finish();
					}
				}
				_cropInputFile = file;
				startPickerActivityForAction(CROP_ACTION);
				return;
		}
		
	  // bail if we have no path to avoid crashing below
		if (file != null) {
		  returnPickedFile(file);
		}
		else {
		  log("No content path found!");
		}
	  
	  log("Exiting handleGalleryItem");
	}
	
	private File getFileForUri(Uri uri)
	{
		ContentResolver resolver = getActivity().getContentResolver();
		InputStream inStream = null;
		OutputStream outStream = null;
		// open a stream to the data
		try {
  		inStream = resolver.openInputStream(uri);
    }
    catch (FileNotFoundException e) {
      log("Failed to get an input stream for URI "+
        uri.toString()+" ("+e.getMessage()+")");
      return(null);
    }
    // get a temporary file to store the media in (hopefully preserving the filename as a suffix)
    File tempFile = getTempFile(uri.getLastPathSegment().replaceAll("[^.\\w]+", ""));
    try {
      outStream = new FileOutputStream(tempFile);
    }
    catch (FileNotFoundException e) {
      log("Failed to get an output stream for file "+
        tempFile.toString()+" ("+e.getMessage()+")");
      return(null);
    }
    // write the data to the temp file 
    try {
      copyStream(inStream, outStream);
      inStream.close();
      outStream.close();
		}
		catch (IOException e) {
      log("IOException saving stream: "+e.getMessage());
      return(null);
    }
    catch (Exception e) {
      log("Failed to save stream: "+e.getMessage());
      e.printStackTrace();
      return(null);
    }
    return(tempFile);
	}


	//-----------------------------------------------------//
	//						 CAMERA						   //
	//-----------------------------------------------------//

	private File _cameraOutputFile;

	private void prepareIntentForPictureCamera(Intent intent)
	{
		_cameraOutputFile = getTempFile(".jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(_cameraOutputFile));
	}
	
	private void prepareIntentForVideoCamera(Intent intent)
	{
		_cameraOutputFile = getTempFile(".3gp");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(_cameraOutputFile));
	}

	private void handleResultForImageCamera(Intent data)
	{
		if (_shouldCrop)
		{
			// stop previous activity
			if ((_currentAction == CAMERA_IMAGE_ACTION) || 
			    (_currentAction == GALLERY_IMAGES_ONLY_ACTION) || 
			    (_currentAction == GALLERY_IMAGES_AND_VIDEOS_ACTION)) {
				if (_pickerActivity != null) {
					_pickerActivity.finish();
				}
			}
			_cropInputFile = _cameraOutputFile;
			startPickerActivityForAction(CROP_ACTION);
		}
		else
		{
		  returnPickedFile(_cameraOutputFile);
		  // clean up temporary files
		  cleanupTempFiles();
		}
	}
	
	private void handleResultForVideoCamera(Intent data)
	{
		Log.d(TAG, "Entering handleResultForVideoCamera");
		
		returnPickedFile(_cameraOutputFile);
		// clean up temporary files
		cleanupTempFiles();
		
		Log.d(TAG, "Exiting handleResultForVideoCamera");
	}

	//-----------------------------------------------------//
	//						  CROP						   //
	//-----------------------------------------------------//

	private Boolean _shouldCrop = false;
	private File _cropInputFile;
	private File _cropOutputFile;

	private void prepareIntentForCrop(Intent intent)
	{
		// set crop input and output to shared storage so the crop intent 
		//  can access them
		_cropInputFile = getSharedFile("AirImagePicker.crop.in", _cropInputFile);
		_cropOutputFile = getSharedFile("AirImagePicker.crop.out");
		
		// set up the intent source and destination
		intent.setDataAndType(Uri.fromFile(_cropInputFile), "image/*");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(_cropOutputFile));

		// Cropped image should be square (aspect ratio 1:1)
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);

		// Set crop output size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(_cropInputFile.getAbsolutePath(), options);
		int smallestEdge = Math.min(options.outWidth, options.outHeight);
		intent.putExtra("outputX", smallestEdge);
		intent.putExtra("outputY", smallestEdge);
	}

	private void handleResultForCrop(Intent data)
	{
		log("Entering handleResultForCrop");
		
		// copy crop output to an internal path to avoid leaving junk 
		//  in shared storage
		File outFile = getTempFile(".jpg");
		try {
  		copyFile(_cropOutputFile, outFile);
    }
    catch (IOException e) {
      log("Failed to get a copy of the crop output");
      outFile = _cropOutputFile;
    }
		
		// return the result and clean up temporary files
		returnPickedFile(outFile);
		// clean up temporary files
		cleanupTempFiles();
		
		log("Exiting handleResultForCrop");
	}

	//-----------------------------------------------------//
	//				   	TEMP STORAGE    			   	               //
	//-----------------------------------------------------//

  private List<File> _tempFiles = new ArrayList<File>();

  // get a new temporary file named using the given suffix
	private File getTempFile(String suffix)
	{
	  File file = null;
	  try {
	    file = File.createTempFile("airImagePicker-", 
	      suffix, getActivity().getCacheDir());
	    // store the temp file for later cleanup
	    _tempFiles.add(file);
	  }
	  catch (IOException e) {
	    log("Failed to create temp file: "+e.getMessage());
	  }
	  return(file);
	}
	
  // remove the file at the given path from our list of temporary files, 
  //  since we don't know when the client will be done using it and 
  //  want to avoid cleaning it up prematurely
  private void protectTempPath(String path) {
	  for (int i = 0; i < _tempFiles.size(); i++) {
	    File file = _tempFiles.get(i);
	    if (file.getAbsolutePath() == path) {
	      _tempFiles.remove(i);
	      break;
	    }
	  } 
	}
	
	// remove all temporary files we have not passed back to the client
	private void cleanupTempFiles()
	{
	  log("Entering cleanupTempFiles");
	
		for (int i = 0; i < _tempFiles.size(); i++) {
	    File file = _tempFiles.get(i);
	    if ((file.exists()) && (file.isFile())) {
	      log(file.getAbsolutePath()+" - deleting");
	      try {
	        file.delete();
	      }
	      catch (Exception e) {
	        log("Failed to delete temp file: "+e.getMessage());
	      }
	    }
	    else {
	      log(file.getAbsolutePath()+" - does not exist");
	    }
	  }
	  // clear the list of temp files, since none of the them should exist anymore
	  _tempFiles.clear();
	  
	  log("Exiting cleanupTempFiles");
	}
	
	// get a path in external storage that can be read and written 
	//  by other applications
	private File getSharedFile(String name, File sourceFile) {
	  // see if we can write to external storage
    String state = Environment.getExternalStorageState();
    if (! Environment.MEDIA_MOUNTED.equals(state)) {
      log("Unable to write to external storage");
      return(sourceFile);
    }
    // get the directory and make a path there
    File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
    File file = new File(path, name);
    try {
      // make sure the directory exists
      path.mkdirs();
      // remove the file if it already exists
      if (file.exists()) file.delete();
      // copy a file from internal storage if needed
      if (sourceFile != null) {
        copyFile(sourceFile, file);
      }
      // store the file for cleanup
      _tempFiles.add(file);
      // switch the path to the shared one
      return(file);
    }
    catch (IOException e) {
	    log("Failed to create shared file: "+e.getMessage());
    }
    // fall back to returning the path to the source data
    return(sourceFile);
  }
  private File getSharedFile(String name) {
    return(getSharedFile(name, null));
  }
  
  // copy data from one file to another
  private void copyFile(File inputFile, File outputFile) throws IOException {
    InputStream inStream = new FileInputStream(inputFile);
    OutputStream outStream = new FileOutputStream(outputFile);
    copyStream(inStream, outStream);
    inStream.close();
    outStream.close();
  }
  
  // copy data from one stream to another
  private void copyStream(InputStream inStream, OutputStream outStream) throws IOException {
    byte[] buffer = new byte[8 * 1024];
    int bytesRead;
    while ((bytesRead = inStream.read(buffer)) != -1) {
      outStream.write(buffer, 0, bytesRead);
    }
  }
	
}
