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
package com.freshplanet.ane.AirImagePicker {
import com.freshplanet.ane.AirImagePicker.enums.AirImagePickerPermissionStatus;
import com.freshplanet.ane.AirImagePicker.events.AirImagePickerDataEvent;
import com.freshplanet.ane.AirImagePicker.events.AirImagePickerRecentImagesEvent;

import flash.display.BitmapData;
import flash.events.EventDispatcher;
import flash.events.StatusEvent;
import flash.external.ExtensionContext;
import flash.geom.Rectangle;
import flash.system.Capabilities;
import flash.utils.ByteArray;

public class AirImagePicker extends EventDispatcher {

	// --------------------------------------------------------------------------------------//
	//																						 //
	// 									   PUBLIC API										 //
	// 																						 //
	// --------------------------------------------------------------------------------------//

	/** supported on Android and iOS devices. */
	public static function get isSupported() : Boolean {
		return isAndroid || isIOS;
	}

	/**
	 * AirImagePicker instance
	 * @return AirImagePicker instance
	 */
	public static function get instance() : AirImagePicker {
		return _instance ? _instance : new AirImagePicker();
	}

	/**
	 * If <code>true</code>, logs will be displayed at the Actionscript level.
	 */
	public function get logEnabled() : Boolean {
		return _logEnabled;
	}

	public function set logEnabled( value : Boolean ) : void {
		_logEnabled = value;
	}

	/**
	 * Display the image picker
	 * @param maxImageWidth max width of returned image
	 * @param maxImageHeight max height of returned image
	 * @param crop allow user to crop
	 * @param anchor for iPads. Anhor point of the popUp
	 */
	public function displayImagePicker(maxImageWidth:int = -1,
	                                   maxImageHeight:int = -1,
	                                   crop : Boolean = false,
	                                   anchor : Rectangle = null):void {
		if(isSupported) {
			anchor ?
					_context.call("displayImagePicker",
					maxImageWidth,
					maxImageHeight,
					crop,
					anchor.x,
					anchor.y,
					anchor.width,
					anchor.height)
					:
					_context.call("displayImagePicker",
							maxImageWidth,
							maxImageHeight,
							crop)

		}

	}

	/**
	 * Display the camera
	 * @param maxImageWidth max width of returned image
	 * @param maxImageHeight max height of returned image
	 * @param crop allow user to crop
	 */
	public function displayCamera(maxImageWidth:int = -1,
	                                   maxImageHeight:int = -1,
	                                   crop : Boolean = false):void {
		if(isSupported)
					_context.call("displayCamera",
							maxImageWidth,
							maxImageHeight,
							crop);

	}

	/**
	 * Load most recent images from the device
	 * @param loadLimit how many images to load
	 * @param maxImageWidth max width of returned image
	 * @param maxImageHeight max height of returned image
	 */
	public function loadRecentImages(loadLimit:int,
	                                 maxImageWidth:int = -1,
	                                 maxImageHeight:int = -1):void {
		if(isSupported)
			_context.call("loadRecentImages",
					loadLimit,
					maxImageWidth,
					maxImageHeight);

	}

	/**
	 * Get Camera permission status
	 * @return
	 */
	public function getCameraPermissionStatus():AirImagePickerPermissionStatus {
		if(isSupported) {
			if(isAndroid)
				return AirImagePickerPermissionStatus.AUTHORIZED;
			else
				return AirImagePickerPermissionStatus.fromValue(_context.call("getCameraPermissionStatus") as String);
		}

		return AirImagePickerPermissionStatus.NOT_DETERMINED;
	}

	/**
	 * Get Gallery permission status
	 * @return
	 */
	public function getGalleryPermissionStatus():AirImagePickerPermissionStatus {
		if(isSupported) {
			if(isAndroid)
				return AirImagePickerPermissionStatus.AUTHORIZED;
			else
				return AirImagePickerPermissionStatus.fromValue(_context.call("getGalleryPermissionStatus") as String);
		}


		return AirImagePickerPermissionStatus.NOT_DETERMINED;
	}


	/**
	 * Open phone settings
	 * @return
	 */
	public function openSettings():void {
		if(isSupported)
			_context.call("openSettings");


	}


	// --------------------------------------------------------------------------------------//
	//																						 //
	// 									 	PRIVATE API										 //
	// 																						 //
	// --------------------------------------------------------------------------------------//

	private static const EXTENSION_ID : String = "com.freshplanet.ane.AirImagePicker";
	private static var _instance : AirImagePicker;
	private var _context : ExtensionContext = null;
	private var _logEnabled : Boolean = true;

	/**
	 * "private" singleton constructor
	 */
	public function AirImagePicker() {
		if (!_instance) {
			_context = ExtensionContext.createExtensionContext(EXTENSION_ID, null);
			if (!_context) {
				log("ERROR - Extension context is null. Please check if extension.xml is setup correctly.");
				return;
			}
			_context.addEventListener(StatusEvent.STATUS, onStatus);

			_instance = this;
		}
		else {
			throw Error("This is a singleton, use instance, do not call the constructor directly.");
		}
	}

	private function log(message:String):void {
		if (_logEnabled) trace("[AirImagePicker] " + message);
	}

	private function onStatus(event:StatusEvent):void {
		var imagePath:String;
		var bitmapData:BitmapData;
		var byteArray:ByteArray;

		if (event.code == "photoChosen") {
			imagePath = event.level;
			bitmapData = _context.call("internalGetChosenPhotoBitmapData", imagePath) as BitmapData;
			byteArray = _context.call("internalGetChosenPhotoByteArray", imagePath) as ByteArray;
			_context.call("internalRemoveStoredImage", imagePath);
			this.dispatchEvent(new AirImagePickerDataEvent(AirImagePickerDataEvent.IMAGE_CHOSEN, new AirImagePickerImageData(bitmapData, byteArray)));
		}
		else if (event.code == "recentResult") {
			var result:Object = JSON.parse(event.level);
			var imagePathsArray:Object = result.imagePaths;
			var images:Vector.<AirImagePickerImageData> = new <AirImagePickerImageData>[];
			for (var i:int = 0; i < imagePathsArray.length; i++) {
				imagePath = imagePathsArray[i];
				bitmapData = _context.call("internalGetChosenPhotoBitmapData", imagePath) as BitmapData;
				byteArray = _context.call("internalGetChosenPhotoByteArray", imagePath) as ByteArray;
				_context.call("internalRemoveStoredImage", imagePath);
				images.push(new AirImagePickerImageData(bitmapData, byteArray));

			}
			this.dispatchEvent(new AirImagePickerRecentImagesEvent(AirImagePickerRecentImagesEvent.ON_LOAD_RESULT, images));

		}
		else if (event.code == AirImagePickerDataEvent.CANCELLED) {
			this.dispatchEvent(new AirImagePickerDataEvent(AirImagePickerDataEvent.CANCELLED));
		}
		else if (event.code == AirImagePickerDataEvent.GALLERY_PERMISSION_ERROR || event.code == AirImagePickerDataEvent.CAMERA_PERMISSION_ERROR) {
			this.dispatchEvent(new AirImagePickerDataEvent(event.code));
		}
		else if (event.code == AirImagePickerRecentImagesEvent.PERMISSION_ERROR) {
			this.dispatchEvent(new AirImagePickerRecentImagesEvent(AirImagePickerRecentImagesEvent.PERMISSION_ERROR));
		}
		else if (event.code == "log") {
			log(event.level);
		}
	}

	private static function get isIOS():Boolean {
		return Capabilities.manufacturer.indexOf("iOS") > -1 && Capabilities.os.indexOf("x86_64") < 0 && Capabilities.os.indexOf("i386") < 0;
	}

	private static function get isAndroid():Boolean {
		return Capabilities.manufacturer.indexOf("Android") > -1;
	}

}
}
