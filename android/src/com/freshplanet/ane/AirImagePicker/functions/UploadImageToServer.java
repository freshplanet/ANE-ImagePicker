package com.freshplanet.ane.AirImagePicker.functions;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREInvalidObjectException;
import com.adobe.fre.FREObject;
import com.adobe.fre.FRETypeMismatchException;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.tasks.UploadToGoogleCloudStorageAsyncTask;

public class UploadImageToServer implements FREFunction {

	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext ctx, FREObject[] args) 
	{
		Log.d(TAG, "[UploadImageToServer] Entering call()");
		
		String localURL = null;
		String uploadURL = null;
		JSONObject uploadParamsJSON = null;
		
		try {
			localURL = args[0].getAsString();
			uploadURL = args[1].getAsString();
			uploadParamsJSON = new JSONObject(args[2].getAsString());
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (FRETypeMismatchException e) {
			e.printStackTrace();
		} catch (FREInvalidObjectException e) {
			e.printStackTrace();
		} catch (FREWrongThreadException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (localURL != null && uploadURL != null && uploadParamsJSON != null)
		{
			new UploadToGoogleCloudStorageAsyncTask(uploadParamsJSON, localURL).execute(uploadURL);
		}
		
		Log.d(TAG, "[UploadImageToServer] Exiting call()");
		return null;
	}
	
	
	
	

}
