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
		Boolean allowVideo = false;
		Boolean allowDocument = false;
		Boolean allowMultiple = false;
		Boolean crop = false;
		try
		{
			allowVideo = args[0].getAsBool();
			allowDocument = args[1].getAsBool();
			allowMultiple = args[2].getAsBool();
			crop = args[3].getAsBool();
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		AirImagePickerExtension.context.displayImagePicker(allowVideo, allowDocument, allowMultiple, crop);
		
		return null;
	}
}
