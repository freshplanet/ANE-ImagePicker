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
	
	public class AirImagePicker extends EventDispatcher
	{
		// --------------------------------------------------------------------------------------//
		//																						 //
		// 									   PUBLIC API										 //
		// 																						 //
		// --------------------------------------------------------------------------------------//
		
		/** AirImagePicker is supported on iOS and Android devices. */
		public static function get isSupported() : Boolean
		{
			return Capabilities.manufacturer.indexOf("iOS") > -1 || Capabilities.manufacturer.indexOf("Android") > -1;
		}
		
		public function AirImagePicker()
		{
			if (!_instance)
			{
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
		 * If the user cancels, <code>null</code> is returned to the callback.
		 * 
		 * @param callback A callback function of the following form:
		 * <code>function myCallback(image:BitmapData, data:ByteArray)</code>. The <code>
		 * data</code> parameter will contain a JPEG-encoded version of the image.
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
		public function displayImagePicker( callback : Function, crop : Boolean = false, anchor : Rectangle = null ) : void
		{
			if (!isImagePickerAvailable()) callback(null, null);
			
			_callback = callback;
			
			if (anchor != null) _context.call("displayImagePicker", crop, anchor);
			else _context.call("displayImagePicker", crop);
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
		 * <code>function myCallback(image:BitmapData, data:ByteArray)</code>
		 * @param crop If <code>true</code>, the image will be cropped with a 1:1 aspect
		 * ratio. A native UI will be displayed to allow the user to do the cropping
		 * properly. Default: <code>false</code>.
		 * 
		 * @see #isCameraAvailable()
		 */
		public function displayCamera( callback : Function, crop : Boolean = false ) : void
		{
			if (!isCameraAvailable()) callback(null, null);
			
			prepareToDisplayNativeUI(callback);
			_context.call("displayCamera", crop);
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
			return function(image:BitmapData, data:ByteArray):void
			{
				if (_context3DLost)
				{
					var onContextRestored:Function = function(event:Event):void
					{
						_stage3D.removeEventListener(Event.CONTEXT3D_CREATE, onContextRestored);
						_context.call("removeOverlay");
						if (callback != null) callback(image, data);
					};
					_stage3D.addEventListener(Event.CONTEXT3D_CREATE, onContextRestored);
				}
				else
				{
					_context.call("removeOverlay");
					if (callback != null) callback(image, data);
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
			
			if (event.code == "DID_FINISH_PICKING")
			{
				if (callback != null)
				{
					_callback = null;
					
					// Load BitmapData
					var pickedImageWidth:int = _context.call("getPickedImageWidth") as int;
					var pickedImageHeight:int = _context.call("getPickedImageHeight") as int;
					var pickedImageBitmapData:BitmapData = new BitmapData(pickedImageWidth, pickedImageHeight);
					_context.call("drawPickedImageToBitmapData", pickedImageBitmapData);
					
					// Load JPEG-encoded ByteArray
					var pickedImageByteArray:ByteArray = new ByteArray();
					pickedImageByteArray.length = _context.call("getPickedImageJPEGRepresentationSize") as int;
					_context.call("copyPickedImageJPEGRepresentationToByteArray", pickedImageByteArray);
					
					callback(pickedImageBitmapData, pickedImageByteArray);
				}
			}
			else if (event.code == "DID_CANCEL")
			{
				if (callback != null)
				{
					_callback = null;
					callback(null, null);
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