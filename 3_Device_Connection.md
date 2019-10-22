# Device Connection
Available connection methods
* [Bluetooth LE](#bluetooth-le) (for EXA devices)
* [TCP/IP socket (Ethernet, WLAN)](#tcpip) (usually for fixed readers)
* [USB](#usb) (readers provided with USB port)
* [Search & select reader from list](#select-reader-from-search-dialog) NurApiAndroid's built-in device search activity.
## Bluetooth LE
Following definitions are required in order to connect reader using Bluetooth LE.
### AndroidManifest.xml
##### User permissions
````
<manifest xmlns:android="http://schemas.android.com/apk/res/android"  
  ...  
    <uses-permission android:name="android.permission.BLUETOOTH" />  
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />  
    <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" />
  ...
````
##### UartService
````
<application  
 ...  
    <service  
	  android:name="com.nordicid.nurapi.UartService"  
	  android:enabled="true"  
	  android:exported="true" />  
 ...
</application>
````

### build.gradle (Module: app)
````
dependencies {  
  ...
  implementation 'no.nordicsemi.android.support.v18:scanner:1.4.0'  
  ...
}
````

## Connection example
Creating simple connection to the EXA using NurApiBLEAutoConnect and known BT MAC address (printed on label).
### MainActivity.java
````
public class MainActivity extends AppCompatActivity {  
  ...
	private NurApiBLEAutoConnect mBLEAuto= null;
  ...
  ````
  ````
  @Override  
protected void onCreate(Bundle savedInstanceState) {     
  ...
	mNurApi = new NurApi();    
	// In this activity, we use mNurApiListener for receiving events  
	mNurApi.setListener(mNurApiListener);

    //Bluetooth LE scanner need to find EXA's near  
	BleScanner.init(this);  
  
    String strAddress = "DF:86:FA:B8:01:08";  //Known address of EXA
    mBLEAuto= new NurApiBLEAutoConnect(MainActivity.this,mNurApi);  
    mBLEAuto.setAddress(strAddress); //Auto connection start here
    ...
   ````
   Receiving connection events
   ````
   private NurApiListener mNurApiListener = new NurApiListener()  
{  
...
    @Override  
  public void disconnectedEvent() {  
		//EXA disconnected
        mConnStatus="Disconnected";  
        showOnUI();  
    }  
    ...
    @Override  
  public void connectedEvent() {  		        
	    try {  //EXA connected. Show serial.
	        NurRespReaderInfo info = mNurApi.getReaderInfo();  
	        mConnStatus=info.altSerial;  
    }  
    catch (Exception ex) {  //Show error if any..
        mConnStatus = ex.getMessage();  
    }    
    showOnUI();   
    ...  
````
**Note**: NurApi commands usually required to be inside "try catch" scheme where application must handle possible exception properly.

### Connection handling
You can stop, pause, resume.. connection based on your navigation needs between activities.
````
@Override  
protected void onPause() {      
    super.onPause();  
    mBLEAuto.onPause();  
}  
  
@Override  
protected void onResume() {       
    super.onResume();  
    mBLEAuto.onResume();  
}  
  
@Override  
protected void onStop() {     
    super.onStop();  
    mBLEAuto.onStop();  
}  
  
@Override  
protected void onDestroy() {      
    super.onDestroy();  
    mBLEAuto.onDestroy();  
}
````

## TCP/IP
Connecting to reader supporting TCP/IP connectivity. (Usually fixed readers)
NurApiSocketAutoConnect works same way as NurApiBLEAutoConnect.
````
....
private NurApiSocketAutoConnect mSockAuto = null;
....
````

````
String addr  = "192.168.1.203:4333";  
mSockAuto = new NurApiSocketAutoConnect(this,mNurApi);  
mSockAuto.setAddress(addr);
````
After calling setAddress, connection will be established and maintained automatically.
Use ```` onStop, onPause, onResume, onDestroy ```` for controlling connection.

## USB
Use ```` NurApiUsbAutoConnect ```` for connecting to readers provided with USB port.
Usage is similar than ````NurApiSocketAutoConnect```` and ````NurApiBLEAutoConnect````

## Select reader from search dialog
Easy way to connect is to select correct device from list. EXA devices advertising itself via Bluetooth LE. TCP/IP devices using Multicast DNS for detecting devices from local network.
````NurDeviceListActivity```` can be used for show device list.
#### Required definitions for AndroidManifest.xml
Same definitions needed as for [Bluetooth LE](#bluetooth-le) connection including following:
````
<application  
...  
    <activity android:name="com.nordicid.nurapi.NurDeviceListActivity" android:label="@string/app_name" android:theme="@android:style/Theme.Dialog"/>  
 ...      
</application>
````

#### MainActivity
````
private NurApiAutoConnectTransport mAutoTransport;
...
//Open device select activity  
NurDeviceListActivity.startDeviceRequest(MainActivity.this, mNurApi);
````

Handle result of device list activity
````
/**  
 \* DeviceList activity result \* @param requestCode We are intrest code "NurDeviceListActivity.REQUEST\_SELECT\_DEVICE" (32778)  
 \* @param resultCode If RESULT_OK user has selected device and then we create NurDeviceSpec (spec) and transport
 \* @param data  
  */  
@Override  
public void onActivityResult(int requestCode, int resultCode, Intent data)  {  
    switch (requestCode)  
    {  
        case NurDeviceListActivity.REQUEST_SELECT_DEVICE: {  
            if (data == null || resultCode != NurDeviceListActivity.RESULT_OK)  
                return;  
            try {  
                NurDeviceSpec spec = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));  
  
                if (mAutoTransport != null) {  
                    //Dispose existing transport  
					mAutoTransport.dispose();  
                }  
  
                String strAddress;  
                mAutoTransport = NurDeviceSpec.createAutoConnectTransport(this, mNurApi, spec);  
                strAddress = spec.getAddress();  
                mAutoTransport.setAddress(strAddress);  
  
            } catch (Exception e) {  
                mConnStatus = e.getMessage();  
                showOnUI();  
            }  
        }  
        break;  
    }  
    super.onActivityResult(requestCode,resultCode,data);  
}
````
After calling spec.setAddress, connection will be established and maintained automatically.
Use ```` mAutoTransport: onStop, onPause, onResume, onDestroy ```` for controlling connection.

back to [README](README.md)
