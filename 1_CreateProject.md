# Creating new Android Studio project  
Step by step instruction to build "Hello world" type of Android studio project with libraries needed.  
* **NurApi.jar** Java library for RFID and transport operations.  
* **NurApiAndroid.aar** for Android and device specific operations like Barcode reading, BLE connectivity, battery status, I/O events etc.  
* **NurApiSmartPair.aar** (*optional*) for easy connection into nearest reader.  
* **TDTLib.jar** (*optional*) library for dealing with GS1 coded tags  
### Create new project  
1. Open Android Studio and create a new project with "Empty activity". SDK version must be 21 or higher.  
2. Copy **NurApi.jar** and **TDTLib.jar** in to `app/libs` folder)  
   Alternatively,   copy NurApi and TDTLib folders from this sample into your project root and set following rows in to the dependecies of `app/build.gradle` file
   `implementation files('../NurApi/NurApi.jar')`    
   `implementation files('../TDTLib/TDTLib.jar')` 

3. Add **NurApiAndroid.aar** and other *aar files in to your project as module. (New.. Module --> Import .JAR/.AAR Package --> Browse to location of ***.aar** file  
4. Select the project view  
5. Right-click the project and select "Open Module Settings"  
6. From left pane, select your app from "Modules"  
7. Select "Dependencies" tab  
8. Add dependencies by clicking + from the left side of pane.  
9. Choose "3. Module dependency" and select ***.aar** file from list. -->OK to close project window  
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
 protected void onCreate(Bundle savedInstanceState) {              super.onCreate(savedInstanceState);  
 setContentView(R.layout.activity_main);     
           NurApi mApi = new NurApi();    
    
           helloNurText = (TextView)findViewById(R.id.id\_hello\_nur);    
           helloNurText.setText("Hello NurApi\\nVersion:" \+ mApi.getFileVersion());    
} } ````  
  
# Using the API in your application   
The API has actually two parts - The NurApi that is same for all Java applications and on top of that is the NurApiAndroid that has some extensions such as battery information and barcode scanning.  
  
In your app declare the API instance and the accessory extension. The latter takes the API as its parameter:  
````  
private NurApi mApi = new NurApi();  
private NurAccessoryExtension mExtension = new NurAccessoryExtension(mApi);  

Next: [Event listener](2_EventListener.md) Add event listener for receiving events from NurApi.
