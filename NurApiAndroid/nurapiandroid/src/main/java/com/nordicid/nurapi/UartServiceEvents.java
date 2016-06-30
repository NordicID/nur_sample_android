package com.nordicid.nurapi;

/**
 * Interface representing the UART service's events.
 */
public interface UartServiceEvents {
	public void onConnStateChanged();
	public void onDataAvailable(byte []data);	
}
