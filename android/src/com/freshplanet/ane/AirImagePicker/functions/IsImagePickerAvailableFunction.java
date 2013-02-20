package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;

public class IsImagePickerAvailableFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		try
		{
			AirImagePickerExtensionContext ctx = (AirImagePickerExtensionContext)context;
			
			Boolean isImagePickerIntentAvailable = ctx.isIntentAvailable(ctx.getIntentForAction(AirImagePickerExtensionContext.SELECT_IMAGE));
			
			return FREObject.newObject(isImagePickerIntentAvailable);
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
	}
}
