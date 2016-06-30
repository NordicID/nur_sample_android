package com.nordicid.accessorydemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nordicid.nurapi.*;

class TagEPCAdapter extends BaseAdapter {
    NurTagStorage mTagStorage;
    Context mContext;
    LayoutInflater mInflater;

    public TagEPCAdapter(Context context, NurTagStorage tags) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mTagStorage = tags;
    }

    @Override
    public int getCount() {
        return mTagStorage.size();
    }

    @Override
    public Object getItem(int position) {
        return mTagStorage.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) mInflater.inflate(R.layout.rfid_tag_item, null);
        }

        NurTag tag = mTagStorage.get(position);
        final TextView tvEPC = ((TextView) vg.findViewById(R.id.tag_item_epc));
        final TextView tvRSSI = ((TextView) vg.findViewById(R.id.tag_item_rssi));

        // tvrssi.setVisibility(View.VISIBLE);

        tvEPC.setText(tag.getEpcString());
        tvRSSI.setText(String.valueOf(tag.getRssi()));

        return vg;
    }
}