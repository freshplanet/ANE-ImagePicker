package com.freshplanet.ane.AirImagePicker.tasks;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.freshplanet.ane.AirImagePicker.AirImagePickerExtension;

import android.os.AsyncTask;
import android.util.Log;

public class UploadToGoogleCloudStorageAsyncTask extends AsyncTask<String, Void, String> {

	private static String TAG = "AirImagePicker";
	
	private final HttpClient client = new DefaultHttpClient();
	private String response = null;
	
	private String mediaPath;
	private JSONObject uploadParams;
	
	public UploadToGoogleCloudStorageAsyncTask(JSONObject uParams, String mPath)
	{
		mediaPath = mPath;
		uploadParams = uParams;
	}
	
	@Override
	protected String doInBackground(String... urls) {
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Entering doInBackground()");
		
		byte[] result = null;
		HttpPost post = new HttpPost(urls[0]);
		
		try {
			Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: Prepare for httpPostData");
			// prepare for httpPost data
			String boundary = "b0undaryFP";
			
			
			Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: Get the byte[] of the media we want to upload");
			// Get the byte[] of the media we want to upload
			byte[] mediaBytes = toByteArray(mediaPath);
			
			Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: build the data");
			// build the data 
			ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
			for (@SuppressWarnings("unchecked")
			Iterator<String> keys = uploadParams.keys(); keys.hasNext();) {
				String key = keys.next();
				String value = uploadParams.getString(key);
				requestBody.write(("\r\n--%@\r\n".replace("%@", boundary)).getBytes());
				requestBody.write(("Content-Disposition: form-data; name=\"%@\"\r\n\r\n%@".
						replaceFirst("%@", key).replaceFirst("%@", value)).getBytes());
			}
			requestBody.write(("\r\n--%@\r\n".replace("%@", boundary)).getBytes());
			requestBody.write(("Content-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n\r\n").getBytes());
			requestBody.write(mediaBytes);
			// this is the final boundary
			requestBody.write(("\r\n--%@\r\n".replace("%@", boundary)).getBytes());
			
			Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: Set content-type and content of http post");
			// Set content-type and content of http post
			post.setHeader("Content-Type", "multipart/form-data; boundary="+boundary);
			post.setEntity(new ByteArrayEntity( requestBody.toByteArray() ));
			
			Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: execute post.");
			// execute post.
			HttpResponse httpResponse = client.execute(post);
			StatusLine statusResponse = httpResponse.getStatusLine();
			if (statusResponse.getStatusCode() == HttpURLConnection.HTTP_OK)
			{
				result = EntityUtils.toByteArray(httpResponse.getEntity());
				response = new String(result, "UTF-8");
				Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: got a response: " + response);
			}
			else
			{
				Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ ERR: status code: " + statusResponse.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Exiting doInBackground()");
		return response;
	}
	
	@Override
	protected void onPostExecute(String result) {
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Entering onPostExecute()");
		
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] ~~~ DBG: dispatching to actionscript a StatusEvent: " + result);
		
		AirImagePickerExtension.context.dispatchStatusEventAsync("FILE_UPLOAD_DONE", result);
		
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Exiting onPostExecute()");
	}
	
	private byte[] toByteArray( String path )
	{
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Entering toByteArray()");
		
		File file = new File(path);
		int size = (int) file.length();
		byte[] bytes = new byte[size];
		
			BufferedInputStream buf;
			try {
				buf = new BufferedInputStream(new FileInputStream(file));
				buf.read(bytes, 0, bytes.length);
				buf.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		Log.d(TAG, "[UploadToGoogleCloudStorageAsyncTask] Exiting toByteArray()");
		return bytes;
	}


}
