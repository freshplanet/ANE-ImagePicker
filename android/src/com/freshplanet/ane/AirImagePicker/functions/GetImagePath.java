package com.freshplanet.ane.AirImagePicker.functions;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class GetImagePath implements FREFunction {

	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext ctx, FREObject[] args) 
	{
		Log.d(TAG, "[GetImagePath] Entering call()");
		
		FREObject imagePath = null;
		try {
			imagePath = FREObject.newObject( AirImagePickerExtension.context.getImagePath() );
		} catch (Exception exception) {
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		Log.d(TAG, "[GetImagePath] Exiting call()");
		return imagePath;
	}

}
