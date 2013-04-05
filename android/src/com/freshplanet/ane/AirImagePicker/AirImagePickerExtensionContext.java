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
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
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
		_shouldCrop = crop;
		startPickerActivityForAction(GALLERY_ACTION);
	}

	public Boolean isCameraAvailable()
	{
		Boolean hasCameraFeature = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

		return hasCameraFeature && isActionAvailable(CAMERA_ACTION);
	}

	public void displayCamera(Boolean crop, String albumName)
	{
		_shouldCrop = crop;
		_albumName = albumName;
		startPickerActivityForAction(CAMERA_ACTION);
	}

	public int getPickedImageWidth()
	{
		return _pickedImage.getWidth();
	}

	public int getPickedImageHeight()
	{
		return _pickedImage.getHeight();
	}

	public void drawPickedImageToBitmapData(FREBitmapData bitmapData)
	{
		try
		{
			bitmapData.acquire();
			ByteBuffer bitmapBits = bitmapData.getBits();

			try
			{
				ByteBuffer pickedImageBits = ByteBuffer.allocate(4*_pickedImage.getWidth()*_pickedImage.getHeight());
				_pickedImage.copyPixelsToBuffer(pickedImageBits);

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
		return _pickedImageJPEGRepresentation.length;
	}

	public void copyPickedImageJPEGRepresentationToByteArray(FREByteArray byteArray)
	{
		try
		{
			byteArray.acquire();
			ByteBuffer bytes = byteArray.getBytes();

			try
			{
				bytes.put(_pickedImageJPEGRepresentation);
			}
			finally
			{
				byteArray.release();
			}
		}
		catch (Exception exception)
		{
			AirImagePickerExtension.log(exception.getMessage());
		}
	}


	//-----------------------------------------------------//
	//						ANE EVENTS					   //
	//-----------------------------------------------------//

	private void dispatchResultEvent(Boolean success)
	{
		_currentAction = NO_ACTION;
		if (_pickerActivity != null)
		{
			_pickerActivity.finish();
		}

		String event = success ? "DID_FINISH_PICKING" : "DID_CANCEL";
		dispatchStatusEventAsync(event, "OK");
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
		_pickerActivity = null;
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

	public void onPickerActivityResult(int requestCode, int resultCode, Intent data)
	{
		AirImagePickerExtension.log("onPickerActivityResult - requestCode = "+requestCode+" - resultCode = "+resultCode+" - data = "+data);
		if (requestCode == _currentAction && resultCode == Activity.RESULT_OK)
		{
			handleResultForAction(data, _currentAction);
		}
		else
		{
			dispatchResultEvent(false);
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

		if (_shouldCrop)
		{
			_cropInputPath = getPathForProcessedPickedImage(imagePath);
			startPickerActivityForAction(CROP_ACTION);
		}
		else
		{
			processPickedImage(imagePath);
			dispatchResultEvent(true);
		}
	}


	//-----------------------------------------------------//
	//						 CAMERA						   //
	//-----------------------------------------------------//

	private String _cameraOutputPath;

	private void prepareIntentForCamera(Intent intent)
	{
		File tempFile = getTemporaryImageFile();
		_cameraOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
	}

	private void handleResultForCamera(Intent data)
	{
		if (_shouldCrop)
		{
			_cropInputPath = getPathForProcessedPickedImage(_cameraOutputPath);
			startPickerActivityForAction(CROP_ACTION);
		}
		else
		{
			processPickedImage(_cameraOutputPath);
			dispatchResultEvent(true);
		}

		deleteTemporaryImageFile(_cameraOutputPath);
	}


	//-----------------------------------------------------//
	//						  CROP						   //
	//-----------------------------------------------------//

	private Boolean _shouldCrop = false;
	private String _cropInputPath;
	private String _cropOutputPath;

	private void prepareIntentForCrop(Intent intent)
	{
		// Set crop input
		intent.setDataAndType(Uri.fromFile(new File(_cropInputPath)), "image/*");

		// Set crop output
		File tempFile = getTemporaryImageFile();
		_cropOutputPath = tempFile.getAbsolutePath();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));

		// Cropped image should be square (aspect ratio 1:1)
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);

		// Set crop output size
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(_cropInputPath, options);
		int smallestEdge = Math.min(options.outWidth, options.outHeight);
		intent.putExtra("outputX", smallestEdge);
		intent.putExtra("outputY", smallestEdge);
	}

	private void handleResultForCrop(Intent data)
	{
		AirImagePickerExtension.log("Handle result for crop: " + data);

		_pickedImage = BitmapFactory.decodeFile(_cropOutputPath);

		deleteTemporaryImageFile(_cropInputPath);
		deleteTemporaryImageFile(_cropOutputPath);

		dispatchResultEvent(true);
	}


	//-----------------------------------------------------//
	//				   	IMAGE PROCESSING			   	   //
	//-----------------------------------------------------//

	private static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB

	private Bitmap _pickedImage;
	private byte[] _pickedImageJPEGRepresentation;
	private String _albumName;

	private File getTemporaryImageFile()
	{
		// Get or create folder for temp files
		File tempFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (!tempFolder.exists())
		{
			tempFolder.mkdir();
			try
			{
				new File(tempFolder, ".nomedia").createNewFile();
			}
			catch (Exception e) {}
		}

		// Create temp file
		return new File(tempFolder, String.valueOf(System.currentTimeMillis())+".jpg");
	}

	private void deleteTemporaryImageFile(String filePath)
	{
		new File(filePath).delete();
	}

	private String getPathForProcessedPickedImage(String filePath)
	{
		processPickedImage(filePath);

		File tempFile = getTemporaryImageFile();
		try
		{
			FileOutputStream stream = new FileOutputStream(tempFile);
			stream.write(_pickedImageJPEGRepresentation);
			stream.close();
		}
		catch (Exception exception) {}

		return tempFile.getAbsolutePath();
	}

	private void processPickedImage(String filePath)
	{
		_pickedImage = getOrientedSampleBitmapFromPath(filePath);
		_pickedImageJPEGRepresentation = getJPEGRepresentationFromBitmap(_pickedImage);

		if (_albumName != null)
		{
			long current = System.currentTimeMillis();

			// Save image to album
			File folder = new File(Environment.getExternalStorageDirectory() + File.separator + _albumName);
			if (!folder.exists()) {
				folder.mkdir();
				try {
					new File(folder, ".nomedia").createNewFile();
				} catch (Exception e) {}
			}
			File picture = new File(folder, "IMG_" + current);

			// Write Image to File
			try {
				FileOutputStream stream = new FileOutputStream(picture);
				stream.write(_pickedImageJPEGRepresentation);
				stream.close();
			} catch (Exception exception) {}

			// Notify Gallery provider that we have a new image.
			ContentValues values = new ContentValues();
			values.put(MediaStore.Images.Media.TITLE, "My HelloPop Image " + current);
			values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current/1000));
			values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
			values.put(MediaStore.Images.Media.DATA, picture.getAbsolutePath());
			ContentResolver contentResolver = getActivity().getContentResolver();
			Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			Uri newUri = contentResolver.insert(base, values);
			getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
		}
	}

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
