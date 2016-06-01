package com.freshplanet.ane.AirImagePicker.functions;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREInvalidObjectException;
import com.adobe.fre.FREObject;
import com.adobe.fre.FRETypeMismatchException;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class DisplayCameraFunction implements FREFunction
{
	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext ctx, FREObject[] args)
	{
		Log.d(TAG, "[DisplayCameraFunction] entering call()");
		
		Boolean allowVideoCapture = false;
		Boolean crop = false;
		
		try {
			allowVideoCapture = args[0].getAsBool();
			crop = args[1].getAsBool();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (FRETypeMismatchException e) {
			e.printStackTrace();
		} catch (FREInvalidObjectException e) {
			e.printStackTrace();
		} catch (FREWrongThreadException e) {
			e.printStackTrace();
		}
		AirImagePickerExtension.context.displayCamera(allowVideoCapture,crop);
		
		Log.d(TAG, "[DisplayCameraFunction] exiting call()");
		return null;
	}
}
