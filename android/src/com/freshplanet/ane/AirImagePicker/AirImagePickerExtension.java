//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//    http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//////////////////////////////////////////////////////////////////////////////////////

package com.freshplanet.ane.AirImagePicker;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREExtension;

public class AirImagePickerExtension implements FREExtension 
{
	private static String TAG = "AirImagePicker";
	
	private static Boolean PRINT_LOG = true;

	public static AirImagePickerExtensionContext context;
	
	public FREContext createContext(String extId)
	{
		Log.d(TAG, "[AirImagePickerExtension] Entering createContext");
		Log.d(TAG, "[AirImagePickerExtension] Exiting createContext");
		return context = new AirImagePickerExtensionContext();
	}

	public void dispose() 
	{
		Log.d(TAG, "[AirImagePickerExtension] Entering dispose");
		context = null;
		Log.d(TAG, "[AirImagePickerExtension] Exiting dispose");
	}
	
	public void initialize() {}
	
	public static void log(String message)
	{
		if (PRINT_LOG) Log.d(TAG, "[AirImagePickerExtension] " + message);
		context.dispatchStatusEventAsync("LOGGING", message);
	}

	public static void log(String message, Throwable e)
	{
		Log.e(TAG, "[AirImagePickerExtension] ", e);
		context.dispatchStatusEventAsync("LOGGING", message + " " + e.getLocalizedMessage());
	}
}
