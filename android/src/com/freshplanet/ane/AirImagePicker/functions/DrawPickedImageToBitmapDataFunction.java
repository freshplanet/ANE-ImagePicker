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
			
			// Get picked image data
			Bitmap pickedImage = AirImagePickerExtension.context.pickedImage;
			ByteBuffer pickedImageBits = ByteBuffer.allocate(4*pickedImage.getWidth()*pickedImage.getHeight());
			pickedImage.copyPixelsToBuffer(pickedImageBits);
			
			// Copy image in BitmapData and convert from RGBA to BGRA
			int EXCEPT_R_MASK = 0x00FFFFFF;
			int ONLY_R_MASK = ~EXCEPT_R_MASK;
			int EXCEPT_B_MASK = 0xFFFF00FF;
			int ONLY_B_MASK = ~EXCEPT_B_MASK;
			int pixel, newPixel, r, b;
			pickedImageBits.position(0);
			while (pickedImageBits.hasRemaining())
			{
				pixel = pickedImageBits.getInt();
				r = (pixel & ONLY_R_MASK) >> 24;
				b = (pixel & ONLY_B_MASK) >> 8;
				newPixel = (pixel & EXCEPT_B_MASK & EXCEPT_R_MASK) | (b << 24) | (r << 8);
				bitmapBits.putInt(newPixel);
			}
			
			// Clean up
			bitmapData.release();
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		return null;
	}
}
