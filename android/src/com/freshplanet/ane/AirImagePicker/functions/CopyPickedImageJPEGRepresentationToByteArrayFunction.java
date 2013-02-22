package com.freshplanet.ane.AirImagePicker.functions;

import java.nio.ByteBuffer;

import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class CopyPickedImageJPEGRepresentationToByteArrayFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		try
		{
			// Get ByteArray
			FREByteArray byteArray = (FREByteArray)args[0];
			byteArray.acquire();
			ByteBuffer bytes = byteArray.getBytes();
			
			try
			{
				// Copy data
				bytes.put(AirImagePickerExtension.context.pickedImageJPEGRepresentation);
			}
			finally
			{
				// Clean up
				byteArray.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		return null;
	}
}
