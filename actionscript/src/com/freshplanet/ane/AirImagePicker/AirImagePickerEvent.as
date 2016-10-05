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
    import flash.events.Event;

    public class AirImagePickerEvent extends Event
    {
        //for getting recent images directly from Photos
        public static const ANE_ERROR:String = "ANE_ERROR";
        public static const IMAGE_LIST_SUCCEEDED:String = "IMAGE_LIST_SUCCEEDED";
        public static const IMAGE_LIST_ERROR:String = "IMAGE_LIST_ERROR";
        public static const IMAGE_LOAD_ERROR:String = "IMAGE_LOAD_ERROR";
        public static const IMAGE_LOAD_TEMP:String = "IMAGE_LOAD_TEMP"; // a temp. crappy placeholder until the fetch completes
        public static const IMAGE_LOAD_CANCELLED:String = "IMAGE_LOAD_CANCELLED";
        public static const IMAGE_LOAD_SUCCEEDED:String = "IMAGE_LOAD_SUCCEEDED";

        private var _data:Object;
        private var _requestId:int = -1;
        private var _error:String;

        public function AirImagePickerEvent(type:String, dat:*)
        {
            super(type);
            _data = dat;
            _requestId = _data.requestId;
            _error = _data.error;
        }


        public function get data():Object {
            return _data;
        }

        public function get requestId():int {
            return _requestId;
        }

        public function get error():String {
            return _error;
        }
    }
}
