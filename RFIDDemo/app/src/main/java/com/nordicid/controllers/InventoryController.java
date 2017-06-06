package com.nordicid.controllers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiException;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;
import com.nordicid.rfiddemo.Beeper;
import com.nordicid.rfiddemo.R;
import com.nordicid.rfiddemo.TraceApp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class InventoryController {

	public class Stats
	{
		final double TAGS_PER_SEC_OVERTIME = 2;

		private AvgBuffer mTagsPerSecBuffer = new AvgBuffer(1000, (int)(TAGS_PER_SEC_OVERTIME * 1000));

		private long mTagsReadTotal = 0;
		private double mTagsPerSec = 0;
		private double mAvgTagsPerSec = 0;
		private double mMaxTagsPerSec = 0;
		private int mInventoryRounds = 0;
		private long mInventoryStartTime = 0;
		private double mTagsFoundInTime = 0;

		public void updateStats(NurEventInventory ev)
		{
			mTagsPerSecBuffer.add(ev.tagsAdded);

			mTagsReadTotal += ev.tagsAdded;

			mTagsPerSec = mTagsPerSecBuffer.getSumValue() / TAGS_PER_SEC_OVERTIME;
			if (getElapsedSecs() > 1)
				mAvgTagsPerSec = mTagsReadTotal / getElapsedSecs();
			else
				mAvgTagsPerSec = mTagsPerSec;

			if (mTagsPerSec > mMaxTagsPerSec)
				mMaxTagsPerSec = mTagsPerSec;

			mInventoryRounds += ev.roundsDone;
		}

		public void start()
		{
			mInventoryStartTime = System.currentTimeMillis();
		}

		public void clear()
		{
			mTagsPerSecBuffer.clear();
			mTagsReadTotal = 0;
			mTagsPerSec = 0;
			mAvgTagsPerSec = 0;
			mMaxTagsPerSec = 0;
			mInventoryRounds = 0;
			mInventoryStartTime = 0;
			mTagsFoundInTime = 0;
		}

		public double getElapsedSecs()
		{
			if (mInventoryStartTime == 0) return 0;
			return (double)(System.currentTimeMillis() - mInventoryStartTime) / 1000.0;
		}

		public long getTagsReadTotal() {
			return mTagsReadTotal;
		}

		public double getTagsPerSec() {
			return mTagsPerSec;
		}

		public double getAvgTagsPerSec() {
			return mAvgTagsPerSec;
		}

		public double getMaxTagsPerSec() {
			return mMaxTagsPerSec;
		}

		public int getInventoryRounds() {
			return mInventoryRounds;
		}

		public double getTagsFoundInTimeSecs() {
			return mTagsFoundInTime;
		}

		public void setTagsFoundInTimeSecs() {
			mTagsFoundInTime = getElapsedSecs();
		}
	}

	public interface InventoryControllerListener {
		public void tagFound(NurTag tag, boolean isNew);
		public void inventoryRoundDone(NurTagStorage storage, int newTagsOffset, int newTagsAdded);
		public void readerDisconnected();
		public void readerConnected();
		public void inventoryStateChanged();
		public void IOChangeEvent(NurEventIOChange event);
	}

	private boolean mInventoryRunning = false;
	private int mAddedUnique = 0;
	private NurApi mApi = null;
	private InventoryControllerListener mInventoryListener = null;
	private NurApiListener mThisClassListener = null;
	private Stats mStats = new Stats();
	private Thread mBeeperThread = null;
	private NurTagStorage mTagStorage = new NurTagStorage();

	private ArrayList<HashMap<String, String>> mListViewAdapterData = new ArrayList<HashMap<String,String>>();

	public NurApiListener getNurApiListener()
	{		
		return mThisClassListener;
	}

	public ArrayList<HashMap<String, String>> getListViewAdapterData()
	{
		return mListViewAdapterData;
	}

	public NurTagStorage getTagStorage()
	{
		return mTagStorage;
	}

	public InventoryController(NurApi na)
	{
		mApi = na;

		mThisClassListener = new NurApiListener()
		{
			@Override 
			public void inventoryStreamEvent(NurEventInventory event) {
				// Make sure listener is set
				if (mInventoryListener == null)
					return;

				// Update stats
				mStats.updateStats(event);

				// Handle inventoried tags
				handleInventoryResult();

				// Restart reading if needed
				if (event.stopped && mInventoryRunning) {
					
					try {
						mApi.startInventoryStream();
					}
					catch (Exception err) {
						err.printStackTrace();
					}
				}
			}
			
			@Override
			public void connectedEvent() {
				if (mInventoryListener != null) {
					mInventoryListener.readerConnected();
				}
			}
			
			@Override
			public void disconnectedEvent() {
				if (mInventoryListener != null) {
					mInventoryListener.readerDisconnected();
					stopInventory();
				}
			}

			@Override public void IOChangeEvent(NurEventIOChange event) {
				if (mInventoryListener != null) {
					mInventoryListener.IOChangeEvent(event);
				}
			}
			@Override public void bootEvent(String arg0) {}
			@Override public void clientConnectedEvent(NurEventClientInfo arg0) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo arg0) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo arg0) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop arg0) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory arg0) {}
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) {}
			@Override public void programmingProgressEvent(NurEventProgrammingProgress arg0) {}
			@Override public void traceTagEvent(NurEventTraceTag arg0) { }
			@Override public void triggeredReadEvent(NurEventTriggeredRead arg0) {}
			@Override public void logEvent(int arg0, String arg1) {}
			@Override public void debugMessageEvent(String arg0) {}
			@Override public void epcEnumEvent(NurEventEpcEnum event) {}
			@Override public void autotuneEvent(NurEventAutotune event) { }
			@Override public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }			
		};
	}

	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy kk:mm:ss");

	void handleInventoryResult()
	{
		synchronized (mApi.getStorage())
		{
			HashMap<String, String> tmp;
			NurTagStorage tagStorage = mApi.getStorage();
			int curUniqueCount = mTagStorage.size();

			// Add tags tp internal tag storage
			for (int i = 0; i < tagStorage.size(); i++) {

				NurTag tag = tagStorage.get(i);

				if (mTagStorage.addTag(tag))
				{
					tmp = new HashMap<String, String>();
					// Add new
					tmp.put("epc", tag.getEpcString());
					tmp.put("rssi", Integer.toString(tag.getRssi()));
					tmp.put("timestamp", Integer.toString(tag.getTimestamp()));
					tmp.put("freq", Integer.toString(tag.getFreq())+" kHz Ch: "+Integer.toString(tag.getChannel()));
					tmp.put("found", "1");
					tmp.put("foundpercent", "100");
					tmp.put("firstseentime", dateFormatter.format(new Date()));
					tmp.put("lastseentime", dateFormatter.format(new Date()));
					tag.setUserdata(tmp);
					mListViewAdapterData.add(tmp);

					if (mInventoryListener != null)
						mInventoryListener.tagFound(tag, true);
				}
				else
				{
					tag = mTagStorage.getTag(tag.getEpc());

					// Update
					tmp = (HashMap<String, String>) tag.getUserdata();
					tmp.put("rssi", Integer.toString(tag.getRssi()));
					tmp.put("timestamp", Integer.toString(tag.getTimestamp()));
					tmp.put("freq", Integer.toString(tag.getFreq())+" kHz (Ch: "+Integer.toString(tag.getChannel())+")");
					tmp.put("found", Integer.toString(tag.getUpdateCount()));
					tmp.put("foundpercent", Integer.toString((int) (((double) tag.getUpdateCount()) / (double) mStats.getInventoryRounds() * 100)));
					tmp.put("lastseentime", dateFormatter.format(new Date()));

					if (mInventoryListener != null)
						mInventoryListener.tagFound(tag, false);
				}
			}

			// Clear NurApi tag storage
			tagStorage.clear();

			// Check & report new unique tags
			mAddedUnique = mTagStorage.size() - curUniqueCount;
			if (mAddedUnique > 0)
			{
				mStats.setTagsFoundInTimeSecs();

				// Report round done w/ new unique tags
				if (mInventoryListener != null)
					mInventoryListener.inventoryRoundDone(mTagStorage, curUniqueCount, mAddedUnique);
			}
		}
	}

	public boolean doSingleInventory() throws Exception {
		if (!mApi.isConnected())
			return false;

		// Make sure antenna autoswitch is enabled
		if (mApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
			mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

		// Clear old readings
		clearInventoryReadings();
		// Perform inventory
		try {
			mApi.inventory();
			// Fetch tags from NUR
			mApi.fetchTags();
		}
		catch (NurApiException ex)
		{
			// Did not get any tags
			if (ex.error == NurApiErrors.NO_TAG)
				return true;

			throw ex;
		}
		// Handle inventoried tags
		handleInventoryResult();

		return true;
	}

	public boolean startContinuousInventory() throws Exception {

		if (!mApi.isConnected())
			return false;

		// Enable inventory stream zero reading report
		if ((mApi.getSetupOpFlags() & NurApi.OPFLAGS_INVSTREAM_ZEROS) == 0)
			mApi.setSetupOpFlags(mApi.getSetupOpFlags() | NurApi.OPFLAGS_INVSTREAM_ZEROS);

		// Make sure antenna autoswitch is enabled
		if (mApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
			mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

		// Start reading
		mApi.startInventoryStream();
		mInventoryRunning = true;

		// Clear & start stats
		mStats.clear();
		mStats.start();

		// Start beeper thread
		mBeeperThread = new Thread(mBeeperThreadRunnable);
		mBeeperThread.setPriority(Thread.MIN_PRIORITY);
		mBeeperThread.start();

		// Notify state change
		if (mInventoryListener != null)
			mInventoryListener.inventoryStateChanged();

		return true;
	}
	
	public boolean isInventoryRunning() {
		return mInventoryRunning;
	}
	
	public void stopInventory() {
		try {
			mInventoryRunning = false;

			// Stop reading
			if (mApi.isConnected()) {
				mApi.stopInventoryStream();
				mApi.setSetupOpFlags(mApi.getSetupOpFlags() & ~NurApi.OPFLAGS_INVSTREAM_ZEROS);
			}

			// Stop beeper thread
			if (mBeeperThread != null) {
				mBeeperThread.join(5000);
				mBeeperThread = null;
			}

		} catch (Exception err) {
			err.printStackTrace();
		}

		// Notify state change
		if (mInventoryListener != null)
			mInventoryListener.inventoryStateChanged();
	}

	public void setListener(InventoryControllerListener l) {
		mInventoryListener = l;
	}
	
	public void clearInventoryReadings() {
		mAddedUnique = 0;
		mApi.getStorage().clear();
		mTagStorage.clear();
		mStats.clear();
		mListViewAdapterData.clear();

		if (isInventoryRunning())
			mStats.start();
	}
	
	Runnable mBeeperThreadRunnable = new Runnable() {
		@Override
		public void run()
		{
			while (mInventoryRunning)
			{
				if (mAddedUnique > 0) {
					int sleepTime = 100 - mAddedUnique;
					int beepDuration = Beeper.BEEP_40MS;

					if (sleepTime < 40)
						sleepTime = 40;

					Beeper.beep(beepDuration);

					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else
				{
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};

	public Stats getStats() {
		return mStats;
	}

	public void showTagDialog(Context ctx, final HashMap<String, String> tagData) {
		//shows dialog and the clicked tags information
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View tagDialogLayout = inflater.inflate(R.layout.dialog_tagdata, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

		builder.setView(tagDialogLayout);

		final TextView epcTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_epc);
		epcTextView.setText(ctx.getString(R.string.dialog_epc)+" "+tagData.get("epc"));

		final TextView rssiTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_rssi);
		rssiTextView.setText(ctx.getString(R.string.dialog_rssi)+" "+tagData.get("rssi"));

		final TextView timestampTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_timestamp);
		timestampTextView.setText(ctx.getString(R.string.dialog_timestamp)+" "+tagData.get("timestamp"));

		final TextView fregTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_freq);
		fregTextView.setText(ctx.getString(R.string.dialog_freg)+" "+tagData.get("freq"));

		final TextView foundTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_found);
		foundTextView.setText(ctx.getString(R.string.dialog_found)+" "+tagData.get("found"));

		final TextView foundPercentTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_foundpercent);
		foundPercentTextView.setText(ctx.getString(R.string.dialog_found_precent)+" "+tagData.get("foundpercent"));

		final AlertDialog dialog = builder.create();

		//close button made in "Android L" style. See the layout
		final Button closeDialog = (Button) tagDialogLayout.findViewById(R.id.selected_tag_close_button);
		closeDialog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		final Button locateTag = (Button) tagDialogLayout.findViewById(R.id.selected_tag_locate_button);
		locateTag.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();

				TraceApp.setStartParams(tagData.get("epc"), true);
				AppTemplate.getAppTemplate().setApp("Locate");
			}
		});

		final Context _ctx = ctx;
		final ArrayList<HashMap<String, String>> tags = mListViewAdapterData;
		final Button exportAllTags = (Button) tagDialogLayout.findViewById(R.id.export_csv);
		exportAllTags.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();

				try {

					String postfix = "";
					if (mApi.isConnected())
					{
						postfix = mApi.getReaderInfo().altSerial;
						if (postfix.length() == 0) {
							postfix = mApi.getReaderInfo().serial;
						}
						if (postfix.length() != 0) {
							postfix += "_";
						}
					}

					String filename = "epc_export_";
					filename += postfix;
					filename += new SimpleDateFormat("ddMMyyyykkmmss").format(new Date());
					filename += ".csv";

					File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
					path.mkdirs();

					File outputFile = new File(path, filename);
					outputFile.createNewFile();

					FileWriter fileWriter = new FileWriter(outputFile, true);
					fileWriter.append("firstseen;lastseen;epc;rssi\n");

					for (HashMap<String, String> tag : tags)
					{
						fileWriter.append(tag.get("firstseentime") + ";");
						fileWriter.append(tag.get("lastseentime") + ";");
						fileWriter.append(tag.get("epc") + ";");
						fileWriter.append(tag.get("rssi") + "\n");
					}
					fileWriter.close();

					Log.i("outputFile", "= " + Uri.fromFile(outputFile));

					Intent intent2 = new Intent();
					intent2.setAction(Intent.ACTION_SEND);
					intent2.setType("text/plain");
					intent2.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
					_ctx.startActivity(Intent.createChooser(intent2, _ctx.getString(R.string.share_via)));

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(_ctx, "Error:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});

		dialog.show();
	}
}
