package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class DrawPickedImageToBitmapDataFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		FREBitmapData bitmapData = (FREBitmapData)args[0];
		
		AirImagePickerExtension.context.drawPickedImageToBitmapData(bitmapData);
		
		return null;
	}
}
