package com.freshplanet.ane.AirImagePicker;

import android.content.Intent;
import android.util.Log;

public class VideoCameraActivity extends AirImagePickerActivity {

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "[AirImagePickerActivity] Entering onActivityResult");
		
		if(AirImagePickerExtension.context != null) {
			super.onActivityResult(requestCode, resultCode, data);
			AirImagePickerExtension.context.onPickerActivityResult(requestCode, resultCode, data);
		} else {
			Log.e(TAG, "[AirImagePickerActivity] got result but context is gone: " + airPackageName);
			restartApp();
		}
		
		Log.d(TAG, "[AirImagePickerActivity] Exiting onActivityResult");
	}

}


