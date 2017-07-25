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
package {


import com.freshplanet.ane.AirImagePicker.AirImagePicker;
import com.freshplanet.ane.AirImagePicker.AirImagePickerImageData;
import com.freshplanet.ane.AirImagePicker.events.AirImagePickerDataEvent;
import com.freshplanet.ane.AirImagePicker.events.AirImagePickerRecentImagesEvent;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.events.Event;

import com.freshplanet.ui.ScrollableContainer;
import com.freshplanet.ui.TestBlock;

[SWF(backgroundColor="#057fbc", frameRate='60')]
public class Main extends Sprite {

    public static var stageWidth:Number = 0;
    public static var indent:Number = 0;

    private var _scrollableContainer:ScrollableContainer = null;

    public function Main() {
        this.addEventListener(Event.ADDED_TO_STAGE, _onAddedToStage);
    }

    private function _onAddedToStage(event:Event):void {
        this.removeEventListener(Event.ADDED_TO_STAGE, _onAddedToStage);
        this.stage.align = StageAlign.TOP_LEFT;

        stageWidth = this.stage.stageWidth;
        indent = stage.stageWidth * 0.025;

        _scrollableContainer = new ScrollableContainer(false, true);
        this.addChild(_scrollableContainer);

        if (!AirImagePicker.isSupported) {
            trace("AirImagePicker ANE is NOT supported on this platform!");
            return;
        }


	    AirImagePicker.instance.addEventListener(AirImagePickerDataEvent.CANCELLED, onImagePickCanceled);
	    AirImagePicker.instance.addEventListener(AirImagePickerDataEvent.IMAGE_CHOSEN, onImageChosen);
	    AirImagePicker.instance.addEventListener(AirImagePickerDataEvent.CAMERA_PERMISSION_ERROR, onCameraPermissionError);
	    AirImagePicker.instance.addEventListener(AirImagePickerDataEvent.GALLERY_PERMISSION_ERROR, onGalleryPermissionError);
	    AirImagePicker.instance.addEventListener(AirImagePickerRecentImagesEvent.ON_LOAD_RESULT, onRecentImagesResult);
	    AirImagePicker.instance.addEventListener(AirImagePickerRecentImagesEvent.PERMISSION_ERROR, onRecentImagesPermissionError);


        var blocks:Array = [];

	    blocks.push(new TestBlock("display camera", function():void {
		    AirImagePicker.instance.displayCamera();
	    }));
        blocks.push(new TestBlock("display image picker", function():void {
	        AirImagePicker.instance.displayImagePicker();
        }));
	    blocks.push(new TestBlock("getCameraPermissionStatus", function():void {
		    trace("Camera permission status: ", AirImagePicker.instance.getCameraPermissionStatus().value);
	    }));
	    blocks.push(new TestBlock("getGalleryPermissionStatus", function():void {
		    trace("Gallery permission status: ", AirImagePicker.instance.getGalleryPermissionStatus().value);
	    }));
	    blocks.push(new TestBlock("loadRecentImages", function():void {
		   AirImagePicker.instance.loadRecentImages(1);

	    }));
	    blocks.push(new TestBlock("openSettings", function():void {
		    AirImagePicker.instance.openSettings()
	    }));


        /**
         * add ui to screen
         */

        var nextY:Number = indent;

        for each (var block:TestBlock in blocks) {

            _scrollableContainer.addChild(block);
            block.y = nextY;
            nextY +=  block.height + indent;
        }
    }

	private function onImagePickCanceled(event:AirImagePickerDataEvent):void {
		trace("Image pick cancelled");
	}

	private function onImageChosen(event:AirImagePickerDataEvent):void {
		var imageData:AirImagePickerImageData = event.imageData;
		trace("Image chosen");
	}

	private function onCameraPermissionError(event:AirImagePickerDataEvent):void {
		trace("Camera permission error");
	}

	private function onGalleryPermissionError(event:AirImagePickerDataEvent):void {
		trace("Gallery permission error");
	}

	private function onRecentImagesResult(event:AirImagePickerRecentImagesEvent):void {
		var images:Vector.<AirImagePickerImageData> = event.imagesData;
		trace("Recent images received");
	}

	private function onRecentImagesPermissionError(event:AirImagePickerRecentImagesEvent):void {
		trace("Recent images permission error");
	}



}
}
