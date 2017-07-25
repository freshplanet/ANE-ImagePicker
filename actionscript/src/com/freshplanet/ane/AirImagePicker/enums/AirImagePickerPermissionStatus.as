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
package com.freshplanet.ane.AirImagePicker.enums {
public class AirImagePickerPermissionStatus {
	/***************************
	 *
	 * PUBLIC
	 *
	 ***************************/


	static public const NOT_DETERMINED                  : AirImagePickerPermissionStatus = new AirImagePickerPermissionStatus(Private, "not_determined");
	static public const AUTHORIZED                      : AirImagePickerPermissionStatus = new AirImagePickerPermissionStatus(Private, "authorized");
	static public const RESTRICTED                      : AirImagePickerPermissionStatus = new AirImagePickerPermissionStatus(Private, "restricted");
	static public const DENIED                          : AirImagePickerPermissionStatus = new AirImagePickerPermissionStatus(Private, "denied");

	public static function fromValue(value:String):AirImagePickerPermissionStatus {

		switch (value)
		{
			case NOT_DETERMINED.value:
				return NOT_DETERMINED;
				break;
			case AUTHORIZED.value:
				return AUTHORIZED;
				break;
			case RESTRICTED.value:
				return RESTRICTED;
				break;
			case DENIED.value:
				return DENIED;
				break;
			default:
				return null;
				break;
		}
	}

	public function get value():String {
		return _value;
	}

	/***************************
	 *
	 * PRIVATE
	 *
	 ***************************/

	private var _value:String;

	public function AirImagePickerPermissionStatus(access:Class, value:String) {

		if (access != Private)
			throw new Error("Private constructor call!");

		_value = value;
	}
}
}
final class Private {}