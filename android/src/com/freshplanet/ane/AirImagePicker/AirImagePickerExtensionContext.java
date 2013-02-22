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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.provider.MediaStore;

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
import com.freshplanet.ane.AirImagePicker.functions.IsCameraAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsImagePickerAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveOverlayFunction;

public class AirImagePickerExtensionContext extends FREContext 
{
	public static final int NO_ACTION = -1;
	public static final int SELECT_IMAGE_ACTION = 1;
	public static final int TAKE_PICTURE_ACTION = 2;
	
	public Bitmap pickedImage;
	public byte[] pickedImageJPEGRepresentation;
	
	private int _currentAction = NO_ACTION;
	private Intent _currentIntent;
	
	@Override
	public void dispose() 
	{
		AirImagePickerExtension.context = null;
	}

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
		functions.put("displayOverlay", new DisplayOverlayFunction());
		functions.put("removeOverlay", new RemoveOverlayFunction());
		
		return functions;	
	}
	
	public int getCurrentAction()
	{
		return _currentAction;
	}
	
	public void setCurrentAction(int action)
	{
		if (action != _currentAction && (action == NO_ACTION || isActionAvailable(action)))
		{
			_currentAction = action;
			_currentIntent = null;
		}
	}
	
	public Intent getCurrentIntent()
	{
		if (_currentIntent == null)
		{
			_currentIntent = getIntentForAction(_currentAction);
		}
		
		return _currentIntent;
	}
	
	public Boolean isActionAvailable(int action)
	{
		final PackageManager packageManager = getActivity().getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
	
	private Intent getIntentForAction(int action)
	{
		switch (action)
		{
			case SELECT_IMAGE_ACTION:
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				return Intent.createChooser(intent, "Choose Picture");
			
			case TAKE_PICTURE_ACTION:
				return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
			default:
				return null;
				
		}
	}
}
