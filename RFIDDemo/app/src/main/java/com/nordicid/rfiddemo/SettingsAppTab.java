package com.nordicid.rfiddemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.nordicid.nurapi.NurApiListener;

public class SettingsAppTab extends Fragment {
    SettingsAppTabbed mOwner;

    CheckBox mScreenOrientationCheckBox;
    CheckBox mAppSoundsCheckBox;
    CheckBox mUpdateCheck;
    boolean rotationEnabled = false;
    boolean soundsEnabled = false;
    boolean updateCheckingEnabled = false;
    SharedPreferences settings = null;
    SharedPreferences.Editor settingEditor = null;

    private NurApiListener mThisClassListener = null;

    public NurApiListener getNurApiListener()
    {
        return mThisClassListener;
    }

    public SettingsAppTab() {
        mOwner = SettingsAppTabbed.getInstance();
    }

    private void enableItems(boolean v) {
        mScreenOrientationCheckBox.setEnabled(v);
        mAppSoundsCheckBox.setEnabled(v);
    }

    @Override
    public void onAttach(Activity context){
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        settings = Main.getApplicationPrefences();
        settingEditor = settings.edit();
        rotationEnabled = settings.getBoolean("Rotation",false);
        soundsEnabled = !settings.getBoolean("Sounds",true);
        updateCheckingEnabled = settings.getBoolean("CheckUpdate",false);
        return inflater.inflate(R.layout.tab_settings_app, container, false);
    }


    OnCheckedChangeListener mOnCheckedChangeListenerSounds = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            settingEditor.putBoolean("Sounds",!isChecked);
            settingEditor.apply();
            Beeper.setEnabled(!isChecked);
        }
    };

    OnCheckedChangeListener mOnCheckedChangeListenerRotation = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            settingEditor.putBoolean("Rotation",mScreenOrientationCheckBox.isChecked());
            settingEditor.apply();
            ((Main)getActivity()).toggleScreenRotation(mScreenOrientationCheckBox.isChecked());
        }
    };

    OnCheckedChangeListener mOnCheckedChangeListenerUpdates = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            settingEditor.putBoolean("CheckUpdate",isChecked);
            settingEditor.apply();
        }
    };


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mScreenOrientationCheckBox = (CheckBox)view.findViewById(R.id.enable_orientation_checkbox);
        mAppSoundsCheckBox = (CheckBox)view.findViewById(R.id.enable_sounds_checkbox);
        mUpdateCheck = (CheckBox)view.findViewById(R.id.disable_update_check);
        mUpdateCheck.setOnCheckedChangeListener(mOnCheckedChangeListenerUpdates);
        mScreenOrientationCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListenerRotation);
        mAppSoundsCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListenerSounds);
        mScreenOrientationCheckBox.setChecked(rotationEnabled);
        mUpdateCheck.setChecked(updateCheckingEnabled);
        mAppSoundsCheckBox.setChecked(soundsEnabled);
    }
}
