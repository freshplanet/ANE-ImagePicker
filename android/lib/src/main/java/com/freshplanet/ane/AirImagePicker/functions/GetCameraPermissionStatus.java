/*
 * Copyright 2017 FreshPlanet
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshplanet.ane.AirImagePicker.functions;


import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

public class GetCameraPermissionStatus extends BaseFunction
{

	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		super.call(context, args);


		String result;
		int permission = ContextCompat.checkSelfPermission(context.getActivity(), Manifest.permission.CAMERA);
		if(permission == PackageManager.PERMISSION_GRANTED) {
			result = "authorized";
		}
		else if(permission == PackageManager.PERMISSION_DENIED) {
			result = "denied";
		}
		else  {
			result = "not_determined";
		}

		FREObject retValue = null;

		try {
			retValue = FREObject.newObject(result);
		} catch (FREWrongThreadException e) {
			e.printStackTrace();

		}
		return retValue;
	}
}
