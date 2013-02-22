package com.freshplanet.ane.AirImagePicker.functions;

import android.content.Intent;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.PickerActivity;

public class DisplayCameraFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext ctx, FREObject[] args)
	{
		AirImagePickerExtension.context.setCurrentAction(AirImagePickerExtensionContext.TAKE_PICTURE_ACTION);
		Intent intent = new Intent(AirImagePickerExtension.context.getActivity().getApplicationContext(), PickerActivity.class);
		AirImagePickerExtension.context.getActivity().startActivity(intent);
		return null;
	}
}
