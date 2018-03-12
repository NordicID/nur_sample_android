# Nur Android Samples
This repository contains Android application project where demonstrating basic features of Nordic ID EXA readers.

- Device connection
- Device properties
- Barcode read
- RFID Inventory 

You can download installation packet (APK) of this sample application into your phone from NurSampleAndroid/app/release/.
These simple samples will help you to develop your own Android app to communicate with Nordic ID devices and build sophisticate data collection applications.

## Creating the project from scratch
Step by step instruction to build "Hello world" type of Android studio project with libraries needed.
* **NurApi.jar** Java library for RFID and transport operations.
* **NurApiAndroid.aar** for Android and device specific operations like Barcode reading, BLE connectivity, battery status, I/O events etc.
* **TDTLib.jar** library for dealing with GS1 coded tags
### Create new project
1. Open Android Studio and create a new project with "Empty activity". SDK version must be 21 or higher.
2. Add **NurApi.jar**, **NurApiAndroid.aar** and **TDTLib.jar** to your project as modules. (New.. Module --> Import .JAR/.AAR Package --> Browse to NurSampleAndroid/NurApi ..NurApiAndroid ..TDTLib)
3. Select the project view
4. Right-click the project and select "Open Module Settings"
5. From left pane, select your app from "Modules"
6. Select "Dependencies" tab
7. Add dependencies by clicking + 
8. Choose "3. Module dependency" and select all the modules from list.
### Modify activity
1. Open text view of activity_main.xml
2. Add row: "android:id="@+id/id_hello_nur" after <TextView row
3. Modify MainActivity.java  as below and RUN
4. You should see NurApi version number middle of the screen.
````
import android.widget.TextView;
import com.nordicid.nurapi.*;	   

public class MainActivity extends AppCompatActivity { 

	private TextView helloNurText;       
	   
       	@Override
       	protected void onCreate(Bundle savedInstanceState) {  
       		super.onCreate(savedInstanceState);
        	setContentView(R.layout.activity_main); 
	
        	NurApi mApi = new NurApi();  
  
        	helloNurText = (TextView)findViewById(R.id.id\_hello\_nur);  
        	helloNurText.setText("Hello NurApi\\nVersion:" \+ mApi.getFileVersion());  
	} 
} 
````


# Using the API in your application 

The API has actually two parts - The NurApi that is same for all Java applications and on top of that is the NurApiAndroid that has some extensions such as battery information and barcode scanning.

In your app declare the API instance and the accessory extension. The latter takes the API as its parameter:

    private NurApi mApi = new NurApi();
    private NurAccessoryExtension mExtension = new NurAccessoryExtension(mApi);

## Setting up the transport layer

A NUR reader based application has these layers: application - NUR API - transport. The transport layer merely handles the low level communication with the NUR module.

There are various transport types available. The ones of interest in Android are BLE (Bluetooth Low Energy), USB (with e.g STIX reader) and TCP/IP (with Sampo and ARxx readers).

In the NurApiAndroid there is an interface that represents an automatic connection (NurApiAutoConnectTransport). An example of an automatic connection implemntation is e.g "NurApiBLEAutoConnect". The automatic connection is started by giving an address to the connection class. This method's name is "setAddress" and the address parameter is a string which' contents depend on the type of connection used. In the "RFIDDemo" there is an example how the NurApiAndroid's built-in device search is used and how the address received from the activity is given to the transport layer.
