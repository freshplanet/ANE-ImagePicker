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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
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
	

	
	private String selectedImagePath;
	private String selectedVideoPath;
	private Bitmap _pickedImage;
	
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
	
	public void displayImagePicker(Boolean videosAllowed, Boolean crop, int maxImgWidth, int maxImgHeight)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering displayImagePicker");
		ImagePickerParameters params = new ImagePickerParameters("airImagePicker", null, crop, maxImgWidth, maxImgWidth, null);
		Intent intent = new Intent(getActivity().getApplicationContext(), GalleryActivity.class);
		params.mediaType = videosAllowed ? ImagePickerResult.MEDIA_TYPE_VIDEO : ImagePickerResult.MEDIA_TYPE_IMAGE;
		Bundle b = new Bundle();
		b.putParcelable(ImagePickerActivityBase.PARAMETERS, params);
		intent.putExtra(getActivity().getPackageName() + ImagePickerActivityBase.PARAMETERS, b);
		getActivity().startActivity(intent);
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting displayImagePicker");
	}

	public void displayCamera(Boolean allowVideoCaptures, Boolean crop, String albumName, int maxImgWidth, int maxImgHeight, String baseUri)
	{
		ImagePickerParameters params = new ImagePickerParameters("airImagePicker", baseUri, crop, maxImgWidth, maxImgWidth, albumName);

		Intent intent;
		if (allowVideoCaptures)
		{
			params.mediaType = ImagePickerResult.MEDIA_TYPE_VIDEO;
			intent = new Intent(getActivity().getApplicationContext(), VideoCameraActivity.class);
		}
		else
		{
			params.mediaType = ImagePickerResult.MEDIA_TYPE_IMAGE;
			intent = new Intent(getActivity().getApplicationContext(), ImageCameraActivity.class);
		}
		Bundle b = new Bundle();
		b.putParcelable(ImagePickerActivityBase.PARAMETERS, params);
		intent.putExtra(getActivity().getPackageName() + ImagePickerActivityBase.PARAMETERS, b);
		getActivity().startActivity(intent);
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
		AirImagePickerExtension.log("dispatching event to AS3: " + eventName + ", " + message);
		dispatchStatusEventAsync(eventName, message);
	}
	
	/**
	 * @param eventName "DID_FINISH_PICKING", "DID_CANCEL", "PICASSA_NOT_SUPPORTED"
	 */
	public void dispatchResultEvent(String eventName)
	{
		dispatchResultEvent(eventName, "OK");
	}
	
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
