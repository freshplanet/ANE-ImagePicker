package com.freshplanet.ane.AirImagePicker.functions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREInvalidObjectException;
import com.adobe.fre.FREObject;
import com.adobe.fre.FRETypeMismatchException;
import com.adobe.fre.FREWrongThreadException;

public class UploadToServer implements FREFunction {

	private static String TAG = "AirImagePicker";
	
	@Override
	public FREObject call(FREContext ctx, FREObject[] args) 
	{
		Log.d(TAG, "[UploadToServer] Entering call()");
		
		String localURL = null;
		String uploadURL = null;
		JSONObject uploadParamsJSON = null;
		
		try {
			localURL = args[0].getAsString();
			uploadURL = args[1].getAsString();
			uploadParamsJSON = new JSONObject(args[2].getAsString());
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (FRETypeMismatchException e) {
			e.printStackTrace();
		} catch (FREInvalidObjectException e) {
			e.printStackTrace();
		} catch (FREWrongThreadException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (localURL != null && uploadURL != null && uploadParamsJSON != null)
		{
			try {
				uploadToGoogleCloudStorage(localURL, uploadURL, uploadParamsJSON);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Log.d(TAG, "[UploadToServer] Exiting call()");
		return null;
	}
	
	private void uploadToGoogleCloudStorage( String mediaPath, String uploadURL, JSONObject uploadParams ) throws JSONException, ClientProtocolException, IOException
	{
		// Get the byte[] of the media we want to upload
		byte[] mediaBytes = toByteArray(mediaPath);
		
		// Connection to GCS
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(uploadURL);
		
		// prepare for httpPost data
		String boundary = "b0undary";
		
		// build the data 
		StringBuffer requestBody = new StringBuffer();
		for (Iterator<String> keys = uploadParams.keys(); keys.hasNext();) {
			String key = keys.next();
			String value = uploadParams.getString(key);
			requestBody.append("\r\n--%@\r\n".replace("%@", boundary));
			requestBody.append("Content-Disposition: form-data; name=\"%@\"\r\n\r\n%@".
					replaceFirst("%@", key).replaceFirst("%@", value));
		}
		requestBody.append("\r\n--%@\r\n".replace("%@", boundary));
		requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n\r\n");
		requestBody.append(new String(mediaBytes));
		// this is the final boundary
		requestBody.append("\r\n--%@\r\n".replace("%@", boundary));
		
		// send the data to GCS
		httpPost.setEntity(new ByteArrayEntity( requestBody.toString().getBytes() ));
		HttpResponse response = httpClient.execute(httpPost);
	}
	
	private byte[] toByteArray( String path )
	{
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
			
		return bytes;
	}

}
