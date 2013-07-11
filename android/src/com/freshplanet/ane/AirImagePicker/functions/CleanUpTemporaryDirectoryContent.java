package com.freshplanet.ane.AirImagePicker.functions;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class CleanUpTemporaryDirectoryContent implements FREFunction{
	private static String TAG = "AirImagePicker";

	@Override
	public FREObject call(FREContext ctx, FREObject[] arg1) {
		
		try {
			return FREObject.newObject(AirImagePickerExtension.context.cleanUpTemporaryDirectoryContent());
		} catch (Exception exception) {
			AirImagePickerExtension.log(exception.getMessage());
			return null;
		}
		
	}
	
}
