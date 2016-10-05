package com.freshplanet.ane.AirImagePicker.functions;

import java.io.File;

import android.os.Environment;
import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;

public class CleanUpTemporaryDirectoryContent implements FREFunction{
	private static String TAG = "AirImagePicker";

	@Override
	public FREObject call(FREContext ctx, FREObject[] arg1) {
		
		File tempFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (tempFolder.exists())
		{
			File[] files = tempFolder.listFiles();
			for(int i=0; i<files.length; i++) {
				Log.d(TAG, "[CleanUpTemporaryDirectoryContent] deleting file:" + files[i].getAbsolutePath());
                    files[i].delete();
            }
		}
		Log.d(TAG, "[CleanUpTemporaryDirectoryContent] cleanUpTemporaryDirectoryContent");
		try {
			return FREObject.newObject(true);
		} catch (Exception e) {
			Log.d(TAG, "[CleanUpTemporaryDirectoryContent] " + e.getMessage());
			return null;
		}
		
	}
	
}
