package com.nordicid.rfiddemo;

import java.util.ArrayList;

import com.nordicid.nurapi.AntennaMapping;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurTuneResponse;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsAppTuneTab extends Fragment 
{
	SettingsAppTabbed mOwner;
	
	private Button mTuneButton;
	private Button mReflPowerButton;
	private TextView mEditText;
	
	Handler mHandler;
	NurApi mApi;
	
	public SettingsAppTuneTab() {
		mOwner = SettingsAppTabbed.getInstance();
		mApi = mOwner.getNurApi();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_settings_tune, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mTuneButton = (Button)view.findViewById(R.id.tune_button);
		mTuneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doTune();
			}
		});
		
		mReflPowerButton = (Button)view.findViewById(R.id.refl_power_button);
		mReflPowerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doReflPower();
			}
		});
		
		mEditText = (TextView)view.findViewById(R.id.tune_text);
        /** enable scrolling to see all antennas' details on landscape (small screen)**/
        mEditText.setMovementMethod( new ScrollingMovementMethod());
	}
	
	String text;
	ProgressDialog barProgressDialog;
	int mCurAnt = 0;
	AntennaMapping[] mAntMapping = null;

	void doTune()
	{
		//if (barProgressDialog == null)
		{
			barProgressDialog = new ProgressDialog(mOwner.getAppTemplate());
		
		    barProgressDialog.setTitle("Tuning ...");
		    barProgressDialog.setMessage("Starting tuning...");
		    barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);		    
		}
		
		if (mHandler == null)
		{
			mHandler = new Handler(Looper.getMainLooper()) {
	            @Override
	            public void handleMessage(Message inputMessage) {
	            	if (inputMessage.what == 0)
	            	{
	            		int ant = (Integer)inputMessage.obj;
	            		String text;
						if (ant >= mAntMapping.length)
							text = "Tuning antenna #" + (ant+1);
						else
							text = "Tuning " + mAntMapping[ant].name;

						barProgressDialog.setMessage(text);
	            	}
	            	else if (inputMessage.what == 1)
	            	{
	            		mCurAnt++;
	            		barProgressDialog.setProgress(mCurAnt);
	            		
	            		NurTuneResponse[] r = (NurTuneResponse[])inputMessage.obj;
						if (r[0].antenna >= mAntMapping.length)
	            			text += "Antenna #" + (r[0].antenna+1) + ":\n";
						else
							text += mAntMapping[r[0].antenna].name + ":\n";

	            		for (int n=0; n<r.length; n++)
	            		{
	            			if (n > 0)
	            				text += "; ";
	            			text += r[n].frequency + "=" + r[n].dBm;
	            		}
	            		text += "\n\n";
	            		
	            		mEditText.setText(text);
	            	}
	            	else if (inputMessage.what == 2)
	            	{
	            		barProgressDialog.dismiss();
	            	}
	            }
			};	
		}
		
		if (mApi.isConnected())
		{
			try {
				
				mEditText.setText("Please wait..");
				text = "Tune Results\n\n";

				try {
					mAntMapping = mApi.getAntennaMapping();
				} catch (Exception e) { }
				
				int antennaMask = mApi.getSetupAntennaMaskEx();
				final ArrayList <Integer> selInd = new ArrayList<Integer>();
				for (int n=0; n<32; n++)
				{
					if ((antennaMask & (1<<n)) != 0) {
						selInd.add(n);
					}
				}
				
				mCurAnt = 0;
				barProgressDialog.setProgress(0);
				barProgressDialog.setMax(selInd.size());
			    barProgressDialog.show();
				
				new Thread(new Runnable() {
					@Override
					public void run() 
					{
						NurTuneResponse[] r;
						Message completeMessage;
						
						try {
							for (int n=0; n<selInd.size(); n++)
							{
								completeMessage = mHandler.obtainMessage(0, n);
				                completeMessage.sendToTarget();
								r = mApi.tuneAntenna(selInd.get(n), true, true);
								completeMessage = mHandler.obtainMessage(1, r);
				                completeMessage.sendToTarget();
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						completeMessage = mHandler.obtainMessage(2);
		                completeMessage.sendToTarget();
					}
				}).start();				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(getActivity(), "Tune failed", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
		}
	}
	
	void doReflPower()
	{
		if (mApi.isConnected())
		{
			try {
				mEditText.setText("Please wait..");
				text = "Reflected power\n\n";

				try {
					mAntMapping = mApi.getAntennaMapping();
				} catch (Exception e) { }
				
				int antennaMask = mApi.getSetupAntennaMaskEx();
				final ArrayList <Integer> selInd = new ArrayList<Integer>();
				for (int n=0; n<32; n++)
				{
					if ((antennaMask & (1<<n)) != 0) {
						selInd.add(n);
					}
				}
				
				int curSel = mApi.getSetupSelectedAntenna();

				try {
					for (int n=0; n<selInd.size(); n++)
					{
						mApi.setSetupSelectedAntenna(selInd.get(n));
						float val = mApi.getReflectedPowerF();		
						//text += "Antenna " + (selInd.get(n)+1) + ": "+val+"\n";

						if (selInd.get(n) >= mAntMapping.length)
							text += "Antenna #"+(selInd.get(n)+1);
						else
							text += mAntMapping[selInd.get(n)].name;

						text += ": "+val+"\n";
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				mApi.setSetupSelectedAntenna(curSel);
				mEditText.setText(text);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(getActivity(), "Measure reflected power failed", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
		}
	}
}
