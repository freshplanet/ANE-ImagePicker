package com.freshplanet.ane.AirImagePicker.functions;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;

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
		try
		{
			// Get BitmapData
			FREBitmapData bitmapData = (FREBitmapData)args[0];
			bitmapData.acquire();
			ByteBuffer bitmapBits = bitmapData.getBits();
			
			try
			{
				// Get picked image data
				Bitmap pickedImage = AirImagePickerExtension.context.pickedImage;
				ByteBuffer pickedImageBits = ByteBuffer.allocate(4*pickedImage.getWidth()*pickedImage.getHeight());
				pickedImage.copyPixelsToBuffer(pickedImageBits);
				
				// Copy image in BitmapData and convert from RGBA to BGRA
				int i;
				byte a, r, g, b;
				int capacity = pickedImageBits.capacity();
				for (i=0; i<capacity; i+=4)
				{
					r = pickedImageBits.get(i);
					g = pickedImageBits.get(i+1);
					b = pickedImageBits.get(i+2);
					a = pickedImageBits.get(i+3);
					
					bitmapBits.put(i, b);
					bitmapBits.put(i+1, g);
					bitmapBits.put(i+2, r);
					bitmapBits.put(i+3, a);
				}
			}
			finally
			{
				// Clean up
				bitmapData.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		return null;
	}
}
