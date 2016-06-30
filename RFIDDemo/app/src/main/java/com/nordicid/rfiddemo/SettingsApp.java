package com.nordicid.rfiddemo;

import java.util.ArrayList;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubApp;
import com.nordicid.nurapi.AutotuneSetup;
import com.nordicid.nurapi.NurApi;
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
import com.nordicid.nurapi.NurSetup;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsApp extends SubApp {
	
	private NurApi mApi;
	
	private Spinner mRegionSpinner;
	private Spinner mTxLevelSpinner;
	private Spinner mLinkFreqSpinner;
	private Spinner mRxDecodSpinner;
	private Spinner mTxModulSpinner;
	private Spinner mQSpinner;
	private Spinner mRoundSpinner;
	private Spinner mSessionSpinner;
	private Spinner mTargetSpinner;
	private MultiSelectionSpinner mAntennaSpinner;
	private CheckBox mAutotuneCheckbox;

	private NurApiListener mThisClassListener = null;

	@Override
	public NurApiListener getNurApiListener()
	{
		return mThisClassListener;
	}
	
	public SettingsApp(Context c, AppTemplate t, NurApi na) {
		super(c, t, na);
		
		mApi = na;
		
		mThisClassListener =  new NurApiListener() {
			@Override
			public void connectedEvent() {
				if (isAdded()) {
					enableItems(true);
					setCurrentSetup();
				}
			}

			@Override
			public void disconnectedEvent() {
				if (isAdded()) {
					enableItems(true);
					setCurrentSetup();
				}
			}

			@Override public void logEvent(int level, String txt) {}
			@Override public void bootEvent(String event) {}
			@Override public void inventoryStreamEvent(NurEventInventory event) { } 
			@Override public void IOChangeEvent(NurEventIOChange event) {}
			@Override public void traceTagEvent(NurEventTraceTag event) { } 
			@Override public void triggeredReadEvent(NurEventTriggeredRead event) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop event) {}
			@Override public void debugMessageEvent(String event) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory event) { } 
			@Override public void programmingProgressEvent(NurEventProgrammingProgress event) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo event) {}
			@Override public void clientConnectedEvent(NurEventClientInfo event) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo event) {}
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm event) {}
			@Override public void epcEnumEvent(NurEventEpcEnum event) {}

			@Override
			public void autotuneEvent(NurEventAutotune event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void tagTrackingScanEvent(NurEventTagTrackingData event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {
				// TODO Auto-generated method stub
				
			}
			
		};
	}
	
	@Override
	public String getAppName() {
		return "Settings";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_settings;
	}

	@Override
	public int getLayout() {
		return R.layout.app_settings;
	}
	
	@Override
	public void onVisibility(boolean val)
	{
		if (val)
		{
			if (isAdded() && mApi.isConnected()) {
				enableItems(true);
				setCurrentSetup();
			}
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mRegionSpinner = (Spinner) view.findViewById(R.id.region_spinner);
		mRegionSpinner.setEnabled(false);
		mTxLevelSpinner = (Spinner) view.findViewById(R.id.txlevel_spinner);
		mTxLevelSpinner.setEnabled(false);
		mLinkFreqSpinner = (Spinner) view.findViewById(R.id.linkfreq_spinner);
		mLinkFreqSpinner.setEnabled(false);
		mRxDecodSpinner = (Spinner) view.findViewById(R.id.rxdecod_spinner);
		mRxDecodSpinner.setEnabled(false);
		mTxModulSpinner = (Spinner) view.findViewById(R.id.txmodul_spinner);
		mTxModulSpinner.setEnabled(false);
		mQSpinner = (Spinner) view.findViewById(R.id.q_spinner);
		mQSpinner.setEnabled(false);
		mRoundSpinner = (Spinner) view.findViewById(R.id.rounds_spinner);
		mRoundSpinner.setEnabled(false);
		mSessionSpinner = (Spinner) view.findViewById(R.id.session_spinner);
		mSessionSpinner.setEnabled(false);
		mTargetSpinner = (Spinner) view.findViewById(R.id.target_spinner);
		mTargetSpinner.setEnabled(false);
		mAntennaSpinner = (MultiSelectionSpinner) view.findViewById(R.id.antenna_spinner);
		mAntennaSpinner.setEnabled(false);
		mAutotuneCheckbox = (CheckBox)view.findViewById(R.id.autotune_checkbox);
		mAutotuneCheckbox.setEnabled(false);
		
		ArrayAdapter<CharSequence> regionSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.regions_entries, android.R.layout.simple_spinner_item);
		regionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRegionSpinner.setAdapter(regionSpinnerAdapter);
		mRegionSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupRegionId(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> txLevelSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.tx_level_entries, android.R.layout.simple_spinner_item);
		txLevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mTxLevelSpinner.setAdapter(txLevelSpinnerAdapter);
		mTxLevelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupTxLevel(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> linkFrequencySpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.link_frequency_entries, android.R.layout.simple_spinner_item);
		linkFrequencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mLinkFreqSpinner.setAdapter(linkFrequencySpinnerAdapter);
		mLinkFreqSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						switch (position) { // To set proper TX-level, we must use frequency integer in Hz
							case 0: 
								mApi.setSetupLinkFreq(NurApi.LINK_FREQUENCY_160000); 
								break;
							case 1:
								mApi.setSetupLinkFreq(NurApi.LINK_FREQUENCY_256000); 
								break;
							case 2: 
								mApi.setSetupLinkFreq(NurApi.LINK_FREQUENCY_320000); 
								break;
							default: 
								break;
						}
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> rxDecodingSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.rx_decoding_entries, android.R.layout.simple_spinner_item);
		rxDecodingSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRxDecodSpinner.setAdapter(rxDecodingSpinnerAdapter);
		mRxDecodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupRxDecoding(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		
		ArrayAdapter<CharSequence> txModulationSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.tx_modulation_entries, android.R.layout.simple_spinner_item);
		txModulationSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mTxModulSpinner.setAdapter(txModulationSpinnerAdapter);
		mTxModulSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupTxModulation(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> qSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.q_entries, android.R.layout.simple_spinner_item);
		qSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mQSpinner.setAdapter(qSpinnerAdapter);
		mQSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupInventoryQ(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> roundsSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.rounds_entries, android.R.layout.simple_spinner_item);
		roundsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRoundSpinner.setAdapter(roundsSpinnerAdapter);
		mRoundSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupInventoryRounds(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> sessionSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.session_entries, android.R.layout.simple_spinner_item);
		sessionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSessionSpinner.setAdapter(sessionSpinnerAdapter);
		mSessionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupInventorySession(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		ArrayAdapter<CharSequence> targetSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.target_entries, android.R.layout.simple_spinner_item);
		targetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mTargetSpinner.setAdapter(targetSpinnerAdapter);
		mTargetSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						mApi.setSetupInventoryTarget(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		mAntennaSpinner.setItems(new String[] { "Antenna1","Antenna2","Antenna3","Antenna4" });
		mAntennaSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
					try {
						int antennaMask = 0;
						for (int a : mAntennaSpinner.getSelectedIndices()) {
							antennaMask |= (1<<a);
						}
						if (antennaMask != 0) {
							mApi.setSetupAntennaMask(antennaMask);
							mApi.storeSetup(NurApi.STORE_RF);
						}
						else {
							Toast.makeText(SettingsApp.this.getActivity(), "At least one antenna must be selected", Toast.LENGTH_SHORT).show();
						}
						
					} catch (Exception e) {
						storeError(e);						
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});
		
		mAutotuneCheckbox.setOnCheckedChangeListener(mAutoTuneListener);
		
		if (mApi.isConnected()) {
			enableItems(true);
			setCurrentSetup();
		}
	}
	
	OnCheckedChangeListener mAutoTuneListener = new OnCheckedChangeListener() {
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (mApi.isConnected()) {
				try {
					AutotuneSetup setup = new AutotuneSetup();
					setup.mode = isChecked ? (AutotuneSetup.ATMODE_EN|AutotuneSetup.ATMODE_THEN) : AutotuneSetup.ATMODE_OFF;
					setup.thresholddBm = -10;
					mApi.setSetupAutotune(setup);
					mApi.storeSetup(NurApi.STORE_RF);
				} catch (Exception e) {
					storeError(e);
				}
			}
			
		}
	};
	
	void storeError(Exception e)
	{
		Toast.makeText(getActivity(), "Problem occured while setting reader setup", Toast.LENGTH_SHORT).show();
		e.printStackTrace();
	}
	
	private void setCurrentSetup() {

		final Object []listeners = new Object[11];
		int idx = 0;
		listeners[idx++] = mRegionSpinner.getOnItemSelectedListener();
		listeners[idx++] = mTxLevelSpinner.getOnItemSelectedListener();
		listeners[idx++] = mLinkFreqSpinner.getOnItemSelectedListener();
		listeners[idx++] = mRxDecodSpinner.getOnItemSelectedListener();
		listeners[idx++] = mTxModulSpinner.getOnItemSelectedListener();
		listeners[idx++] = mQSpinner.getOnItemSelectedListener();
		listeners[idx++] = mRoundSpinner.getOnItemSelectedListener();
		listeners[idx++] = mSessionSpinner.getOnItemSelectedListener();
		listeners[idx++] = mTargetSpinner.getOnItemSelectedListener();
		listeners[idx++] = mAntennaSpinner.getOnItemSelectedListener();
		listeners[idx++] = mAutoTuneListener;

		mRegionSpinner.setOnItemSelectedListener(null);
		mTxLevelSpinner.setOnItemSelectedListener(null);
		mLinkFreqSpinner.setOnItemSelectedListener(null);
		mRxDecodSpinner.setOnItemSelectedListener(null);
		mTxModulSpinner.setOnItemSelectedListener(null);
		mQSpinner.setOnItemSelectedListener(null);
		mRoundSpinner.setOnItemSelectedListener(null);
		mSessionSpinner.setOnItemSelectedListener(null);
		mTargetSpinner.setOnItemSelectedListener(null);			
		mAntennaSpinner.setOnItemSelectedListener(null);			
		mAutotuneCheckbox.setOnCheckedChangeListener(null);

		try {
			
			NurSetup setup = mApi.getModuleSetup();
			int regionSetup = setup.regionId;// mApi.getSetupRegionId();
			mRegionSpinner.setSelection(regionSetup);
			int txLevelSetup = setup.txLevel;// mApi.getSetupTxLevel();
			mTxLevelSpinner.setSelection(txLevelSetup);
			int linkFreqSetup = setup.linkFreq;// mApi.getSetupLinkFreq();
			
			switch (linkFreqSetup) {
				case NurApi.LINK_FREQUENCY_160000: 
					mLinkFreqSpinner.setSelection(0); 
					break;
				case NurApi.LINK_FREQUENCY_256000:
					mLinkFreqSpinner.setSelection(1); 
					break;
				case NurApi.LINK_FREQUENCY_320000: 
					mLinkFreqSpinner.setSelection(2); 
					break;
				default: 
					break;
			}
			
			int rxDecodSetup = setup.rxDecoding;// mApi.getSetupRxDecoding();
			mRxDecodSpinner.setSelection(rxDecodSetup);
			int txModulSetup = setup.txModulation;// mApi.getSetupTxModulation();
			mTxModulSpinner.setSelection(txModulSetup);
			int qSpinnerSetup = setup.inventoryQ;// mApi.getSetupInventoryQ();
			mQSpinner.setSelection(qSpinnerSetup);
			int roundsSetup = setup.inventoryRounds;// mApi.getSetupInventoryRounds();
			mRoundSpinner.setSelection(roundsSetup);
			int sessionSetup = setup.inventorySession;// mApi.getSetupInventorySession();
			mSessionSpinner.setSelection(sessionSetup);
			int targetSetup = setup.inventoryTarget;// mApi.getSetupInventoryTarget();
			mTargetSpinner.setSelection(targetSetup);
			
			int antennaMask = setup.antennaMask;
			ArrayList <Integer> selInd = new ArrayList<Integer>();
			for (int n=0; n<32; n++)
			{
				if ((antennaMask & (1<<n)) != 0) {
					selInd.add(n);
				}
			}
			mAntennaSpinner.setSelectionI(selInd);
			
			mAutotuneCheckbox.setChecked(setup.autotune.mode != 0);
			
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getActivity(), "Problem occured while retrieving readers setup", Toast.LENGTH_SHORT).show();
		}
		
		mTargetSpinner.post(new Runnable() {
		    @Override
			public void run() {
				int idx = 0;
				mRegionSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mTxLevelSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mLinkFreqSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mRxDecodSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mTxModulSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mQSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mRoundSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mSessionSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mTargetSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mAntennaSpinner.setOnItemSelectedListener((OnItemSelectedListener) listeners[idx++]);
				mAutotuneCheckbox.setOnCheckedChangeListener((OnCheckedChangeListener) listeners[idx++]);
		    }
		});
	}

	private void enableItems(boolean v) {
		mRegionSpinner.setEnabled(v);
		mTxLevelSpinner.setEnabled(v);
		mLinkFreqSpinner.setEnabled(v);
		mRxDecodSpinner.setEnabled(v);
		mTxModulSpinner.setEnabled(v);
		mQSpinner.setEnabled(v);
		mRoundSpinner.setEnabled(v);
		mSessionSpinner.setEnabled(v);
		mTargetSpinner.setEnabled(v);
		mAntennaSpinner.setEnabled(v);
		mAutotuneCheckbox.setEnabled(v);
	}
}
