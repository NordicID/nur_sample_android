package com.nordicid.rfiddemo;

import java.util.ArrayList;
import java.util.List;

import com.nordicid.nurapi.AntennaMapping;
import com.nordicid.nurapi.AutotuneSetup;
import com.nordicid.nurapi.NurApi;
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
import com.nordicid.nurapi.NurPacket;
import com.nordicid.nurapi.NurSetup;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SettingsAppSettingsTab extends Fragment
{
	SettingsAppTabbed mOwner;
	NurApi mApi;

    private ToggleButton mRegionLockDevice;
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
	private ArrayAdapter<String> mRegionSpinnerAdapter;

    private byte CMD_PRODUCTION_CFG = 0x76;

	private NurApiListener mThisClassListener = null;

	public NurApiListener getNurApiListener()
	{
		return mThisClassListener;
	}

	public SettingsAppSettingsTab() {
		mOwner = SettingsAppTabbed.getInstance();
		mApi = mOwner.getNurApi();

		mThisClassListener =  new NurApiListener() {
			@Override
			public void connectedEvent() {
				if (isAdded()) {
					mRegionSpinnerAdapter.clear();
					mRegionSpinnerAdapter.addAll(getDeviceRegions());
					mRegionSpinnerAdapter.notifyDataSetChanged();
					enableItems(true);
					readCurrentSetup();
				}
			}

			@Override
			public void disconnectedEvent() {
				if (isAdded()) {
					enableItems(false);
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
			@Override public void autotuneEvent(NurEventAutotune event) {}

			@Override
			public void tagTrackingScanEvent(NurEventTagTrackingData event) {

			}

			@Override
			public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {

			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_settings, container, false);
	}

	public void onVisibility(boolean val)
	{
		if (val)
		{
			if (isAdded()) {
				enableItems(mApi.isConnected());
				if (mApi.isConnected())
					readCurrentSetup();
			}
		}
	}

	private List<String> getDeviceRegions(){
        List<String> regions = new ArrayList<>();
		if(mApi.isConnected()) {
			try {
				Log.e("NUMREGIONS", "" + mApi.getReaderInfo().numRegions);
				for (int i = 0; i < mApi.getReaderInfo().numRegions; i++) {
					Log.e("REgion", "" + i);
					regions.add(mApi.getRegionInfo(i).name);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        return regions;
    }

	ArrayAdapter<CharSequence> txLevelSpinnerAdapter;
	ArrayAdapter<CharSequence> txLevelSpinnerAdapter1W;

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
        mRegionLockDevice = (ToggleButton) view.findViewById(R.id.regionLock_checkbox);
        mRegionLockDevice.setEnabled(false);

		mRegionSpinnerAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item, getDeviceRegions());
		mRegionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRegionSpinner.setAdapter(mRegionSpinnerAdapter);
		mRegionSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				if (mApi.isConnected()) {
                    int currentRegion = 0;
					try {
                        currentRegion = mApi.getSetupRegionId();
						mApi.setSetupRegionId(position);
						mApi.storeSetup(NurApi.STORE_RF);
					} catch (NurApiException e) {
                        mRegionSpinner.setSelection(currentRegion);
                        if(e.error == 5)
                            Toast.makeText(Main.getInstance(),"Failed to set region, Device is region locked",Toast.LENGTH_SHORT).show();
						else
                            storeError(e);
					} catch (Exception ex){
                        storeError(ex);
                    }
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});

		txLevelSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.tx_level_entries, android.R.layout.simple_spinner_item);
		txLevelSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		txLevelSpinnerAdapter1W = ArrayAdapter.createFromResource(getActivity(), R.array.tx_level_entries_1W, android.R.layout.simple_spinner_item);
		txLevelSpinnerAdapter1W.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
						int antennaMaskEx = 0;
						for (int a : mAntennaSpinner.getSelectedIndices()) {
							antennaMaskEx |= (1<<a);
						}
						if (antennaMaskEx != 0) {
							mApi.setSetupAntennaMaskEx(antennaMaskEx);
							mApi.storeSetup(NurApi.STORE_RF);
						}
						else {
							Toast.makeText(mOwner.getActivity(), "At least one antenna must be selected", Toast.LENGTH_SHORT).show();
						}

					} catch (Exception e) {
						storeError(e);
					}
				}
			}
			@Override public void onNothingSelected(AdapterView<?> arg0) {  }
		});

		mAutotuneCheckbox.setOnCheckedChangeListener(mAutoTuneListener);
        mRegionLockDevice.setOnClickListener(mRegionalLockListener);

		enableItems(mApi.isConnected());
		if (mApi.isConnected()) {
			readCurrentSetup();
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

    View.OnClickListener mRegionalLockListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRegionLockDevice.setChecked(!mRegionLockDevice.isChecked());
            if (mApi.isConnected()) {
                try {
                    final int region = mApi.getSetupRegionId();
                    final boolean lockState = mRegionLockDevice.isChecked();
                    AlertDialog.Builder alert = new AlertDialog.Builder(Main.getInstance());
                    alert.setTitle("Unlock Code");
                    alert.setMessage("Please enter the lock/unlock code.");
                    final EditText input = new EditText(Main.getInstance());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    alert.setView(input);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //TODO
                            try {
                                mApi.customCmd(CMD_PRODUCTION_CFG,(!lockState) ? getLockRegionCommand(input.getText().toString(),region) : getLockRegionCommand(input.getText().toString(),-1));
                                mApi.storeSetup(NurApi.STORE_RF);
                                mRegionLockDevice.setChecked(!lockState);
                            } catch (Exception e) {
                                Toast.makeText(Main.getInstance(),"Failed to " + ((lockState) ? "unlock" : "lock") + " device",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // do nothing
                        }
                    });
                    alert.show();
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

	private void readCurrentSetup() {

		final Object []listeners = new Object[12];
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
        listeners[idx++] = mRegionalLockListener;

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
        mRegionLockDevice.setOnClickListener(null);


		try {

			if (mApi.getDeviceCaps().isOneWattReader())
				mTxLevelSpinner.setAdapter(txLevelSpinnerAdapter1W);
			else
				mTxLevelSpinner.setAdapter(txLevelSpinnerAdapter);

			NurSetup setup = mApi.getModuleSetup();
			int regionSetup = setup.regionId;// mApi.getSetupRegionId();
			mRegionSpinner.setSelection(regionSetup);
			int txLevelSetup = setup.txLevel;// mApi.getSetupTxLevel();
			mTxLevelSpinner.setSelection(txLevelSetup);
			int linkFreqSetup = setup.linkFreq;// mApi.getSetupLinkFreq();

            /** Check if device region locked **/
            try{
                // try to change region to one other than the current one
				int testregion = regionSetup > 0 ? 0 : 1;
                mApi.setSetupRegionId(testregion);
                // expected NurAPIException 5 if device is region locked
                mRegionLockDevice.setChecked(false);
				// Restore
				mApi.setSetupRegionId(regionSetup);
            } catch (Exception e) {
                mRegionLockDevice.setChecked(true);
            }
            /** **/

			// Make sure antenna autoswitch is enabled
			if (setup.selectedAntenna != NurApi.ANTENNAID_AUTOSELECT)
				mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

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

			String []antStrings;
			AntennaMapping []mapping = mApi.getAntennaMapping();
			if (mapping != null && mapping.length > 0)
			{
				antStrings = new String[mapping.length];
				for (int n=0; n<mapping.length; n++)
					antStrings[n] = mapping[n].name;
			} else {
				antStrings = new String[mapping.length];
				for (int n=0; n<4; n++)
					antStrings[n] = "Antenna"+n;
			}
			mAntennaSpinner.setItems(antStrings);

			int antennaMaskEx = setup.antennaMaskEx;
			ArrayList <Integer> selInd = new ArrayList<Integer>();
			for (int n=0; n<32; n++)
			{
				if ((antennaMaskEx & (1<<n)) != 0) {
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
                mRegionLockDevice.setOnClickListener((View.OnClickListener) listeners[idx++]);
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
        mRegionLockDevice.setEnabled(v);
	}

    private byte[] getLockRegionCommand(String command,int regionId) throws Exception{
            byte[] commandAr = NurApi.hexStringToByteArray(command);
            if(regionId != -1) {
                byte[] cmdArray = new byte[commandAr.length + 1];
                System.arraycopy(commandAr, 0, cmdArray, 0, commandAr.length);
                cmdArray[cmdArray.length - 1] = (byte) regionId;
                return cmdArray;
            }
            return commandAr;
    }
}
