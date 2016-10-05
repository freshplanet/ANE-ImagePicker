package com.freshplanet.ane.AirImagePicker.functions;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerUtils;

public class IsCropAvailableFunction implements FREFunction {

	private static String TAG = "AirImagePicker";
	@Override
	public FREObject call(FREContext context, FREObject[] arg1) {
		Log.d(TAG, "[IsCropAvailableFunction] entering call()");
		try
		{
			Boolean isAvailable = AirImagePickerUtils.isCropAvailable(context.getActivity());
			FREObject retValue = FREObject.newObject(isAvailable);
			
			Log.d(TAG, "[IsCropAvailableFunction] exiting call(), available == " + (isAvailable ? "true" : "false"));
			return retValue;
		}
		catch (FREWrongThreadException exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
			Log.e(TAG, "[IsCropAvailableFunction] exiting call() with WrongThreadException");
			return null;
		}
	}

}
