## Add NurApiListener
In order to receive events from NurApi, NurApiListener must be defined.
All event handlers must be defined for all possible events NurApi may fired.

Set Event handlers:
````
mNurApi.setListener(mNurApiListener);
````

Add all event handlers in to you MainActivity.java

````
private NurApiListener mNurApiListener = new NurApiListener()  
{  
    @Override  
  public void triggeredReadEvent(NurEventTriggeredRead event) { }  
    @Override  
  public void traceTagEvent(NurEventTraceTag event) { }  
    @Override  
  public void programmingProgressEvent(NurEventProgrammingProgress event) { }  
    @Override  
  public void nxpEasAlarmEvent(NurEventNxpAlarm event) { }  
    @Override  
  public void logEvent(int level, String txt) { }  
    @Override  
  public void inventoryStreamEvent(NurEventInventory event) { }  
    @Override  
  public void inventoryExtendedStreamEvent(NurEventInventory event) {}  
    @Override  
  public void frequencyHopEvent(NurEventFrequencyHop event) { }  
    @Override  
  public void epcEnumEvent(NurEventEpcEnum event) { }  
    @Override  
  public void disconnectedEvent() { }  
    @Override  
  public void deviceSearchEvent(NurEventDeviceInfo event) { }  
    @Override  
  public void debugMessageEvent(String event) { }  
    @Override  
  public void connectedEvent() { }  
    @Override  
  public void clientDisconnectedEvent(NurEventClientInfo event) { }  
    @Override  
  public void clientConnectedEvent(NurEventClientInfo event) { }  
    @Override  
  public void bootEvent(String event) {}  
    @Override  
  public void IOChangeEvent(NurEventIOChange event) { }  
    @Override  
  public void autotuneEvent(NurEventAutotune event) { }  
    @Override  
  public void tagTrackingScanEvent(NurEventTagTrackingData event) { }  
    //@Override  
  public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }  
};
````
All NurApi events are called from NurApi thread, thus direct UI updates are not allowed.
If you need to access UI controls, you can use runOnUiThread(Runnable) or Handler.

Example
````
//Connection status string (global)
String mConnStatus;
````
````
private void showOnUI() {  
    runOnUiThread(new Runnable() {  
        @Override  
  public void run() {  
            helloNurText.setText(mConnStatus);  
        }  
    });  
}
````
..when connection event occurs..
````
...
@Override
 public void disconnectedEvent() {
	mConnStatus="Disconnected";
	showOnUI();
}
...
@Override
 public void connectedEvent() {
	mConnStatus="Connected";
	showOnUI();
}
````
Next: [Device connection](3_Device_Connection.md) Connect reader into your application
