package com.freshplanet.ane.AirImagePicker.functions;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import com.adobe.fre.*;
import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by peternicolai on 7/29/16.
 */
public class RecentPhotosFetcher implements RecentPhotosTasks.MediaQueryTask.OnQueryCompleteListener,
        RecentPhotosTasks.BitmapFactoryTask.OnBitmapLoadedListener
{

    public static final String ANE_ERROR = "ANE_ERROR";
    public static final String  IMAGE_LIST_SUCCEEDED = "IMAGE_LIST_SUCCEEDED";
    public static final String  IMAGE_LIST_ERROR = "IMAGE_LIST_ERROR";
    public static final String  IMAGE_LOAD_ERROR = "IMAGE_LOAD_ERROR";
    public static final String  IMAGE_LOAD_TEMP = "IMAGE_LOAD_TEMP"; // a temp. crappy placeholder until the fetch completes
    public static final String  IMAGE_LOAD_CANCELLED = "IMAGE_LOAD_CANCELLED";
    public static final String  IMAGE_LOAD_SUCCEEDED = "IMAGE_LOAD_SUCCEEDED";

    private RecentPhotosTasks.MediaQueryTask queryHandler;
    private int queryCount = 0;
    private int fetchCount = 0;

    private ConcurrentHashMap<Integer, Bitmap> loadedBitmaps;

    public RecentPhotosFetcher(ContentResolver cr)
    {
        queryHandler = new RecentPhotosTasks.MediaQueryTask(cr, this);
        loadedBitmaps = new ConcurrentHashMap<Integer, Bitmap>();
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
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATE_TAKEN,
                        MediaStore.Images.Media.MIME_TYPE,
                };

                queryHandler.startQuery(queryCount++, this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.Media.DATE_TAKEN +
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

    public final FREFunction fetchImages = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            try {
                AirImagePickerExtension.log("Entering fetchImages");
                FREArray imageIds = (FREArray)freObjects[0];
                FREArray requestIds = FREArray.newArray((int)imageIds.getLength());

                int width = freObjects[1].getAsInt();
                int height = freObjects[2].getAsInt();
                boolean zoomedFillMode = freObjects[3].getAsBool();
                for (int i = 0; i < imageIds.getLength(); ++i) {
                    int reqId = fetchCount++;
                    RecentPhotosTasks.BitmapFactoryTask bitmapFactoryTask = new RecentPhotosTasks.BitmapFactoryTask(
                            freContext.getActivity().getContentResolver(), RecentPhotosFetcher.this,
                            imageIds.getObjectAt(i).getAsString(), reqId, width, height);
                    bitmapFactoryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    requestIds.setObjectAt(i, FREObject.newObject(reqId));
                }
                return requestIds;

            } catch (FRETypeMismatchException e) {
                e.printStackTrace();
            } catch (FREInvalidObjectException e) {
                e.printStackTrace();
            } catch (FREWrongThreadException e) {
                e.printStackTrace();
            } catch (FREASErrorException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    @Override
    public void onBitmapLoaded(int reqId, Bitmap bitmap)
    {
        String responseJSON =  String.format("{\"requestId\": %d}", reqId);
        if(bitmap == null) {
            AirImagePickerExtension.context.dispatchResultEvent(IMAGE_LOAD_ERROR, responseJSON);
            return;
        }
        loadedBitmaps.put(reqId, bitmap);
        AirImagePickerExtension.context.dispatchResultEvent(IMAGE_LOAD_SUCCEEDED, responseJSON);
    }

    public final FREFunction retrieveFetchedImage = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering retrieveFetchedImage");

            try {
                int requestId = freObjects[0].getAsInt();
                if(!loadedBitmaps.containsKey(requestId)) {
                    AirImagePickerExtension.log("retrieveFetchedImage has no key for request id");
                    return null;
                }
                Bitmap bitmap = loadedBitmaps.get(requestId);
                loadedBitmaps.remove(requestId);

                Byte color[] = {0, 0, 0, 0};
                FREBitmapData as3BitmapData = null;

                try {
                    as3BitmapData = FREBitmapData.newBitmapData(bitmap.getWidth(),
                            bitmap.getHeight(), false, color);
                    as3BitmapData.acquire();
                    bitmap.copyPixelsToBuffer(as3BitmapData.getBits());
                    as3BitmapData.release();
                } catch (Exception e) {
                    AirImagePickerExtension.log("retrieveFetchedImage error trying to create bitmapdata", e);
                }

                return as3BitmapData;

            } catch (FRETypeMismatchException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            } catch (FREInvalidObjectException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            } catch (FREWrongThreadException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            }
            AirImagePickerExtension.log("retrieveFetchedImage returning null!");
            return null;
        }

    };

    public final FREFunction retrieveFetchedImageAsFile = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering retrieveFetchedImageAsFile");

            try {
                int requestId = freObjects[0].getAsInt();
                if(!loadedBitmaps.containsKey(requestId)) {
                    AirImagePickerExtension.log("retrieveFetchedImage has no key for request id");
                    return null;
                }

                int maxWidth = freObjects[1].getAsInt();
                int maxHeight = freObjects[2].getAsInt();

                Bitmap bitmap = loadedBitmaps.get(requestId);
                loadedBitmaps.remove(requestId);

                if(bitmap == null) {
                    return null;
                }

                double maxScale = Math.min((double)maxWidth / bitmap.getWidth(), (double)maxHeight / bitmap.getHeight());

                if (maxScale < 1.0) {
                    maxWidth = (int)(maxScale * maxWidth + 0.5);
                    maxHeight = (int)(maxScale * maxHeight + 0.5);
                    bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);
                }

                FREByteArray as3bytes = null;
                try {
                    as3bytes = FREByteArray.newByteArray();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
                    as3bytes.setProperty("length", FREObject.newObject(out.size()));
                    as3bytes.acquire();
                    as3bytes.getBytes().put(out.toByteArray());
                } catch (Exception e) {
                    AirImagePickerExtension.log(e.getStackTrace().toString());
                }

                if(as3bytes != null) {
                    as3bytes.release();
                }

                return as3bytes;

            } catch (FRETypeMismatchException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            } catch (FREInvalidObjectException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            } catch (FREWrongThreadException e) {
                AirImagePickerExtension.log("retrieveFetchedImage", e);
                e.printStackTrace();
            }
            AirImagePickerExtension.log("retrieveFetchedImageAsFile returning null!");
            return null;
        }

    };

    public final FREFunction cancelImageFetch = new FREFunction() {
        @Override
        public FREObject call(FREContext freContext, FREObject[] freObjects) {
            AirImagePickerExtension.log("Entering cancelImageFetch");
            return null;
        }
    };

}
