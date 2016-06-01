//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2012 Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//
//  Copyright 2016 VoiceThread (https://voicethread.com/)
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
	import flash.filesystem.File;

	/**
	*  Take care of picking images and videos on iOS/Android
	*
	*  CALLBACKS:   
	*
	*   The callbacks to every method in this native extension have the following form: 
	*   <code>function(status:String, media:File = null):void</code>
	*
	*	  For each image or video that was picked, the callback will be called with 
	*   <code>status</code> set to STATUS_MEDIA and <code>media</code> set to 
	*   a temporary file containing the selected media. You should delete this 
	*   file once you finish with it.
	*
	*   When all picked media has been returned, the callback will be called with 
	*   STATUS_DID_FINISH and a null media file to indicate that no more media is 
	*   available.
	*
	*   If something goes wrong while picking media, the callback will be called 
	*   with STATUS_ERROR, or STATUS_NOT_SUPPORTED if the requested feature isn't 
	*   available on the current device.
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

		public static const STATUS_MEDIA:String = "MEDIA";
		public static const STATUS_DID_FINISH:String = "DID_FINISH";
		public static const STATUS_ERROR:String = "ERROR";
		public static const STATUS_NOT_SUPPORTED:String = "NOT_SUPPORTED";

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
		 * If <code>true</code>, the user will be allowed to select videos as well 
		 *  as images.
		 * If <code>false</code>, only images will be selected.
		 */
		public function get allowVideo():Boolean { return(_allowVideo); }
		public function set allowVideo(value:Boolean ):void { _allowVideo = value; }
		
		/**
		 * If <code>true</code>, the user will be allowed to select documents as well 
		 *  as images and videos.
		 * If <code>false</code>, only images will be selected.
		 */
		public function get allowDocument():Boolean { return(_allowDocument); }
		public function set allowDocument(value:Boolean ):void { _allowDocument = value; }
		
		/**
		 * If <code>true</code>, the user will be allowed to select multiple images
		 *  or videos when displayImagePicker is called.
		 * If <code>false</code>, the user will only be able to select one image 
		 *  or video.
		 */
		public function get allowMultiple():Boolean { return(_allowMultiple); }
		public function set allowMultiple(value:Boolean ):void { _allowMultiple = value; }
		
		/**
		 * If <code>true</code>, an interface will be shown to crop selected images 
		 *  to a 1:1 aspect ratio.
		 * If <code>false</code>, selected media will be returned unmodified.
		 */
		public function get showCrop():Boolean { return(_showCrop); }
		public function set showCrop(value:Boolean ):void { _showCrop = value; }
		
		/**
		 * Display the gallery image picker if it is available on the current device.
		 * Otherwise, do nothing.<br><br>
		 * 
		 * Once the user picks an image, it is returned to the provided callback function,
		 * both as a <code>BitmapData</code> and a JPEG-encoded <code>BypeArray</code>.
		 * If the user cancels, <code>null</code> is returned to the callback.<br><br>
		 *
		 * @param callback A callback function of the following form:
		 * <code>function(status:String, media:File = null):void</code>. See the ASDoc 
		 * for this class for a in-depth explanation of the arguments passed to the callback.
		 * @param anchor On the iPad, the image picker is displayed in a popover that
		 * doesn't cover the whole screen. This parameter is the anchor from which the
		 * popover will be presented. For example, it could be the bounds of the button
		 * on which the user clicked to display the image picker. Note that you should
		 * use absolute stage coordinates. Example: <code>var anchor:Rectangle = 
		 * myButton.getBounds(stage);</code>
		 * 
		 * @see #isImagePickerAvailable()
		 */
		public function displayImagePicker(callback:Function, anchor:Rectangle=null):void
		{
			if (!isImagePickerAvailable()) callback(STATUS_NOT_SUPPORTED, null);
			
			_callback = callback;
			_pickedMediaCount = 0;
			
			if (anchor != null) {
			  _context.call("displayImagePicker", _allowVideo, _allowDocument, _allowMultiple, _showCrop, anchor);
			}
			else {
			  _context.call("displayImagePicker", _allowVideo, _allowDocument, _allowMultiple, _showCrop);
			}
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
		 * <code>function(status:String, media:File = null):void</code>. 
		 * See the ASDoc for this class for a in-depth explanation of the arguments 
		 * passed to the callback.
		 * 
		 * @see #isCameraAvailable()
		 */
		public function displayCamera(callback:Function):void
		{
			if (! isCameraAvailable()) callback(STATUS_NOT_SUPPORTED, null);
			
			_pickedMediaCount = 0;
			
			prepareToDisplayNativeUI(callback);
			
			_context.call("displayCamera", _allowVideo, _showCrop);
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
		private var _pickedMediaCount:int = 0;
		
		// picker configuration
		private var _allowVideo:Boolean = false;
		private var _allowDocument:Boolean = false;
		private var _allowMultiple:Boolean = false;
		private var _showCrop:Boolean = false;
		
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
			if (event.code == "ERROR")
			{
				if (_callback != null)
				{
					log("Error while picking media = "+event.level);
					callback(STATUS_ERROR, event.level);
				}
			}
			else if (event.code == "DID_PICK_MEDIA")
			{
			  _pickedMediaCount++;
				if (callback != null) callback(STATUS_MEDIA, new File(event.level));
			}
			else if (event.code == "DID_FINISH")
			{
				if (callback != null) {
				  _callback = null;
				  callback(STATUS_DID_FINISH);
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
