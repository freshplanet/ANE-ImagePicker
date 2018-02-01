package com.freshplanet.ane.AirImagePicker.events {
import flash.events.Event;

public class AirImagePickerErrorEvent extends Event {

	public static const CAMERA_PERMISSION_ERROR:String = "AirImagePickerErrorEvent_cameraPermissionError";
	public static const GALLERY_PERMISSION_ERROR:String = "AirImagePickerErrorEvent_galleryPermissionError";
	public static const ERROR:String = "AirImagePickerErrorEvent_error";

	private var _error:String;

	public function AirImagePickerErrorEvent(type:String, error:String = null, bubbles:Boolean = false, cancelable:Boolean = false) {
		super(type, bubbles, cancelable);
		_error = error;
	}

	public function get error():String {
		return _error;
	}
}
}
