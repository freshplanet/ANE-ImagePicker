package com.freshplanet.ane.AirImagePicker.functions;

import android.content.pm.PackageManager;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;

public class IsCameraAvailableFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		try
		{
			Boolean hasCameraFeature = AirImagePickerExtension.context.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
			Boolean isActionAvailable = AirImagePickerExtension.context.isActionAvailable(AirImagePickerExtensionContext.TAKE_PICTURE_ACTION);
			
			return FREObject.newObject(hasCameraFeature && isActionAvailable);
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
	}
}
