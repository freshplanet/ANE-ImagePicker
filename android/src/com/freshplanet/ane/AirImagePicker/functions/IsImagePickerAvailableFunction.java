package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;

public class IsImagePickerAvailableFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		try
		{
			return FREObject.newObject(AirImagePickerUtils.isImagePickerAvailable(context.getActivity()));
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
	}
}
