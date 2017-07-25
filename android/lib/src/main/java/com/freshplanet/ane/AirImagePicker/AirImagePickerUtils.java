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

package com.freshplanet.ane.AirImagePicker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class AirImagePickerUtils {
	
	static public class SavedBitmap {
		
		public Bitmap bitmap;
		public String path;
		
		public SavedBitmap(Bitmap bitmap, String path) {
			this.bitmap = bitmap;
			this.path = path;
		}
		
	}

	static public byte[] getJPEGRepresentationFromBitmap(Bitmap bitmap) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
		return outputStream.toByteArray();
	}

	static public Bitmap getOrientedBitmapFromBitmapAndPath(Bitmap bitmap, String filePath) {
		Log.d(TAG, "[AirImagePickerUtils] Entering getOrientedBitmapFromBitmapAndPath");
		try {
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
			Log.d(TAG, "[AirImagePickerUtils] Exiting getOrientedBitmapFromBitmapAndPath");
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
		}
		catch (Exception exception) {
			Log.d(TAG, "Couldn't fix bitmap orientation: " + exception.getMessage());
			Log.d(TAG, "[AirImagePickerUtils] Exiting getOrientedBitmapFromBitmapAndPath");
			return bitmap;
		}
	}

	static public Bitmap getOrientedSampleBitmapFromPath(String filePath) {
		Log.d(TAG, "[AirImagePickerUtils] Entering getOrientedSampleBitmapFromPath");
		
		// Choose a sample size according the memory limit
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = 1;
		while (options.outWidth/sampleSize * options.outHeight/sampleSize * 4 > AirImagePickerUtils.BITMAP_MEMORY_LIMIT)
			sampleSize *= 2;
	
		// Decode the image
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		Bitmap sampleBitmap = BitmapFactory.decodeFile(filePath, options);
		
		if(sampleBitmap == null) {
			Log.e(TAG, "[AirImagePickerUtils] Couldn't decode file:" + filePath);
			return null;
		}
	
		// Fix orientation
		Bitmap orientedSampleBitmap = getOrientedBitmapFromBitmapAndPath(sampleBitmap, filePath);
	
		Log.d(TAG, "[AirImagePickerUtils] Exiting getOrientedSampleBitmapFromPath");
		return orientedSampleBitmap;
	}

	public static final String TAG = "AirImagePicker";

	static public File getTemporaryFile( String extension ) {
		// Get or create folder for temp files
		File tempFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (!tempFolder.exists()) {
			tempFolder.mkdirs();
			try {
				new File(tempFolder, ".nomedia").createNewFile();
			}
			catch (Exception e) {
				Log.e(TAG, "Couldn't create temporary file with extension '" + extension + "'");
			}
		}
	
		// Create temp file
		try {
		    return new File(tempFolder, String.valueOf(System.currentTimeMillis())+extension);
		} catch (Exception e) {
			Log.e(TAG, "Couldn't create temp file");
		}
		return null;
	}
	
	public static File getAlbumFolder(String albumName) {
		File folder = new File(Environment.getExternalStorageDirectory() + File.separator + albumName);
		if (!folder.exists()) {
			folder.mkdir();
			try {
				new File(folder, ".yesmedia").createNewFile();
			} catch (Exception e) {
				Log.d(TAG, "[AirImagePickerUtils] exception = " + e.getMessage());
				Log.d(TAG, "[AirImagePickerUtils] Exiting didSavePictureInGallery (failed)");
				return null;
			}
		}
		return folder;
	}
	

	public static File savePictureInGallery(String albumName, String prefix, byte[] fileBytes) {
		Log.d(TAG, "[AirImagePickerUtils] Entering didSavePictureInGallery");
		
		long current = System.currentTimeMillis();
	
		// Save image to album
		File folder = getAlbumFolder(albumName);
		File picture = new File(folder, prefix + "_" + current);
	
		// Write Image to File
		try {
			FileOutputStream stream = new FileOutputStream(picture);
			stream.write(fileBytes);
			stream.close();
		} catch (Exception exception) { 
			Log.d(TAG, "[AirImagePickerUtils] exception = " + exception.getMessage());
			Log.d(TAG, "[AirImagePickerUtils] Exiting didSavePictureInGallery (failed)");
			return null; 
		}
		
		Log.d(TAG, "[AirImagePickerUtils] Exiting didSavePictureInGallery (succeeded)");
		return picture;
	}

	public static String saveImageToTemporaryDirectory(Bitmap image) {
		Log.d(TAG, "[AirImagePickerUtils] Entering saveImageToTemporaryDirectory");
		String path = "";
	    FileOutputStream outputStream;
	    try {
			File file = getTemporaryFile(".jpg");
	    	outputStream = new FileOutputStream(file);
	        outputStream.write(getJPEGRepresentationFromBitmap(image));
	        outputStream.close();
	        path = file.getAbsolutePath();
			Log.d(TAG, "[AirImagePickerUtils] saveImageToTemporaryDirectory path:"+path);
	    }
	    catch (IOException e) {
			Log.e(TAG, "[AirImagePickerUtils] saveImageToTemporaryDirectory error:"+e.toString());
	        // Error while creating file
	    }
		Log.d(TAG, "[AirImagePickerUtils] Exiting saveImageToTemporaryDirectory");
	    return path;
	}

	public static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
		Log.d(TAG, "[AirImagePickerUtils] Entering resizeImage: " + String.valueOf(maxWidth) + " x " + String.valueOf(maxHeight));
		Bitmap result = image;
		// make sure that the image has the correct height
		if (image.getWidth() > maxWidth || image.getHeight() > maxHeight
				&& maxWidth != -1 && maxHeight != -1) {
	        float reductionFactor = Math.max(Float.valueOf(image.getWidth()) / maxWidth, Float.valueOf(image.getHeight()) / maxHeight);
	        
			result = Bitmap.createScaledBitmap( image, (int)(image.getWidth()/reductionFactor), (int)(image.getHeight()/reductionFactor), true);
			Log.d(TAG, "[AirImagePickerUtils] resized image to: " + String.valueOf(result.getWidth()) + " x " + String.valueOf(result.getHeight()));
		}
		Log.d(TAG, "[AirImagePickerUtils] Exiting resizeImage");
		return result;
	}

	public static Boolean isPicasa(String path) 
	{
		return false;
	}

	public static Bitmap createThumbnailForVideo(String videoPath) {
		return ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND);
	}

	public static final int NO_ACTION = -1;
	public static final int GALLERY_IMAGES_ONLY_ACTION = 0;
	public static final int GALLERY_VIDEOS_ONLY_ACTION = 1;
	public static final int CAMERA_IMAGE_ACTION = 2;
	public static final int CAMERA_VIDEO_ACTION = 3;
	public static final int CROP_ACTION = 4;
	

	public static Intent getIntentForAction(int action) {
		Log.d(TAG, "[AirImagePickerUtils] Entering getIntentForAction");
		Intent intent;
		switch (action)
		{
		case GALLERY_IMAGES_ONLY_ACTION:
			intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return intent;
			
		case GALLERY_VIDEOS_ONLY_ACTION:
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*");
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return intent;
			
		case CAMERA_IMAGE_ACTION:
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
		case CAMERA_VIDEO_ACTION:
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			
		case CROP_ACTION:
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return new Intent("com.android.camera.action.CROP");
		
		default:
			Log.d(TAG, "[AirImagePickerUtils] Exiting getIntentForAction");
			return null;
		}
	}

	public static Boolean isCropAvailable(Activity activity) {
		Log.d(TAG, "[AirImagePickerUtils] isCropAvailable");
	
		final PackageManager packageManager = activity.getPackageManager();
		Intent intent = getIntentForAction(CROP_ACTION);
		intent.setType("image/*");
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		
		Log.d(TAG, "[AirImagePickerUtils] Exiting isCropAvailable");
		
		return list.size() > 0;
	}

	public static Boolean isActionAvailable(Activity activity, int action) {
		Log.d(TAG, "[AirImagePickerUtils] Entering isActionAvailable");
		if(action == CROP_ACTION) {
			return isCropAvailable(activity);
		}
		
		final PackageManager packageManager = activity.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
		
		Log.d(TAG, "[AirImagePickerUtils] Exiting isActionAvailable");
		
		return list.size() > 0;
	}


	static final int BITMAP_MEMORY_LIMIT = 5 * 1024 * 1024; // 5MB

	public static Bitmap swapColors(Bitmap inBitmap) {

		float matrix[] = new float[] {
				0, 0, 1, 0, 0,
				0, 1, 0, 0, 0,
				1, 0, 0, 0, 0,
				0, 0, 0, 1, 0
		};
		ColorMatrix rbSwap = new ColorMatrix(matrix);
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		paint.setColorFilter(new ColorMatrixColorFilter(rbSwap));

		Bitmap outBitmap = Bitmap.createBitmap(inBitmap.getWidth(), inBitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(outBitmap);
		canvas.drawBitmap(inBitmap, 0, 0, paint);
		return outBitmap;
	}

}
