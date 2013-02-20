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
			AirImagePickerExtensionContext ctx = (AirImagePickerExtensionContext)context;
			
			Boolean hasCameraFeature = ctx.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
			Boolean isCameraIntentAvailable = ctx.isIntentAvailable(ctx.getIntentForAction(AirImagePickerExtensionContext.TAKE_PICTURE));
			
			return FREObject.newObject(hasCameraFeature && isCameraIntentAvailable);
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
	}
}
