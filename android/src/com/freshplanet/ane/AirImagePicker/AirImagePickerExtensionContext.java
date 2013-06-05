//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.freshplanet.ane.AirImagePicker.functions.CopyPickedImageJPEGRepresentationToByteArrayFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayCameraFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayImagePickerFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayOverlayFunction;
import com.freshplanet.ane.AirImagePicker.functions.DrawPickedImageToBitmapDataFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageHeightFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageJPEGRepresentationSizeFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageWidthFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetVideoPath;
import com.freshplanet.ane.AirImagePicker.functions.IsCameraAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsImagePickerAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveOverlayFunction;
import com.freshplanet.ane.AirImagePicker.functions.UploadToServer;

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
		functions.put("getPickedImageWidth", new GetPickedImageWidthFunction());
		functions.put("getPickedImageHeight", new GetPickedImageHeightFunction());
		functions.put("drawPickedImageToBitmapData", new DrawPickedImageToBitmapDataFunction());
		functions.put("getPickedImageJPEGRepresentationSize", new GetPickedImageJPEGRepresentationSizeFunction());
		functions.put("copyPickedImageJPEGRepresentationToByteArray", new CopyPickedImageJPEGRepresentationToByteArrayFunction());
		functions.put("getVideoPath", new GetVideoPath());
		functions.put("uploadToServer", new UploadToServer());
		functions.put("displayOverlay", new DisplayOverlayFunction()); // not implemented
		functions.put("removeOverlay", new RemoveOverlayFunction()); // not implemented

		return functions;	
	}
	
	public Boolean isImagePickerAvailable()
	{
		return isActionAvailable(GALLERY_IMAGES_AND_VIDEOS_ACTION);
	}

	public void displayImagePicker(Boolean videosAllowed, Boolean crop)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering displayImagePicker");
		_shouldCrop = crop;
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

	public void displayCamera(Boolean allowVideoCaptures,Boolean crop, String albumName)
	{
		_shouldCrop = crop;
		if (albumName != null) 
			_albumName = albumName;
		
		if (allowVideoCaptures)
		{
			startPickerActivityForAction(CAMERA_VIDEO_ACTION);
		}
		else
		{
			startPickerActivityForAction(CAMERA_IMAGE_ACTION);
		}
	}

	public int getPickedImageWidth()
	{
		return _pickedImage.getWidth();
	}

	public int getPickedImageHeight()
	{
		return _pickedImage.getHeight();
	}

	public void drawPickedImageToBitmapData(FREBitmapData bitmapData)
	{
		try
		{
			bitmapData.acquire();
			ByteBuffer bitmapBits = bitmapData.getBits();

			try
			{
				ByteBuffer pickedImageBits = ByteBuffer.allocate(4*_pickedImage.getWidth()*_pickedImage.getHeight());
				_pickedImage.copyPixelsToBuffer(pickedImageBits);

				// Copy image in BitmapData and convert from RGBA to BGRA
				int i;
				byte a, r, g, b;
				int capacity = pickedImageBits.capacity();
				for (i=0; i<capacity; i+=4)
				{
					r = pickedImageBits.get(i);
					g = pickedImageBits.get(i+1);
					b = pickedImageBits.get(i+2);
					a = pickedImageBits.get(i+3);

					bitmapBits.put(i, b);
					bitmapBits.put(i+1, g);
					bitmapBits.put(i+2, r);
					bitmapBits.put(i+3, a);
				}
			}
			finally
			{
				bitmapData.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
	}

	public int getPickedImageJPEGRepresentationSize()
	{
		return _pickedImageJPEGRepresentation.length;
	}

	public void copyPickedImageJPEGRepresentationToByteArray(FREByteArray byteArray)
	{
		try
		{
			byteArray.acquire();
			ByteBuffer bytes = byteArray.getBytes();

			try
			{
				bytes.put(_pickedImageJPEGRepresentation);
			}
			finally
			{
				byteArray.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
	}


	//-----------------------------------------------------//
	//						ANE EVENTS					   //
	//-----------------------------------------------------//

	/** 
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL", "PICASSA_NOT_SUPPORTED"
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
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL", "PICASSA_NOT_SUPPORTED"
	 */
	private void dispatchResultEvent(String eventName)
	{
		dispatchResultEvent(eventName, "OK");
	}


	//-----------------------------------------------------//
	//					INTENTS AND ACTIONS				   //
	//-----------------------------------------------------//

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
		List<ResolveInfo> list = packageManager.queryIntentActivities(getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
		
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
			intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return intent;
			
		case GALLERY_IMAGES_AND_VIDEOS_ACTION:
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*, images/*");
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
		if (action == GALLERY_IMAGES_ONLY_ACTION || action == GALLERY_IMAGES_AND_VIDEOS_ACTION)
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
		
		Uri selectedImageUri = data.getData();
		
		// OI File Manager
		String fileManagerString = selectedImageUri.getPath();
		
		// Media Gallery
		selectedImagePath = getPath(selectedImageUri);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] fileManager = " + fileManagerString);
		Log.d(TAG, "[AirImagePickerExtensionContext] selectedImagePath = " + selectedImagePath);
		
		Boolean isVideo = AirImagePickerExtension.context.getActivity().getContentResolver().getType(selectedImageUri).indexOf("video") != -1;
		
		if ( isVideo )
		{
			selectedVideoPath = selectedImagePath; 
			createThumbnailForVideo();
			if ( processPickedImage(selectedImagePath) )
				dispatchResultEvent("DID_FINISH_PICKING", "VIDEO");
		}
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
				_cropInputPath = getPathForProcessedPickedImage(selectedImagePath);
				startPickerActivityForAction(CROP_ACTION);
			}
			else
			{
				if ( processPickedImage(selectedImagePath) )
					dispatchResultEvent("DID_FINISH_PICKING","IMAGE");
			}
		}
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting handleResultForGallery");
	}
	
	private void createThumbnailForVideo()
	{
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(selectedVideoPath, MediaStore.Images.Thumbnails.MICRO_KIND);
		File thumbFile = getTemporaryImageFile(".jpg");
		try {
			FileOutputStream out = new FileOutputStream(thumbFile);
			thumb.compress(Bitmap.CompressFormat.JPEG, 90, out);
			selectedImagePath = thumbFile.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getPath(Uri selectedImage)
	{
		final String[] filePathColumn = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
		Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
		
		// Some devices return an URI of com.android instead of com.google.android
		if (selectedImage.toString().startsWith("content://com.android.gallery3d.provider"))
		{
			selectedImage = Uri.parse( selectedImage.toString().replace("com.android.gallery3d", "com.google.android.gallery3d") );
		}

		if (cursor != null)
		{
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(MediaColumns.DATA);
			
			// if it is a picassa image on newer devices with OS 3.0 and up
			if (selectedImage.toString().startsWith("content://com.google.android.gallery3d"))
			{
				columnIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
				return selectedImage.toString();
			}
			else
			{
				return cursor.getString(columnIndex);
			}
		}
		else if ( selectedImage != null && selectedImage.toString().length() > 0 )
		{
			return selectedImage.toString();
		}
		else return null;
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
			_cropInputPath = getPathForProcessedPickedImage(_cameraOutputPath);
			startPickerActivityForAction(CROP_ACTION);
		}
		else
		{
			if ( processPickedImage(_cameraOutputPath) )
				dispatchResultEvent("DID_FINISH_PICKING", "IMAGE");
		}

		deleteTemporaryImageFile(_cameraOutputPath);
	}
	
	private void handleResultForVideoCamera(Intent data)
	{
		Log.d(TAG, "Entering handleResultForVideoCamera");

		Log.d(TAG, "_cameraOutputPath = "+_cameraOutputPath);
		
		selectedVideoPath = _cameraOutputPath; 
		createThumbnailForVideo();
		if ( processPickedImage(selectedImagePath) )
			dispatchResultEvent("DID_FINISH_PICKING", "VIDEO");
		
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
		
		_pickedImage = BitmapFactory.decodeFile(_cropOutputPath);
		
		deleteTemporaryImageFile(_cropInputPath);
		deleteTemporaryImageFile(_cropOutputPath);
		
		dispatchResultEvent("DID_FINISH_PICKING", "IMAGE");
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting handleResultForCrop");
	}


	//-----------------------------------------------------//
	//				   	IMAGE PROCESSING			   	   //
	//-----------------------------------------------------//

	private static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB

	private Bitmap _pickedImage;
	private byte[] _pickedImageJPEGRepresentation;
	private String _albumName;

	private File getTemporaryImageFile( String extension )
	{
		// Get or create folder for temp files
		File tempFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (!tempFolder.exists())
		{
			tempFolder.mkdir();
			try
			{
				new File(tempFolder, ".nomedia").createNewFile();
			}
			catch (Exception e) {}
		}

		// Create temp file
		return new File(tempFolder, String.valueOf(System.currentTimeMillis())+extension);
	}
	
	private void deleteTemporaryImageFile(String filePath)
	{
		new File(filePath).delete();
	}

	private String getPathForProcessedPickedImage(String filePath)
	{
		processPickedImage(filePath);

		File tempFile = getTemporaryImageFile(".jpg");
		try
		{
			FileOutputStream stream = new FileOutputStream(tempFile);
			stream.write(_pickedImageJPEGRepresentation);
			stream.close();
		}
		catch (Exception exception) {}

		return tempFile.getAbsolutePath();
	}

	private Boolean processPickedImage(String filePath)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering processPickedImage");
		
		if ( filePath.startsWith("content://com.google.android.gallery3d.provider") )
		{
			// RETRIEVING IMAGES FROM PICASSA IS NOT SUPPORTED IN THIS VERSION
			dispatchResultEvent("PICASSA_NOT_SUPPORTED");
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting processPickedImage (ret value false)");
			return false;
			
//			final String picassaPath = filePath;
//			// Do this in a background thread, since we are fetching a large image from the web.
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					_pickedImage = getOrientedSampleBitmapFromPicassa(picassaPath);
//				}
//			}).start();
		}
		else {
			_pickedImage = getOrientedSampleBitmapFromPath(filePath);
		}
		
		_pickedImageJPEGRepresentation = getJPEGRepresentationFromBitmap(_pickedImage);

		Boolean result = _albumName != null ? didSavePictureInGallery() : true;
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting processPickedImage (ret value +"+result.toString()+")");
		return result;
	}
	
	private Boolean didSavePictureInGallery()
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering didSavePictureInGallery");
		
		long current = System.currentTimeMillis();

		// Save image to album
		File folder = new File(Environment.getExternalStorageDirectory() + File.separator + _albumName);
		if (!folder.exists()) {
			folder.mkdir();
			try {
				new File(folder, ".nomedia").createNewFile();
			} catch (Exception e) {
				Log.d(TAG, "[AirImagePickerExtensionContext] exception = " + e.getMessage());
				Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (ret value false)");
				return false;
			}
		}
		File picture = new File(folder, "IMG_" + current);

		// Write Image to File
		try {
			FileOutputStream stream = new FileOutputStream(picture);
			stream.write(_pickedImageJPEGRepresentation);
			stream.close();
		} catch (Exception exception) { 
			Log.d(TAG, "[AirImagePickerExtensionContext] exception = " + exception.getMessage());
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (ret value false)");
			return false; 
		}

		// Notify Gallery provider that we saved a new image
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, "My HelloPop Image " + current);
		values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current/1000));
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.Media.DATA, picture.getAbsolutePath());
		ContentResolver contentResolver = getActivity().getContentResolver();
		Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		Uri newUri = contentResolver.insert(base, values);
		getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (ret value true)");
		return true;
	}

	@SuppressWarnings("unused")
	private Bitmap getOrientedSampleBitmapFromPicassa(String filePath)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering getOrientedSampleBitmapFromPicassa");
		
		// PICASSA PICKING IS NOT SUPPORTED FOR NOW
		// http://dimitar.me/how-to-get-picasa-images-using-the-image-picker-on-android-devices-running-any-os-version/
		
		File cacheDir;
		
		// if the device has a SD card
		if (android.os.Environment.getExternalStorageDirectory().equals(android.os.Environment.MEDIA_MOUNTED)){
			Log.d(TAG, "[AirImagePickerExtensionContext] cacheDir from getExternalStorageDirectory()");
			cacheDir = new File(android.os.Environment.getExternalStorageDirectory(),".OCFL311");
		} else {
			Log.d(TAG, "[AirImagePickerExtensionContext] cacheDir from getCacheDir()");
			cacheDir = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		}
		
		if (!cacheDir.exists())
			cacheDir.mkdirs();
		
		Log.d(TAG, "[AirImagePickerExtensionContext] create file in cache dir");
		File f = new File( cacheDir, "image_file_name.jpg");
		
		try
		{
			Log.d(TAG, "[AirImagePickerExtensionContext] open input stream in picassa");
			
			InputStream is = null;
			if ( filePath.startsWith("content://com.google.android.gallery3d") ) {
				Log.d(TAG, "[AirImagePickerExtensionContext] 1");
				is = getActivity().getApplicationContext().getContentResolver().openInputStream(Uri.parse(filePath));
				Log.d(TAG, "[AirImagePickerExtensionContext] 2");
			} else {
				is = new URL(filePath.toString()).openStream();
			}
			
			Log.d(TAG, "[AirImagePickerExtensionContext] open outputstream in file system");
			OutputStream os = new FileOutputStream(f);
			
			Log.d(TAG, "[AirImagePickerExtensionContext] copy bytes from picassa to file system");
			// 
			byte[] buffer = new byte[1024];
			int len;
			while( (len = is.read(buffer)) != -1 ) {
				os.write(buffer,0,len);
			}
			
			Log.d(TAG, "[AirImagePickerExtensionContext] done copying, close OutputStream");
			os.close();
			Bitmap b = getOrientedSampleBitmapFromPath(f.getAbsolutePath()); 
			
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPicassa");
			return b;
		} 
		catch (Exception ex) {
			Log.d( TAG, "[AirImagePickerExtensionContext] Exception: " + ex.getMessage());
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPicassa");
			return null;
		}
	}
	
	private Bitmap getOrientedSampleBitmapFromPath(String filePath)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering getOrientedSampleBitmapFromPath");
		
		// Choose a sample size according the memory limit
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = 1;
		while (options.outWidth/sampleSize * options.outHeight/sampleSize * 4 > BITMAP_MEMORY_LIMIT)
			sampleSize *= 2;

		// Decode the image
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		Bitmap sampleBitmap = BitmapFactory.decodeFile(filePath, options);

		// Fix orientation
		Bitmap orientedSampleBitmap = getOrientedBitmapFromBitmapAndPath(sampleBitmap, filePath);

		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPath");
		return orientedSampleBitmap;
	}

	private Bitmap getOrientedBitmapFromBitmapAndPath(Bitmap bitmap, String filePath)
	{
		try
		{
			// Get orientation from EXIF
			ExifInterface exif = new ExifInterface(filePath);
			int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

			// Compute rotation matrix
			Matrix rotation = new Matrix();
			switch (exifOrientation)
			{
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotation.preRotate(90);
				break; 

			case ExifInterface.ORIENTATION_ROTATE_180:
				rotation.preRotate(180);
				break;

			case ExifInterface.ORIENTATION_ROTATE_270:
				rotation.preRotate(270);
				break;
			}

			// Return new bitmap
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log("Couldn't fix bitmap orientation: " + exception.getMessage());

			return bitmap;
		}
	}

	private byte[] getJPEGRepresentationFromBitmap(Bitmap bitmap)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		return outputStream.toByteArray();
	}

	public String getVideoPath() {
		return selectedVideoPath;
	}
}
