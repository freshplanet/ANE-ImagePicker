package com.freshplanet.ane.AirImagePicker.functions;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class GetVideoPath implements FREFunction {

	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext ctx, FREObject[] args) 
	{
		Log.d(TAG, "[GetVideoPath] Entering call()");
		
		FREObject videoPath = null;
		try {
			videoPath = FREObject.newObject( AirImagePickerExtension.context.getVideoPath() );
		} catch (Exception exception) {
			AirImagePickerExtension.log(exception.getMessage());
		}
		
		Log.d(TAG, "[GetVideoPath] Exiting call()");
		return videoPath;
	}

}
