package com.freshplanet.ane.AirImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class AirImagePickerUtils {
	
	static public class SavedBitmap {
		
		public Bitmap bitmap;
		public String path;
		
		public SavedBitmap(Bitmap bitmap, String path) {
			this.bitmap = bitmap;
			this.path = path;
		}
		
	}

	static public byte[] getJPEGRepresentationFromBitmap(Bitmap bitmap)
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		return outputStream.toByteArray();
	}

	static public Bitmap getOrientedBitmapFromBitmapAndPath(Bitmap bitmap, String filePath)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering getOrientedBitmapFromBitmapAndPath");
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
			Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting getOrientedBitmapFromBitmapAndPath");
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
		}
		catch (Exception exception)
		{
			Log.d(AirImagePickerUtils.TAG, "Couldn't fix bitmap orientation: " + exception.getMessage());
			Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting getOrientedBitmapFromBitmapAndPath");
			return bitmap;
		}
	}

	static public Bitmap getOrientedSampleBitmapFromPath(String filePath)
	{
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Entering getOrientedSampleBitmapFromPath");
		
		// Choose a sample size according the memory limit
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, options);
		int sampleSize = 1;
		while (options.outWidth/sampleSize * options.outHeight/sampleSize * 4 > AirImagePickerExtensionContext.BITMAP_MEMORY_LIMIT)
			sampleSize *= 2;
	
		// Decode the image
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		Bitmap sampleBitmap = BitmapFactory.decodeFile(filePath, options);
	
		// Fix orientation
		Bitmap orientedSampleBitmap = getOrientedBitmapFromBitmapAndPath(sampleBitmap, filePath);
	
		Log.d(AirImagePickerUtils.TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPath");
		return orientedSampleBitmap;
	}

	public static final String TAG = "AirImagePicker";

	static public File getTemporaryFile( String extension )
	{
		// Get or create folder for temp files
		File tempFolder = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
		if (!tempFolder.exists())
		{
			tempFolder.mkdirs();
			try
			{
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

	public static void notifyGalleryOfNewImage(Activity activity, File picture)
	{
		long current = System.currentTimeMillis();
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering notifyGalleryOfNewImage");
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, "My HelloPop Image " + current);
		values.put(MediaStore.Images.Media.DATE_ADDED, (int) (current/1000));
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		values.put(MediaStore.Images.Media.DATA, picture.getAbsolutePath());
		ContentResolver contentResolver = activity.getContentResolver();
		Uri base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		Uri newUri = contentResolver.insert(base, values);
		activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting notifyGalleryOfNewImage");
	}

	public static File savePictureInGallery(String albumName, byte[] pickedImageJPEGRepresentation)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering didSavePictureInGallery");
		
		long current = System.currentTimeMillis();
	
		// Save image to album
		File folder = new File(Environment.getExternalStorageDirectory() + File.separator + albumName);
		if (!folder.exists()) {
			folder.mkdir();
			try {
				new File(folder, ".nomedia").createNewFile();
			} catch (Exception e) {
				Log.d(TAG, "[AirImagePickerExtensionContext] exception = " + e.getMessage());
				Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (failed)");
				return null;
			}
		}
		File picture = new File(folder, "IMG_" + current);
	
		// Write Image to File
		try {
			FileOutputStream stream = new FileOutputStream(picture);
			stream.write(pickedImageJPEGRepresentation);
			stream.close();
		} catch (Exception exception) { 
			Log.d(TAG, "[AirImagePickerExtensionContext] exception = " + exception.getMessage());
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (failed)");
			return null; 
		}
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting didSavePictureInGallery (succeeded)");
		return picture;
	}

	public static String saveImageToTemporaryDirectory(Bitmap image) {
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering saveImageToTemporaryDirectory");
		String path = "";
	    FileOutputStream outputStream;
	    try {
			File file = getTemporaryFile(".jpg");
	    	outputStream = new FileOutputStream(file);
	        outputStream.write(getJPEGRepresentationFromBitmap(image));
	        outputStream.close();
	        path = file.getAbsolutePath();
			Log.d(TAG, "[AirImagePickerExtensionContext] saveImageToTemporaryDirectory path:"+path);
	    }
	    catch (IOException e) {
			Log.e(TAG, "[AirImagePickerExtensionContext] saveImageToTemporaryDirectory error:"+e.toString());
	        // Error while creating file
	    }
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting saveImageToTemporaryDirectory");
	    return path;
	}

	public static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering resizeImage");
		Bitmap result = image;
		// make sure that the image has the correct height
		if (image.getWidth() > maxWidth || image.getHeight() > maxHeight
				&& maxWidth != -1 && maxHeight != -1)
		{
	        float reductionFactor = Math.max(image.getWidth() / maxWidth, image.getHeight() / maxHeight);
	        
			result = Bitmap.createScaledBitmap( image, (int)(image.getWidth()/reductionFactor), (int)(image.getHeight()/reductionFactor), true);
			Log.d(TAG, "[AirImagePickerExtensionContext] resized image");
		}
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting resizeImage");
		return result;
	}

	public static Boolean isPicasa(String path) 
	{
		return path.contains("com.google.android.gallery3d") || 
				path.contains("com.sec.android.gallery3d");
	}

	public static String getPath(Activity activity, Uri selectedImage)
	{
		final String[] filePathColumn = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
		Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
		
		// Some devices return an URI of com.android instead of com.google.android
		if (selectedImage.toString().startsWith("content://com.android.gallery3d.provider"))
		{
			selectedImage = Uri.parse( selectedImage.toString().replace("com.android.gallery3d", "com.google.android.gallery3d") );
		}
	
		if (cursor != null)
		{
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(MediaColumns.DATA);
			
			// if it is a picassa image on newer devices with OS 3.0 and up
			if (isPicasa(selectedImage.toString()))
			{
				columnIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
				return selectedImage.toString();
			}
			else
			{
				return cursor.getString(columnIndex);
			}
		}
		else if ( selectedImage != null && selectedImage.toString().length() > 0 )
		{
			return selectedImage.toString();
		}
		else return null;
	}

	public static Bitmap createThumbnailForVideo(String videoPath)
	{
		return ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MICRO_KIND);
	}

	public static final int NO_ACTION = -1;
	public static final int GALLERY_IMAGES_ONLY_ACTION = 0;
	public static final int GALLERY_VIDEOS_ONLY_ACTION = 1;
	public static final int CAMERA_IMAGE_ACTION = 2;
	public static final int CAMERA_VIDEO_ACTION = 3;
	public static final int CROP_ACTION = 4;

	public static Intent getIntentForAction(int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering getIntentForAction");
		Intent intent;
		switch (action)
		{
		case GALLERY_IMAGES_ONLY_ACTION:
			intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return intent;
			
		case GALLERY_VIDEOS_ONLY_ACTION:
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*");
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return intent;
			
		case CAMERA_IMAGE_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
		case CAMERA_VIDEO_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			
		case CROP_ACTION:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return new Intent("com.android.camera.action.CROP");
		
		default:
			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getIntentForAction");
			return null;
		}
	}

	public static Boolean isCropAvailable(Activity activity)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] isCropAvailable");
	
		final PackageManager packageManager = activity.getPackageManager();
		Intent intent = getIntentForAction(CROP_ACTION);
		intent.setType("image/*");
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting isCropAvailable");
		
		return list.size() > 0;
	}

	//FRE
	public static Boolean isCameraAvailable(Activity activity)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering isCameraAvailable");
		Boolean hasCameraFeature = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
		Boolean hasFrontCameraFeature = activity.getPackageManager().hasSystemFeature("android.hardware.camera.front");
		Boolean isAvailable = (hasFrontCameraFeature || hasCameraFeature) && 
				(AirImagePickerUtils.isActionAvailable(activity, CAMERA_IMAGE_ACTION) || AirImagePickerUtils.isActionAvailable(activity, CAMERA_VIDEO_ACTION));
	
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting isCameraAvailable");
		return isAvailable;
	}

	public static Boolean isActionAvailable(Activity activity, int action)
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering isActionAvailable");
		if(action == CROP_ACTION) {
			return isCropAvailable(activity);
		}
		
		final PackageManager packageManager = activity.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(getIntentForAction(action), PackageManager.MATCH_DEFAULT_ONLY);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting isActionAvailable");
		
		return list.size() > 0;
	}

	public static Boolean isImagePickerAvailable(Activity activity)
	{
		return isActionAvailable(activity, GALLERY_IMAGES_ONLY_ACTION);
	}

	public static SavedBitmap orientAndSaveImage(Activity activity, String filePath, int maxWidth, int maxHeight, String albumName )
	{
		Log.d(TAG, "[AirImagePickerExtensionContext] Entering processPickedImage");
		
	
		Bitmap image = getOrientedSampleBitmapFromPath(filePath);
	
		if(albumName != null) {
			File savedPicture = savePictureInGallery(albumName, getJPEGRepresentationFromBitmap(image));
			if(savedPicture != null) {
				notifyGalleryOfNewImage(activity, savedPicture);
			}
		}
		
		image = resizeImage(image, maxWidth, maxHeight);
		String outputPath = saveImageToTemporaryDirectory(image);
		
		Log.d(TAG, "[AirImagePickerExtensionContext] Exiting processPickedImage");
		
		return new SavedBitmap(image, outputPath);
	}
	
	
	
//	private Bitmap getOrientedSampleBitmapFromPicassa(String filePath)
//	{
//		Log.d(TAG, "[AirImagePickerExtensionContext] Entering getOrientedSampleBitmapFromPicassa");
//		
//		// PICASSA PICKING IS NOT SUPPORTED FOR NOW
//		// http://dimitar.me/how-to-get-picasa-images-using-the-image-picker-on-android-devices-running-any-os-version/
//		
//		File cacheDir;
//		
//		// if the device has a SD card
//		if (android.os.Environment.getExternalStorageDirectory().equals(android.os.Environment.MEDIA_MOUNTED)){
//			Log.d(TAG, "[AirImagePickerExtensionContext] cacheDir from getExternalStorageDirectory()");
//			cacheDir = new File(android.os.Environment.getExternalStorageDirectory(),".OCFL311");
//		} else {
//			Log.d(TAG, "[AirImagePickerExtensionContext] cacheDir from getCacheDir()");
//			cacheDir = new File(Environment.getExternalStorageDirectory()+File.separator+"airImagePicker");
//		}
//		
//		if (!cacheDir.exists())
//			cacheDir.mkdirs();
//		
//		Log.d(TAG, "[AirImagePickerExtensionContext] create file in cache dir");
//		File f = new File( cacheDir, "image_file_name.jpg");
//		
//		try
//		{
//			Log.d(TAG, "[AirImagePickerExtensionContext] open input stream in picassa");
//			
//			InputStream is = null;
//			if ( filePath.startsWith("content://com.google.android.gallery3d") ) {
//				Log.d(TAG, "[AirImagePickerExtensionContext] 1");
//				is = getActivity().getApplicationContext().getContentResolver().openInputStream(Uri.parse(filePath));
//				Log.d(TAG, "[AirImagePickerExtensionContext] 2");
//			} else {
//				is = new URL(filePath.toString()).openStream();
//			}
//			
//			Log.d(TAG, "[AirImagePickerExtensionContext] open outputstream in file system");
//			OutputStream os = new FileOutputStream(f);
//			
//			Log.d(TAG, "[AirImagePickerExtensionContext] copy bytes from picassa to file system");
//			// 
//			byte[] buffer = new byte[1024];
//			int len;
//			while( (len = is.read(buffer)) != -1 ) {
//				os.write(buffer,0,len);
//			}
//			
//			Log.d(TAG, "[AirImagePickerExtensionContext] done copying, close OutputStream");
//			os.close();
//			Bitmap b = getOrientedSampleBitmapFromPath(f.getAbsolutePath()); 
//			
//			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPicassa");
//			return b;
//		} 
//		catch (Exception ex) {
//			Log.d( TAG, "[AirImagePickerExtensionContext] Exception: " + ex.getMessage());
//			Log.d(TAG, "[AirImagePickerExtensionContext] Exiting getOrientedSampleBitmapFromPicassa");
//			return null;
//		}
//	}
//	
//	private void deleteTemporaryImageFile(String filePath)
//	{
//		Log.d(TAG, "[AirImagePickerExtensionContext] deleting file:" + filePath);
//		new File(filePath).delete();
//	}

//	private String getPathForProcessedPickedImage(String filePath)
//	{
//		processPickedImage(filePath);
//
//		File tempFile = getTemporaryImageFile(".jpg");
//		try
//		{
//			FileOutputStream stream = new FileOutputStream(tempFile);
//			stream.write(_pickedImageJPEGRepresentation);
//			stream.close();
//		}
//		catch (Exception exception) {}
//
//		return tempFile.getAbsolutePath();
//	}

}
