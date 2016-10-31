package com.nordicid.rfiddemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.nurapi.NurApiAutoConnectTransport;
import com.nordicid.nurapi.NurApiListener;

public class SettingsUpdateTab extends Fragment implements View.OnClickListener{

    SettingsAppTabbed mOwner;

    private Button mBtnUpdatdeNur = null;
    private Button mBtnUpdateBth = null;

    private NurApiListener mThisClassListener = null;

    public NurApiListener getNurApiListener()
    {
        return mThisClassListener;
    }

    public SettingsUpdateTab() {
        mOwner = SettingsAppTabbed.getInstance();
    }

    @Override
    public void onAttach(Activity context){
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_settings_update, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NurApiAutoConnectTransport autoConnectTransport = Main.getInstance().getNurAutoConnect();
        mBtnUpdatdeNur = (Button) view.findViewById(R.id.btn_update_nur_fw);
        mBtnUpdateBth = (Button) view.findViewById(R.id.btn_update_bth);
        if(autoConnectTransport == null || !autoConnectTransport.getType().equalsIgnoreCase("BLE"))
            mBtnUpdateBth.setVisibility(View.GONE);
        mBtnUpdatdeNur.setOnClickListener(this);
        mBtnUpdateBth.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_update_bth:
                AppTemplate.getAppTemplate().setApp("BLE Firmware Update",false);
                break;
            case R.id.btn_update_nur_fw:
                AppTemplate.getAppTemplate().setApp("NUR Firmware Update",false);
                break;
            default:
                break;
        }
    }
}
