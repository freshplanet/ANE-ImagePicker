package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class GetPickedImageJPEGRepresentationSizeFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		try
		{
			return FREObject.newObject(AirImagePickerExtension.context.pickedImageJPEGRepresentation.length);
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
	}
}
