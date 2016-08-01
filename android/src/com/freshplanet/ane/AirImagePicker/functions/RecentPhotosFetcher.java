package com.freshplanet.ane.AirImagePicker.functions;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import com.adobe.fre.*;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import org.json.JSONArray;

/**
 * Created by peternicolai on 7/29/16.
 */
public class RecentPhotosFetcher implements MyQueryHandler.OnQueryCompleteListener
{

    public static final String ANE_ERROR = "ANE_ERROR";
    public static final String  IMAGE_LIST_SUCCEEDED = "IMAGE_LIST_SUCCEEDED";
    public static final String  IMAGE_LIST_ERROR = "IMAGE_LIST_ERROR";
    public static final String  IMAGE_LOAD_ERROR = "IMAGE_LOAD_ERROR";
    public static final String  IMAGE_LOAD_TEMP = "IMAGE_LOAD_TEMP"; // a temp. crappy placeholder until the fetch completes
    public static final String  IMAGE_LOAD_CANCELLED = "IMAGE_LOAD_CANCELLED";
    public static final String  IMAGE_LOAD_SUCCEEDED = "IMAGE_LOAD_SUCCEEDED";

    private MyQueryHandler queryHandler;
    private int queryCount = 0;

    public RecentPhotosFetcher(ContentResolver cr)
    {
        queryHandler = new MyQueryHandler(cr, this);
    }

    @Override
    public void onQueryComplete(Cursor cursor, int position)
    {
        int maxResults = cursor.getCount();
        JSONArray results = new JSONArray();

        if (!cursor.moveToFirst()) {
            AirImagePickerExtension.context.dispatchResultEvent(IMAGE_LIST_ERROR, "No images available");
        }

        for (int i = 0; i < maxResults; ++i) {
            AirImagePickerExtension.log(cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC)));
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
            results.put(id);
            if(!cursor.moveToNext()) {
                break;
            }
        }

        AirImagePickerExtension.context.dispatchResultEvent(IMAGE_LIST_SUCCEEDED, results.toString());

    }

    public final FREFunction getRecentImageIds = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering getRecentImageIds");
            int maxResults = 0;
            try {
                maxResults = freObjects[0].getAsInt();
                String[] projection = new String[]{
                        MediaStore.Images.ImageColumns._ID,
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.ImageColumns.DATE_TAKEN,
                        MediaStore.Images.ImageColumns.MIME_TYPE,
                        MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC
                };

                queryHandler.startQuery(queryCount++, this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN +
                                String.format(" DESC LIMIT %d", maxResults));

                return null;

            } catch (FREWrongThreadException e) {
                e.printStackTrace();
            } catch (FREInvalidObjectException e) {
                e.printStackTrace();
            } catch (FRETypeMismatchException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

//
//    //If you call this and never cancel or retrieve the image fetches, they will stay in memory forever.
//    public function fetchImages(imageIds:Array, width:int, height:int, zoomedFillMode:Boolean):Array
//    {
//        return _context.call("fetchImages", imageIds, width, height, zoomedFillMode) as Array || [];
//    }

    public final FREFunction fetchImages = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AirImagePickerExtension.log("Entering fetchImages");
                FREArray imageIds = (FREArray)freObjects[0];
                int width = freObjects[1].getAsInt();
                int height = freObjects[2].getAsInt();
                boolean zoomedFillMode = freObjects[3].getAsBool();


            } catch (FRETypeMismatchException e) {
                e.printStackTrace();
            } catch (FREInvalidObjectException e) {
                e.printStackTrace();
            } catch (FREWrongThreadException e) {
                e.printStackTrace();
            }
            return null;
        }
    };
//
//    public function retrieveFetchedImage(requestId:int):BitmapData
//    {
//        return _context.call("retrieveFetchedImage", requestId) as BitmapData;
//    }

    public final FREFunction retrieveFetchedImage = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering retrieveFetchedImage");
            return null;
        }
    };
//
//    public function cancelImageFetch(requestId:int):void
//    {
//        _context.call("cancelImageFetch", requestId);
//    }

    public final FREFunction cancelImageFetch = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering cancelImageFetch");
            return null;
        }
    };

}
