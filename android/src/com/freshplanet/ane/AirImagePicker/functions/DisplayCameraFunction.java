package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class DisplayCameraFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext ctx, FREObject[] args)
	{
		Boolean crop = false;
		try
		{
			crop = args[0].getAsBool();
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		AirImagePickerExtension.context.displayCamera(crop);
		
		return null;
	}
}
