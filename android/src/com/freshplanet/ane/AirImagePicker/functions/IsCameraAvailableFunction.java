package com.freshplanet.ane.AirImagePicker.functions;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class IsCameraAvailableFunction implements FREFunction
{
	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		Log.d(TAG, "[IsCameraAvailableFunction] entering call()");
		try
		{
			Boolean isAvailable = AirImagePickerExtension.context.isCameraAvailable();
			FREObject retValue = FREObject.newObject(isAvailable);
			
			Log.d(TAG, "[IsCameraAvailableFunction] exiting call()");
			return retValue;
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			Log.e(TAG, "[IsCameraAvailableFunction] exiting call()");
			return null;
		}
	}
}
