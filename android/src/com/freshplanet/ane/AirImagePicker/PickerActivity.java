package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PickerActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		AirImagePickerExtension.context.onCreatePickerActivity(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		AirImagePickerExtension.context.onPickerActivityResult(this, requestCode, resultCode, data);
		
		finish();
	}
}
