package com.freshplanet.ane.AirImagePicker.functions;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * Created by peternicolai on 7/29/16.
 */
public final class RecentPhotosTasks  {

    public static class MediaQueryTask extends AsyncQueryHandler
    {
        public interface OnQueryCompleteListener {
            public void onQueryComplete(Cursor data, int position);
        }

        private WeakReference<OnQueryCompleteListener> _listener;

        public MediaQueryTask(ContentResolver cr, OnQueryCompleteListener listener) {
            super(cr);
            this._listener = new WeakReference<OnQueryCompleteListener>(listener);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            if (_listener != null && _listener.get() != null) {
                _listener.get().onQueryComplete(cursor, token);
            } else {
                if(cursor != null){
                    cursor.close();
                }
            }
        }
    }


    //As in iOS, we can't create an AS3 BitmapData to return unless we're in the stack of an FREFunction.
    //So we pass the id back with the bitmap to be stored until AS3 asks for it.
    public static class BitmapFactoryTask extends AsyncTask<Integer, Void, Bitmap> {

        private ContentResolver contentResolver;
        private String imageId;
        private Uri imageUri;
        private int imageWidth;
        private int imageHeight;
        private int requestId;
        private WeakReference<OnBitmapLoadedListener> listener;

        public interface OnBitmapLoadedListener {
            public void onBitmapLoaded(int requestId, Bitmap bitmap);
        }

        public static int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }


        public BitmapFactoryTask(ContentResolver cr, OnBitmapLoadedListener ls, String id, int reqId, int width, int height) {
            contentResolver = cr;
            listener = new WeakReference<OnBitmapLoadedListener>(ls);
            imageId = id;
            imageUri =  Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            imageWidth = width;
            imageHeight = height;
            requestId = reqId;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            InputStream is;
            try {
                is = contentResolver.openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                AirImagePickerExtension.log("Couldn't resolve path");
                return null;
            }
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, imageWidth, imageHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try {
                is = contentResolver.openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                return null;
            }

            return BitmapFactory.decodeStream(is, null, options);
        }

        public static Bitmap swapColors(Bitmap inBitmap)
        {
            AirImagePickerExtension.log("[Error] Entering swapColors()");
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

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(listener != null && listener.get() != null) {
                listener.get().onBitmapLoaded(requestId, bitmap);
            }
        }
    }
}
