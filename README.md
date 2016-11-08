# NUR Android examples

## About project opening
When downloading the Android samples note that the project(s) need to be opened separately from the Android Studio; Android Studio's direct Git open won't work.
Note that the used Android Studio's version in this package is 2.2.2.

## Re-building NurApiAndroid: 

1. Select Build -> Rebuild Project
2. Select Build -> Generate APK (generates actually NurApiAndroid.aar)
2. Run CopyNurApiAndroid.bat to copy the produced AAR to each project in this repo

## Creating a project

Create a project that fits your needs. To add the NurApiAndroid to the project in Android Studio:

1. Select the project view
2. Right-click the project and select New -> Module
3. In the new module dialog, select the "Import .JAR/.AAR Package"
4. Navigate to the "nurapiandroid-release.aar"
5. Set the sub-project name to e.g. "nurapiandroid"
6. Right-click the created module and select "Open Module Settings"
7. Add the module dependency to "app"
8. Change the project view to "Android" and open the app-module's Gradle script
9. Change the minimum SDK version (minSdkVersion) to 21 if it isn't already
10. Target SDK version (targetSdkVersion) is good to be 21 - higher, e.g. 24 will cause trouble when using Bluetooth. This must be worked around in the application code thus the version 21 recommendation.
11. Where you want to use the Android API, use

import com.nordicid.nurapi.*;

## Using the API in your application 

The API has actually two parts - The NurApi that is same for all Java applications and on top of that is the NurApiAndroid that has some extensions such as battery information and barcode scanning.

In your app declare the API instance and the accessory extension. The latter takes the API as its parameter:

    private NurApi mApi = new NurApi();
    private NurAccessoryExtension mExtension = new NurAccessoryExtension(mApi);

## Setting up the transport layer

A NUR reader based application has these layers: application - NUR API - transport. The transport layer merely handles the low level communication with the NUR module.

There are various transport types available. The ones of interest in Android are BLE (Bluetooth Low Energy), USB (with e.g STIX reader) and TCP/IP (with Sampo and ARxx readers).

In the NurApiAndroid there is an interface that represents an automatic connection (NurApiAutoConnectTransport). An example of an automatic connection implemntation is e.g "NurApiBLEAutoConnect". The automatic connection is started by giving an address to the connection class. This method's name is "setAddress" and the address parameter is a string which' contents depend on the type of connection used. In the "RFIDDemo" there is an example how the NurApiAndroid's built-in device search is used and how the address received from the activity is given to the transport layer.

