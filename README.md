Air Native Extension for mobile camera and gallery features (iOS + Android)
======================================

This is an [Air native extension](http://www.adobe.com/devnet/air/native-extensions-for-air.html) that allows you to display native UI to pick media (videos and images) from the gallery or take a picture/video with the camera on iOS and Android. It has been developed by [FreshPlanet](http://freshplanet.com).


Usage
-----


   Complete usage example is in the *sample* directory
   
   The ANE in the *sample/libs* directory works standalone.
   
   The ANE in the *bin* directory is compiled without Android support libraries and will not work standalone.
   
    
In the manifestAdditions section of your AIR app manifest for Android, include this in the application element:
  ```xml
  <activity android:name="com.freshplanet.ane.AirImagePicker.activities.GalleryActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>
  <activity android:name="com.freshplanet.ane.AirImagePicker.activities.CropActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>
  <activity android:name="com.freshplanet.ane.AirImagePicker.activities.CameraActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>
  ```

Advanced features available: square cropping, custom positioning for the gallery image picker on iPad.

For more information, please look at the Actionscript documentation in [AirImagePicker.as](https://github.com/freshplanet/ANE-ImagePicker/blob/master/actionscript/src/com/freshplanet/ane/AirImagePicker/AirImagePicker.as).


Installation
---------

The ANE binary (AirImagePicker.ane) is located in the *bin* folder. You should add it to your application project's Build Path and make sure to package it with your app (more information [here](http://help.adobe.com/en_US/air/build/WS597e5dadb9cc1e0253f7d2fc1311b491071-8000.html)).


Build script
---------

Should you need to edit the extension source code and/or recompile it, you will find an ant build script (build.xml) in the *build* folder:

    cd /path/to/the/ane/build
    mv example.build.config build.config
    #edit the build.config file to provide your machine-specific paths
    ant


Authors
------

This ANE has been written by [Alexis Taugeron](http://alexistaugeron.com), [Daniel Rodriguez](http://www.github.com/dornad/) and [Mateo Kozomara](mateo.kozomara@gmail.com). It belongs to [FreshPlanet Inc.](http://freshplanet.com) and is distributed under the [Apache Licence, version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
