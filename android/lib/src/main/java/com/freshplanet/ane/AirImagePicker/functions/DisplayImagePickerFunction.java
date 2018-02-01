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

import android.content.Intent;
import android.os.Bundle;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREObject;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import com.freshplanet.ane.AirImagePicker.ImagePickerParameters;
import com.freshplanet.ane.AirImagePicker.ImagePickerResult;
import com.freshplanet.ane.AirImagePicker.activities.GalleryActivity;
import com.freshplanet.ane.AirImagePicker.activities.ImagePickerActivityBase;

public class DisplayImagePickerFunction extends BaseFunction
{
	
	
	@Override
	public FREObject call(FREContext context, FREObject[] args) {
		super.call(context, args);

		int maxImageWidth = getIntFromFREObject(args[0]);
		int maxImageHeight = getIntFromFREObject(args[1]);
		Boolean crop = getBooleanFromFREObject(args[2]);

		ImagePickerParameters params = new ImagePickerParameters(ImagePickerParameters.SCHEME_GALLERY, crop, maxImageWidth, maxImageHeight, 0);
		Intent intent = new Intent(context.getActivity().getApplicationContext(), GalleryActivity.class);
		params.mediaType = ImagePickerResult.MEDIA_TYPE_IMAGE;
		Bundle b = new Bundle();
		b.putParcelable(ImagePickerActivityBase.PARAMETERS, params);
		intent.putExtra(AirImagePickerExtension.context.getActivity().getPackageName() + ImagePickerActivityBase.PARAMETERS, b);
		context.getActivity().startActivity(intent);

		return null;
	}
}
