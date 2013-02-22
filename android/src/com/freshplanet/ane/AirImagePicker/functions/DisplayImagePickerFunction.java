package com.freshplanet.ane.AirImagePicker.functions;

import android.content.Intent;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtensionContext;
import com.freshplanet.ane.AirImagePicker.PickerActivity;

public class DisplayImagePickerFunction implements FREFunction
{
	@Override
	public FREObject call(FREContext context, FREObject[] args)
	{
		AirImagePickerExtension.context.setCurrentAction(AirImagePickerExtensionContext.SELECT_IMAGE_ACTION);
		Intent intent = new Intent(AirImagePickerExtension.context.getActivity().getApplicationContext(), PickerActivity.class);
		AirImagePickerExtension.context.getActivity().startActivity(intent);
		return null;
	}
}
