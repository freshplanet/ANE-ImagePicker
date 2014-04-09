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
		int maxImageWidth = -1;
		int maxImageHeight = -1;
		Boolean crop = false;
		Boolean allowVideo = false;
		try
		{
			maxImageWidth = args[0].getAsInt();
			maxImageHeight = args[1].getAsInt();
			allowVideo = args[2].getAsBool();
			crop = args[3].getAsBool();
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		AirImagePickerExtension.context.displayImagePicker(allowVideo,crop,maxImageWidth,maxImageHeight);
		
		return null;
	}
}
