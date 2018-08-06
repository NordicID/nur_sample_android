# Creating new Android Studio project
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
4. Build and run project. You should see NurApi version number middle of the screen.
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
````
private NurApi mApi = new NurApi();
private NurAccessoryExtension mExtension = new NurAccessoryExtension(mApi);
````
Next: [Event listener](2_EventListener.md) Add event listener for receiving events from NurApi.
