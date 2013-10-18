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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils.SavedBitmap;
import com.freshplanet.ane.AirImagePicker.functions.CleanUpTemporaryDirectoryContent;
import com.freshplanet.ane.AirImagePicker.functions.DisplayCameraFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayImagePickerFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayOverlayFunction;
import com.freshplanet.ane.AirImagePicker.functions.DrawPickedImageToBitmapDataFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetImagePath;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageHeightFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageWidthFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetVideoPath;
import com.freshplanet.ane.AirImagePicker.functions.IsCameraAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsCropAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsImagePickerAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveOverlayFunction;

public class AirImagePickerExtensionContext extends FREContext 
{
	@Override
	public void dispose() 
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering dispose");
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Setting AirImagePickerExtension.context to null.");

		AirImagePickerExtension.context = null;
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting dispose");
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
		functions.put("getVideoPath", new GetVideoPath());
		functions.put("getImagePath", new GetImagePath());
		functions.put("displayOverlay", new DisplayOverlayFunction()); // not implemented
		functions.put("removeOverlay", new RemoveOverlayFunction()); // not implemented
		functions.put("cleanUpTemporaryDirectoryContent", new CleanUpTemporaryDirectoryContent());
		functions.put("isCropAvailable", new IsCropAvailableFunction());

		return functions;	
	}
	
	//FRE
	public void displayImagePicker(Boolean videosAllowed, Boolean crop, int maxImgWidth, int maxImgHeight)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering displayImagePicker");
		
		_maxSize[0] = maxImgWidth;
		_maxSize[1] = maxImgHeight;
		_shouldCrop = crop;
		if (videosAllowed)
		{
			startPickerActivityForAction(AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION);
		} 
		else
		{
			startPickerActivityForAction(AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION);
		}
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting displayImagePicker");
	}

	//FRE
	public void displayCamera(Boolean allowVideoCaptures, Boolean crop, String albumName, int maxImgWidth, int maxImgHeight, String baseUri)
	{
		_maxSize[0] = maxImgWidth;
		_maxSize[1] = maxImgHeight;
		_shouldCrop = crop;
		if (albumName != null) 
			_albumName = albumName;
		_baseUri = baseUri;
		if (allowVideoCaptures)
		{
			startPickerActivityForAction(AirImagePickerUtils.CAMERA_VIDEO_ACTION);
		}
		else
		{
			startPickerActivityForAction(AirImagePickerUtils.CAMERA_IMAGE_ACTION);
		}
	}

	//FRE
	public int getPickedImageWidth()
	{
		return _pickedImage.getWidth();
	}

	//FRE
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
				Bitmap copyImage = _pickedImage.copy(Config.ARGB_8888, true);
				copyImage.copyPixelsToBuffer(pickedImageBits);
				
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
	public void dispatchResultEvent(String eventName, String message)
	{
		_currentAction = AirImagePickerUtils.NO_ACTION;
		if (_pickerActivity != null)
		{
			_pickerActivity.finish();
		}

		dispatchStatusEventAsync(eventName, message);
	}
	
	/**
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL", "PICASSA_NOT_SUPPORTED"
	 */
	public void dispatchResultEvent(String eventName)
	{
		dispatchResultEvent(eventName, "OK");
	}

	//-----------------------------------------------------//
	//					INTENTS AND ACTIONS				   //
	//-----------------------------------------------------//

	private int _currentAction = AirImagePickerUtils.NO_ACTION;

	private void prepareIntentForAction(Intent intent, int action)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering prepareIntentForAction");
		
		if (action == AirImagePickerUtils.CAMERA_IMAGE_ACTION)
		{
			prepareIntentForPictureCamera(intent);
		}
		if (action == AirImagePickerUtils.CAMERA_VIDEO_ACTION)
		{
			prepareIntentForVideoCamera(intent);
		}
		else if (action == AirImagePickerUtils.CROP_ACTION)
		{
			prepareIntentForCrop(intent);
		}
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting prepareIntentForAction");
	}

	private void handleResultForAction(Intent data, int action)
	{
		if (action == AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION || action == AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION)
		{
			handleResultForGallery(data);
		}
		else if (action == AirImagePickerUtils.CAMERA_IMAGE_ACTION )
		{
			handleResultForImageCamera(data);
		}
		else if (action == AirImagePickerUtils.CAMERA_VIDEO_ACTION)
		{
			handleResultForVideoCamera(data);
		}
		else if (action == AirImagePickerUtils.CROP_ACTION)
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
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering startPickerActivityForAction");
		
		_currentAction = action;
		Intent intent = new Intent(getActivity().getApplicationContext(), AirImagePickerActivity.class);
		getActivity().startActivity(intent);
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting startPickerActivityForAction");
	}

	public void onCreatePickerActivity(AirImagePickerActivity pickerActivity)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering onCreatePickerActivity");
		
		if (_currentAction != AirImagePickerUtils.NO_ACTION)
		{
			Intent intent = AirImagePickerUtils.getIntentForAction(_currentAction);
			prepareIntentForAction(intent, _currentAction);
			_pickerActivity = pickerActivity;
			_pickerActivity.startActivityForResult(intent, _currentAction);
		}
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting onCreatePickerActivity");
	}

	public void onPickerActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering onPickerActivityResult");
		
		debugActivityResult(requestCode, resultCode, data);
		
		if (requestCode == _currentAction && resultCode == Activity.RESULT_OK)
		{
			handleResultForAction(data, _currentAction);
		}
		else
		{
			dispatchResultEvent("DID_CANCEL");
		}
		if(_pickerActivity != null)
		{
			_pickerActivity.finish();
			_pickerActivity = null;
		}
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting onPickerActivityResult");
	}

	private void debugActivityResult( int requestCode, int resultCode, Intent data )
	{
		String requestCodeStr;
		if ( requestCode == AirImagePickerUtils.NO_ACTION )	requestCodeStr = "NO_ACTION";
		else if ( requestCode == AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION )	requestCodeStr = "GALLERY_IMAGES_ONLY_ACTION";
		else if ( requestCode == AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION )	requestCodeStr = "GALLERY_IMAGES_AND_VIDEOS_ACTION";
		else if ( requestCode == AirImagePickerUtils.CAMERA_IMAGE_ACTION )	requestCodeStr = "CAMERA_IMAGE_ACTION";
		else if ( requestCode == AirImagePickerUtils.CAMERA_VIDEO_ACTION )	requestCodeStr = "CAMERA_VIDEO_ACTION";
		else if ( requestCode == AirImagePickerUtils.CROP_ACTION )	requestCodeStr = "CROP_ACTION";
		else requestCodeStr = "[unknown]";
		
		String resultCodeStr;
		if (resultCode == Activity.RESULT_OK)	resultCodeStr = "Activity.RESULT_OK";
		else if (resultCode == Activity.RESULT_CANCELED)	resultCodeStr = "Activity.RESULT_CANCELED";
		else if (resultCode == Activity.RESULT_FIRST_USER)	resultCodeStr = "Activity.RESULT_FIRST_USER";
		else resultCodeStr = "[unknown]";
		
		Log.d(AirImagePickerUtils.TAG, "onPickerActivityResult - requestCode = "+requestCodeStr+" - resultCode = "+resultCodeStr+" - data = "+data);
	}

	//-----------------------------------------------------//
	//						 GALLERY					   //
	//-----------------------------------------------------//

	private String selectedImagePath;
	private String selectedVideoPath;
	

	private void handleResultForGallery(Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering handleResultForGallery");
		
		Uri selectedImageUri = data.getData();
		
		// OI File Manager
		String fileManagerString = selectedImageUri.getPath();
		
		// Media Gallery
		selectedImagePath = AirImagePickerUtils.getPath(getActivity(), selectedImageUri);
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] fileManager = " + fileManagerString);
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] selectedImagePath = " + selectedImagePath);
		
		if (AirImagePickerUtils.isPicasa(selectedImagePath))
		{
			dispatchResultEvent("PICASSA_NOT_SUPPORTED");
			Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting handleResultForGallery (ret value false)");
			return;
		}
		
		Boolean isVideo = getActivity().getContentResolver().getType(selectedImageUri).indexOf("video") != -1;
		
		if ( isVideo )
		{
			selectedVideoPath = selectedImagePath; 
			_pickedImage = AirImagePickerUtils.createThumbnailForVideo(selectedVideoPath);
			selectedImagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(_pickedImage);
			//if ( processPickedImage(selectedImagePath) )
			dispatchResultEvent("DID_FINISH_PICKING", "VIDEO");
		}
		else
		{
			if (_shouldCrop && AirImagePickerUtils.isCropAvailable(getActivity()))
			{
				// stop previous activity
				if (_currentAction == AirImagePickerUtils.CAMERA_IMAGE_ACTION || _currentAction == AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION || _currentAction == AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION ) {
					if (_pickerActivity != null) {
						_pickerActivity.finish();
					}
				}
				_cropInputPath = selectedImagePath;
				startPickerActivityForAction(AirImagePickerUtils.CROP_ACTION);
			}
			else
			{
				SavedBitmap savedImage = AirImagePickerUtils.orientAndSaveImage(getActivity(), selectedImagePath, _maxSize[0], _maxSize[1], _albumName);
				
				if(savedImage != null) {
					_pickedImage = savedImage.bitmap;
					selectedImagePath = savedImage.path;
					dispatchResultEvent("DID_FINISH_PICKING", "IMAGE");
				}
			}
		}
		
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting handleResultForGallery");
	}

	//-----------------------------------------------------//
	//						 CAMERA						   //
	//-----------------------------------------------------//

	private String _cameraOutputPath;

	private void prepareIntentForPictureCamera(Intent intent)
	{
		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		_cameraOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
		if(_baseUri != null) {
			intent.putExtra(this.getActivity().getPackageName() + ":BASE_URI", _baseUri);
		}
	}
	
	private void prepareIntentForVideoCamera(Intent intent)
	{
		File tempFile = AirImagePickerUtils.getTemporaryFile(".3gp");
		_cameraOutputPath = tempFile.getAbsolutePath();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
		if(_baseUri != null) {
			intent.putExtra(this.getActivity().getPackageName() + ":BASE_URI", _baseUri);
		}
	}

	private void handleResultForImageCamera(Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] entering handleResultForImageCamera");
		if (_shouldCrop && AirImagePickerUtils.isCropAvailable(getActivity()))
		{
			// stop previous activity
			if (_currentAction == AirImagePickerUtils.CAMERA_IMAGE_ACTION || _currentAction == AirImagePickerUtils.GALLERY_IMAGES_ONLY_ACTION || _currentAction == AirImagePickerUtils.GALLERY_VIDEOS_ONLY_ACTION ) {
				if (_pickerActivity != null) {
					_pickerActivity.finish();
				}
			}
			_cropInputPath = _cameraOutputPath;
			startPickerActivityForAction(AirImagePickerUtils.CROP_ACTION);
		}
		else
		{
			SavedBitmap savedImage = AirImagePickerUtils.orientAndSaveImage(getActivity(), _cameraOutputPath, _maxSize[0], _maxSize[1], _albumName);
			
			if(savedImage != null) {
				_pickedImage = savedImage.bitmap;
				selectedImagePath = savedImage.path;
				dispatchResultEvent("DID_FINISH_PICKING", "IMAGE");
			}
		}

		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] exiting handleResultForImageCamera");
		//deleteTemporaryImageFile(_cameraOutputPath);
	}
	
	private void handleResultForVideoCamera(Intent data) {

		Log.d(AirImagePickerUtils.TAG, "Entering handleResultForVideoCamera");

		// EXTRA_OUTPUT doesn't work on some 2.x phones, copy manually to the
		// desired path
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			FileInputStream inputstr = null;
			FileOutputStream outputstr = null;
			File outputFile = null;
			try {
				outputFile = new File(_cameraOutputPath);
				AssetFileDescriptor videoAsset = getActivity()
						.getContentResolver().openAssetFileDescriptor(
								data.getData(), "r");
				inputstr = videoAsset.createInputStream();
				outputstr = new FileOutputStream(outputFile);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = inputstr.read(buffer)) > 0) {
					outputstr.write(buffer, 0, length);
				}
			} catch (IOException e) {
				// TODO: handle error
				Log.e(AirImagePickerUtils.TAG, e.getMessage());
			} finally {
				try {
					if (inputstr != null)
						inputstr.close();
					if (outputstr != null)
						outputstr.close();
				} catch (Exception e2) {
					// TODO: handle exception
					Log.e(AirImagePickerUtils.TAG, e2.getMessage());
				}
			}
		}
		
		Log.d(AirImagePickerUtils.TAG, "_cameraOutputPath = "+_cameraOutputPath);
		selectedVideoPath = _cameraOutputPath; 
		_pickedImage = AirImagePickerUtils.createThumbnailForVideo(selectedVideoPath);
		selectedImagePath = AirImagePickerUtils.saveImageToTemporaryDirectory(_pickedImage);
		dispatchResultEvent("DID_FINISH_PICKING", "VIDEO");

		Log.d(AirImagePickerUtils.TAG, "Exiting handleResultForVideoCamera");
	}

	//-----------------------------------------------------//
	//						  CROP						   //
	//-----------------------------------------------------//

	private int[] _maxSize = new int[2];
	private Boolean _shouldCrop = false;
	private String _baseUri;
	private String _cropInputPath;
	private String _cropOutputPath;

	private void prepareIntentForCrop(Intent intent)
	{
		// Set crop input
		
		intent.setDataAndType(Uri.fromFile(new File(_cropInputPath)), "image/*");

		// Set crop output
		File tempFile = AirImagePickerUtils.getTemporaryFile(".jpg");
		_cropOutputPath = tempFile.getAbsolutePath();
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] line 625 " + _cropOutputPath);
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

	public void handleResultForCrop(Intent data)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering handleResultForCrop");
		
		if(data.hasExtra(MediaStore.EXTRA_OUTPUT)) {
			Uri extraImageUri = (Uri)data.getExtras().get(MediaStore.EXTRA_OUTPUT);
			if(extraImageUri != null) {
				selectedImagePath = extraImageUri.getPath();
				Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] changing selectedImagePath to: " + selectedImagePath);
			}
		}
		
		_albumName = null;
		
		SavedBitmap savedImage = AirImagePickerUtils.orientAndSaveImage(getActivity(), _cropOutputPath, _maxSize[0], _maxSize[1], _albumName);
		
		if(savedImage != null) {
			_pickedImage = savedImage.bitmap;
			selectedImagePath = savedImage.path;
			dispatchResultEvent("DID_FINISH_PICKING", "IMAGE");
		}

		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting handleResultForCrop");
	}


	//-----------------------------------------------------//
	//				   	IMAGE PROCESSING			   	   //
	//-----------------------------------------------------//

	static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB

	private Bitmap _pickedImage;
	private String _albumName;
	
	public Bitmap getPickedImage() {
		return _pickedImage; 
	}
	public void setPickedImage(Bitmap bitmap) {
		_pickedImage = bitmap;
	}
	
	
	public String getVideoPath() {
		return selectedVideoPath;
	}
	public void setVideoPath(String path) {
		selectedVideoPath = path;
	}

	public String getImagePath() {
		return selectedImagePath;
	}
	public void setImagePath(String path) {
		selectedImagePath = path;
	}
}
