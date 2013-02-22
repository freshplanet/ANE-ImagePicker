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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.adobe.fre.FREBitmapData;
import com.adobe.fre.FREByteArray;
import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.freshplanet.ane.AirImagePicker.functions.CopyPickedImageJPEGRepresentationToByteArrayFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayCameraFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayImagePickerFunction;
import com.freshplanet.ane.AirImagePicker.functions.DisplayOverlayFunction;
import com.freshplanet.ane.AirImagePicker.functions.DrawPickedImageToBitmapDataFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageHeightFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageJPEGRepresentationSizeFunction;
import com.freshplanet.ane.AirImagePicker.functions.GetPickedImageWidthFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsCameraAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.IsImagePickerAvailableFunction;
import com.freshplanet.ane.AirImagePicker.functions.RemoveOverlayFunction;

public class AirImagePickerExtensionContext extends FREContext 
{
	@Override
	public void dispose() 
	{
		AirImagePickerExtension.context = null;
	}
	
	
	//-----------------------------------------------------//
	//					EXTENSION API					   //
	//-----------------------------------------------------//

	@Override
	public Map<String, FREFunction> getFunctions() 
	{
		Map<String, FREFunction> functions = new HashMap<String, FREFunction>();
		
		functions.put("isImagePickerAvailable", new IsImagePickerAvailableFunction());
		functions.put("displayImagePicker", new DisplayImagePickerFunction());
		functions.put("isCameraAvailable", new IsCameraAvailableFunction());
		functions.put("displayCamera", new DisplayCameraFunction());
		functions.put("getPickedImageWidth", new GetPickedImageWidthFunction());
		functions.put("getPickedImageHeight", new GetPickedImageHeightFunction());
		functions.put("drawPickedImageToBitmapData", new DrawPickedImageToBitmapDataFunction());
		functions.put("getPickedImageJPEGRepresentationSize", new GetPickedImageJPEGRepresentationSizeFunction());
		functions.put("copyPickedImageJPEGRepresentationToByteArray", new CopyPickedImageJPEGRepresentationToByteArrayFunction());
		functions.put("displayOverlay", new DisplayOverlayFunction()); // not implemented
		functions.put("removeOverlay", new RemoveOverlayFunction()); // not implemented
		
		return functions;	
	}
	
	public Boolean isImagePickerAvailable()
	{
		return isActionAvailable(GALLERY_ACTION);
	}
	
	public void displayImagePicker(Boolean crop)
	{
		startPickerActivityForAction(GALLERY_ACTION);
	}
	
	public Boolean isCameraAvailable()
	{
		Boolean hasCameraFeature = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
		
		return hasCameraFeature && isActionAvailable(CAMERA_ACTION);
	}
	
	public void displayCamera(Boolean crop)
	{
		startPickerActivityForAction(CAMERA_ACTION);
	}
	
	public int getPickedImageWidth()
	{
		return 0;
	}
	
	public int getPickedImageHeight()
	{
		return 0;
	}
	
	public void drawPickedImageToBitmapData(FREBitmapData bitmapData)
	{
		try
		{
			// Get BitmapData
			
			bitmapData.acquire();
			ByteBuffer bitmapBits = bitmapData.getBits();
			
			try
			{
				// Get picked image data
				Bitmap pickedImage = AirImagePickerExtension.context.pickedImage;
				ByteBuffer pickedImageBits = ByteBuffer.allocate(4*pickedImage.getWidth()*pickedImage.getHeight());
				pickedImage.copyPixelsToBuffer(pickedImageBits);
				
				// Copy image in BitmapData and convert from RGBA to BGRA
				int i;
				byte a, r, g, b;
				int capacity = pickedImageBits.capacity();
				for (i=0; i<capacity; i+=4)
				{
					r = pickedImageBits.get(i);
					g = pickedImageBits.get(i+1);
					b = pickedImageBits.get(i+2);
					a = pickedImageBits.get(i+3);
					
					bitmapBits.put(i, b);
					bitmapBits.put(i+1, g);
					bitmapBits.put(i+2, r);
					bitmapBits.put(i+3, a);
				}
			}
			finally
			{
				// Clean up
				bitmapData.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
	}
	
	public int getPickedImageJPEGRepresentationSize()
	{
		return 0;
	}
	
	public void copyPickedImageJPEGRepresentationToByteArray(FREByteArray byteArray)
	{
		try
		{
			// Get ByteArray
			
			byteArray.acquire();
			ByteBuffer bytes = byteArray.getBytes();
			
			try
			{
				// Copy data
				bytes.put(AirImagePickerExtension.context.pickedImageJPEGRepresentation);
			}
			finally
			{
				// Clean up
				byteArray.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
	}
	
	
	//-----------------------------------------------------//
	//					INTENTS AND ACTIONS				   //
	//-----------------------------------------------------//
	
	public static final int NO_ACTION = -1;
	public static final int GALLERY_ACTION = 0;
	public static final int CAMERA_ACTION = 1;
	public static final int CROP_ACTION = 2;
	
	private int _currentAction = NO_ACTION;
	
	private Boolean isActionAvailable(int action)
	{
		final PackageManager packageManager = getActivity().getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
	
	private Intent getIntentForAction(int action)
	{
		Intent intent;
		switch (action)
		{
			case GALLERY_ACTION:
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				return Intent.createChooser(intent, "Choose Picture");
			
			case CAMERA_ACTION:
				return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
			case CROP_ACTION:
				intent = new Intent("com.android.camera.action.CROP");
		        return intent;
			
			default:
				return null;
		}
	}
	
	private void prepareIntentForAction(Intent intent, int action)
	{
		if (action == CAMERA_ACTION)
		{
			prepareIntentForCamera(intent);
		}
		else if (action == CROP_ACTION)
		{
			prepareIntentForCrop(intent);
		}
	}
	
	private void handleResultForAction(Intent data, int action)
	{
		if (action == GALLERY_ACTION)
		{
			handleResultForGallery(data);
		}
		else if (action == CAMERA_ACTION)
		{
			handleResultForCamera(data);
		}
		else if (action == CROP_ACTION)
		{
			handleResultForCrop(data);
		}
	}
	
	
	//-----------------------------------------------------//
	//					PICKER ACTIVITY					   //
	//-----------------------------------------------------//
	
	PickerActivity _pickerActivity;
	
	private void startPickerActivityForAction(int action)
	{
		_currentAction = action;
		Intent intent = new Intent(getActivity().getApplicationContext(), PickerActivity.class);
		getActivity().startActivity(intent);
	}
	
	public void onCreatePickerActivity(PickerActivity pickerActivity)
	{
		if (_pickerActivity == null && _currentAction != NO_ACTION)
		{
			Intent intent = getIntentForAction(_currentAction);
			prepareIntentForAction(intent, _currentAction);
			_pickerActivity = pickerActivity;
			_pickerActivity.startActivityForResult(intent, _currentAction);
		}
	}
	
	public void onPickerActivityResult(PickerActivity pickerActivity, int requestCode, int resultCode, Intent data)
	{
		if (requestCode == _currentAction && resultCode == Activity.RESULT_OK)
		{
			handleResultForAction(data, _currentAction);
		}
		else
		{
			dispatchStatusEventAsync("DID_CANCEL", "OK");
		}
	}
	
	
	//-----------------------------------------------------//
	//						 GALLERY					   //
	//-----------------------------------------------------//
	
	private void handleResultForGallery(Intent data)
	{
		Uri imageUri = data.getData();
		
		// Convert the resulting Uri into an absolute file path
		String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(imageUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String imagePath = cursor.getString(column_index);
	}
	
	
	//-----------------------------------------------------//
	//						 CAMERA						   //
	//-----------------------------------------------------//
	
	private String _cameraOutputPath;
	
	private void prepareIntentForCamera(Intent intent)
	{
		// Get or create folder for camera pictures
		File cameroOutputFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (!cameroOutputFolder.exists())
		{
			cameroOutputFolder.mkdir();
			try
			{
				new File(cameroOutputFolder, ".nomedia").createNewFile();
			}
			catch (Exception e) {}
		}
		
		// Create camera output path
		File cameraOutputFile = new File(cameroOutputFolder, String.valueOf(System.currentTimeMillis())+".jpg");
		_cameraOutputPath = cameraOutputFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(cameraOutputFile));
	}
	
	private void handleResultForCamera(Intent data)
	{
		// Delete temp file
		new File(_cameraOutputPath).delete();
	}
	
	
	//-----------------------------------------------------//
	//						  CROP						   //
	//-----------------------------------------------------//
	
	private String _cropInputPath;
	
	private void prepareIntentForCrop(Intent intent)
	{
		intent.setDataAndType(Uri.fromFile(new File(_cropInputPath)), "image/*");
		intent.putExtra("outputX", 96);
		intent.putExtra("outputY", 96);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
	}
	
	private void handleResultForCrop(Intent data)
	{
		
	}
	
	
	//-----------------------------------------------------//
	//				   	IMAGE PROCESSING			   	   //
	//-----------------------------------------------------//
	
	private static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB
	
	private Bitmap getOrientedSampleBitmapFromPath(String filePath)
	{
		// Choose a sample size according the memory limit
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = 1;
		while (options.outWidth/sampleSize * options.outHeight/sampleSize * 4 > BITMAP_MEMORY_LIMIT)
			sampleSize *= 2;
		
		// Decode the image
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		Bitmap sampleBitmap = BitmapFactory.decodeFile(filePath, options);
		
		// Fix orientation
		Bitmap orientedSampleBitmap = getOrientedBitmapFromBitmapAndPath(sampleBitmap, filePath);
		
		return orientedSampleBitmap;
	}
	
	private Bitmap getOrientedBitmapFromBitmapAndPath(Bitmap bitmap, String filePath)
	{
		try
		{
			// Get orientation from EXIF
			ExifInterface exif = new ExifInterface(filePath);
			int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			
			// Compute rotation matrix
			Matrix rotation = new Matrix();
			switch (exifOrientation)
			{
	          case ExifInterface.ORIENTATION_ROTATE_90:
	        	  rotation.preRotate(90);
	        	  break; 
	          
	          case ExifInterface.ORIENTATION_ROTATE_180:
	        	  rotation.preRotate(180);
	        	  break;

	          case ExifInterface.ORIENTATION_ROTATE_270:
	        	  rotation.preRotate(270);
	        	  break;
			}
			
			// Return new bitmap
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log("Couldn't fix bitmap orientation: " + exception.getMessage());
			
			return bitmap;
		}
	}
	
	private byte[] getJPEGRepresentationFromBitmap(Bitmap bitmap)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		return outputStream.toByteArray();
	}
}
