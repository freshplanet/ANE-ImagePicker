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

package com.freshplanet.ane.AirImagePicker
{
	import flash.display.BitmapData;
	import flash.display.Stage3D;
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.StatusEvent;
	import flash.external.ExtensionContext;
	import flash.geom.Rectangle;
	import flash.system.Capabilities;
	import flash.utils.ByteArray;

	/**
	*  Take care of picking images and videos on iOS/Android
	*
	*  CALLBACKS:   
	*
	*   Because we are handling both videos and images, the callbacks to every 
	*	Method in this native extension have the following form: <code>function( status:String, ...mediaArgs ):void</code>
	*
	*	status:  Was the picking operation succcessful (STATUS_OK), cancelled by the user
	*	(STATUS_DID_CANCEL), STATUS_ERROR when there's an error during the creation process, or STATUS_NOT_SUPPORTED 
	*   when the requested feature is not supported by your device.
	*   For Android, there's a specific status, STATUS_PICASSA_NOT_SUPPORTED, used when the user tries
	* 	to pick an image from a Picassa Web Album.
	*
	*   mediaArgs:  An Array with different data according to the media type.
	*		
	*    	Images :  A BitmapData instance representing the image, and a ByteArray instance containing
	*					a JPEG representation of the image.
	*
	*		Videos: A String containing the path of the video stored on the device, A BitmapData 
	*					instance for a thumbnail of the video, and a ByteArray instance containing
	*					a JPEG representation of the thumbnail.
	*
	*	You can use StageVideo or FreshPlanet's ANE-Video to play the video on your AIR App.	
	*
	*	@see http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/media/StageVideo.html
	*   @see https://github.com/freshplanet/ANE-Video
	**/
	public class AirImagePicker extends EventDispatcher
	{
		// --------------------------------------------------------------------------------------//
		//																						 //
		// 									   PUBLIC API										 //
		// 																						 //
		// --------------------------------------------------------------------------------------//

		public static const STATUS_OK:String = "OK";
		public static const STATUS_ERROR:String = "ERROR";
		public static const STATUS_DID_CANCEL:String = "DID_CANCEL";
		public static const STATUS_NOT_SUPPORTED:String = "NOT_SUPPORTED";
		public static const STATUS_PICASSA_NOT_SUPPORTED:String = "PICASSA_NOT_SUPPORTED";

		/** AirImagePicker is supported on iOS and Android devices. */
		public static function get isSupported() : Boolean
		{
			return Capabilities.manufacturer.indexOf("iOS") > -1 || Capabilities.manufacturer.indexOf("Android") > -1;
		}
		
		public function AirImagePicker()
		{
			if (!_instance)
			{
				_isAndroid = Capabilities.manufacturer.indexOf("Android") > -1;
				_context = ExtensionContext.createExtensionContext(EXTENSION_ID, null);
				if (!_context)
				{
					log("ERROR - Extension context is null. Please check if extension.xml is setup correctly.");
					return;
				}
				_context.addEventListener(StatusEvent.STATUS, onStatus);
				
				_instance = this;
			}
			else
			{
				throw Error("This is a singleton, use getInstance(), do not call the constructor directly.");
			}
		}
		
		public static function getInstance() : AirImagePicker
		{
			return _instance ? _instance : new AirImagePicker();
		}
		
		/**
		 * If you use Stage3D, displaying the camera or the image gallery will cause a
		 * context loss.<br><br>
		 * 
		 * In order for this ANE not to crash your app, you need to be able to handle a
		 * lost context. If you are using Starling, set <code>handleLostContext</code>
		 * to <code>true</code> on your <code>Starling</code> instance.<br><br>
		 * 
		 * Even then, you will see a white screen before and after displaying the camera.
		 * In order to avoid this, you can pass a <code>BitmapData</code> overlay for the
		 * ANE to display on top of the white screen. For example, you could pass a
		 * screenshot of your user interface.
		 * 
		 * @param stage3D Your Stage3D instance. The ANE will listen to events in order
		 * to remove the overlay when a new context is created.
		 * @param overlay The overlay that will be drawn on the screen.
		 */
		public function setupStage3DOverlay(stage3D:Stage3D, overlay:BitmapData):void
		{
			if (_stage3D)
			{
				_stage3D.removeEventListener(Event.CONTEXT3D_CREATE, onContext3DCreated);
			}
			
			_stage3D = stage3D;
			_overlay = overlay;
			
			if (_stage3D)
			{
				_stage3D.addEventListener(Event.CONTEXT3D_CREATE, onContext3DCreated);
			}
		}
		
		/**
		 * If <code>true</code>, logs will be displayed at the Actionscript level.
		 * If <code>false</code>, logs will be displayed only at the native level.
		 */
		public function get logEnabled() : Boolean
		{
			return _logEnabled;
		}
		
		public function set logEnabled( value : Boolean ) : void
		{
			_logEnabled = value;
		}
		
		/**
		 * Returns <code>true</code> if the gallery image picker is available on the
		 * current device, <code>false</code> otherwise.
		 * 
		 * @see #displayImagePicker()
		 */
		public function isImagePickerAvailable() : Boolean
		{
			return isSupported && _context.call("isImagePickerAvailable");
		}
		
		/**
		 * Display the gallery image picker if it is available on the current device.
		 * Otherwise, do nothing.<br><br>
		 * 
		 * Once the user picks an image, it is returned to the provided callback function,
		 * both as a <code>BitmapData</code> and a JPEG-encoded <code>BypeArray</code>.
		 * If the user cancels, <code>null</code> is returned to the callback.<br><br>
		 *
		 * @param callback A callback function of the following form:
		 * <code>function( status:String, ...mediaArgs ):void</code>. See the ASDoc for this class for a in-depth
		 * explanation of the arguments passed to the callback.
		 * @param maxImageWidth Maximum width of the image.  only applies to image capture, not videos. default value is -1 (ignore)
		 * @param maxImageHeight Maximum height of the image.  only applies to image capture, not videos. default value is -1 (ignore)		 
		 * @param allowVideo if <code>true</code>, the picker will show videos stored on the device as well.
		 * @param crop If <code>true</code>, the image will be cropped with a 1:1 aspect
		 * ratio. A native UI will be displayed to allow the user to do the cropping
		 * properly. Default: <code>false</code>.
		 * @param anchor On the iPad, the image picker is displayed in a popover that
		 * doesn't cover the whole screen. This parameter is the anchor from which the
		 * popover will be presented. For example, it could be the bounds of the button
		 * on which the user clicked to display the image picker. Note that you should
		 * use absolute stage coordinates. Example: <code>var anchor:Rectangle = 
		 * myButton.getBounds(stage);</code>
		 * 
		 * @see #isImagePickerAvailable()
		 */
		public function displayImagePicker( callback : Function, 
			maxImageWidth:int = -1, 
			maxImageHeight:int = -1,
			allowVideo:Boolean = false, 
			crop : Boolean = false, 
			anchor : Rectangle = null) : void
		{
			if (!isImagePickerAvailable()) callback(STATUS_NOT_SUPPORTED, null);
			
			_callback = callback;
			
			if (anchor != null) _context.call("displayImagePicker", maxImageWidth, maxImageHeight, allowVideo, crop, anchor);
			else _context.call("displayImagePicker", maxImageWidth, maxImageHeight, allowVideo, crop);
			
			
		}
		
		/**
		 * Returns <code>true</code> if the camera is available on the current device,
		 * <code>false</code> otherwise.
		 * 
		 * @see #displayCamera()
		 */
		public function isCameraAvailable() : Boolean
		{
			return isSupported && _context.call("isCameraAvailable");
		}
		
		/**
		 * Display the camera if it is available on the current device. Otherwise, do
		 * nothing.<br><br>
		 * 
		 * Once the user takes a picture, it is returned to the provided callback function,
		 * both as a <code>BitmapData</code> and a JPEG-encoded <code>BypeArray</code>.
		 * If the user cancels, <code>null</code> is returned to the callback.
		 * 
		 * @param callback A callback function of the following form:
		 * <code>function( status:String, ...mediaArgs ):void</code>. See the ASDoc for this class for a in-depth
		 * explanation of the arguments passed to the callback.
		 * @param maxImageWidth Maximum width of the image.  only applies to image capture, not videos. default value is -1 (ignore)
		 * @param maxImageHeight Maximum height of the image.  only applies to image capture, not videos. default value is -1 (ignore)
		 * @param allowVideo if <code>true</code>, the picker will show videos stored on the device as well.
		 * @param crop If <code>true</code>, the image will be cropped with a 1:1 aspect
		 * ratio. A native UI will be displayed to allow the user to do the cropping
		 * properly. Default: <code>false</code>.
		 * @param albumName (optional) The name of the Album where you want to store the pictures on the device.  If this value is not passed,
		 * the native extension will not save the image on the device.
		 * 
		 * @see #isCameraAvailable()
		 */
		public function displayCamera( callback : Function,
			maxImageWidth:int = -1, 
			maxImageHeight:int = -1,
			allowVideo:Boolean = false, 
			crop : Boolean = false, 
			albumName:String = null,
		    chatLink:String = null) : void
		{
			if (!isCameraAvailable()) callback(STATUS_NOT_SUPPORTED, null);
			
			prepareToDisplayNativeUI(callback);
			if(_isAndroid) {
				if (albumName != null) _context.call("displayCamera", maxImageWidth, maxImageHeight, allowVideo, crop, albumName, chatLink);
				else _context.call("displayCamera", allowVideo, crop);
			} else {
				if (albumName != null) _context.call("displayCamera", maxImageWidth, maxImageHeight, allowVideo, crop, albumName);
				else _context.call("displayCamera", allowVideo, crop);
			}
		}

		public function cleanUpTemporaryDirectoryContent () : Boolean
		{
			return _context.call("cleanUpTemporaryDirectoryContent");
		}
		
		public function isCropAvailable(): Boolean 
		{
			return _context.call("isCropAvailable");
		}
		
		
		// --------------------------------------------------------------------------------------//
		//																						 //
		// 									 	PRIVATE API										 //
		// 																						 //
		// --------------------------------------------------------------------------------------//
		
		private static const EXTENSION_ID : String = "com.freshplanet.AirImagePicker";
		private static const LOG_TAG : String = "AirImagePicker";
		
		private static var _instance : AirImagePicker;
		
		private var _context : ExtensionContext;
		private var _logEnabled : Boolean = false;
		private var _callback : Function = null;
		private var _stage3D : Stage3D;
		private var _overlay : BitmapData;
		private var _context3DLost : Boolean = false;
		private var _isAndroid:Boolean;
		
		private function prepareToDisplayNativeUI( callback : Function ) : void
		{
			if (_stage3D && _overlay)
			{
				_callback = wrapCallbackForStage3D(callback);
				_context.call("displayOverlay", _overlay);
			}
			else
			{
				_callback = callback;
			}
		}
		
		private function wrapCallbackForStage3D( callback : Function ) : Function
		{
			return function(status:String, ...mediaArgs):void
			{
				if (_context3DLost)
				{
					var onContextRestored:Function = function(event:Event):void
					{
						_stage3D.removeEventListener(Event.CONTEXT3D_CREATE, onContextRestored);
						_context.call("removeOverlay");
						if (callback != null)
							callback.apply(null, [status].concat(mediaArgs));
					};
					_stage3D.addEventListener(Event.CONTEXT3D_CREATE, onContextRestored);
				}
				else
				{
					_context.call("removeOverlay");
					if (callback != null) 
						callback.apply(null, [status].concat(mediaArgs));
				}
			};
		}
		
		private function onContext3DCreated( event : Event ) : void
		{
			_context3DLost = !_context3DLost;
		}
		
		private function onStatus( event : StatusEvent ) : void
		{
			var callback:Function = _callback;
			
			if (event.code == "PICKING_ERROR")
			{
				if (_callback != null)
				{
					log("Error while picking = "+event.level);
					callback(STATUS_ERROR, event.level);
				}
			}
			else if (event.code == "ERROR_GENERATING_VIDEO")
			{
				if (_callback != null)
				{
					log("Error while generating video = "+event.level);
					callback(STATUS_ERROR, event.level);
				}
			}
			else if (event.code == "DID_FINISH_PICKING")
			{
				if (callback != null)
				{
					_callback = null;
					
					var mediaType:String = event.level;
					if (mediaType == "IMAGE")
					{
						// Load BitmapData
						var pickedImageWidth:int = _context.call("getPickedImageWidth") as int;
						var pickedImageHeight:int = _context.call("getPickedImageHeight") as int;
						
						var pickedImagePath:String = _context.call("getImagePath") as String;
						
						var pickedImageBitmapData:BitmapData;
						if(Capabilities.manufacturer.indexOf("Android") == -1) {
							pickedImageBitmapData = new BitmapData(pickedImageWidth, pickedImageHeight);
							_context.call("drawPickedImageToBitmapData", pickedImageBitmapData);
						}
						
//						// Load JPEG-encoded ByteArray
//						var pickedImageByteArray:ByteArray = new ByteArray();
//						pickedImageByteArray.length = _context.call("getPickedImageJPEGRepresentationSize") as int;
//						_context.call("copyPickedImageJPEGRepresentationToByteArray", pickedImageByteArray);
						
						callback(STATUS_OK, pickedImagePath, pickedImageBitmapData);
					}
					else if (mediaType == "VIDEO")
					{
						// Video File path on device
						var videoPath:String = _context.call("getVideoPath") as String;
						
						var thumbnailImagePath:String = _context.call("getImagePath") as String;
						
						// Picked Image Data corresponds to the thumbnail of the video.
						var thumbnailImageWidth:int = _context.call("getPickedImageWidth") as int;
						var thumbnailImageHeight:int = _context.call("getPickedImageHeight") as int;
						
						var thumbnailImageBitmapData:BitmapData;
						if(Capabilities.manufacturer.indexOf("Android") == -1) {
							thumbnailImageBitmapData = new BitmapData(thumbnailImageWidth, thumbnailImageHeight);
							_context.call("drawPickedImageToBitmapData", thumbnailImageBitmapData);
						}

						// Load JPEG-encoded ByteArray of the thumbnail
//						var thumbnailImageByteArray:ByteArray = new ByteArray();
//						thumbnailImageByteArray.length = _context.call("getPickedImageJPEGRepresentationSize") as int;
//						_context.call("copyPickedImageJPEGRepresentationToByteArray", thumbnailImageByteArray);

						callback(STATUS_OK, videoPath, thumbnailImagePath, thumbnailImageBitmapData);
					}
				}
			}
			else if (event.code == STATUS_DID_CANCEL)
			{
				if (callback != null)
				{
					_callback = null;
					callback(STATUS_DID_CANCEL);
				}
			}
			else if (event.code == STATUS_PICASSA_NOT_SUPPORTED)
			{
				if (callback != null)
				{
					_callback = null;
					callback(STATUS_PICASSA_NOT_SUPPORTED);
				}
			}
			else if (event.code == "LOGGING") // Simple log message
			{
				log(event.level);
			}
		}
		
		private function log( message : String ) : void
		{
			if (_logEnabled) trace("["+LOG_TAG+"] " + message);
		}
	}
}