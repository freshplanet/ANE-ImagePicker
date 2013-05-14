package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class DisplayImagePickerFunction implements FREFunction
{
	
	
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		Boolean crop = false;
		Boolean allowVideo = false;
		try
		{
			allowVideo = args[0].getAsBool();
			crop = args[1].getAsBool();
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		AirImagePickerExtension.context.displayImagePicker(allowVideo,crop);
		
		return null;
	}
}
