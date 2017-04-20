package com.nordicid.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nordicid.rfiddemo.R;

import java.util.List;

public class UpdateContainerListAdapter extends ArrayAdapter<UpdateContainer>{

    public UpdateContainerListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public UpdateContainerListAdapter(Context context, int resource, List<UpdateContainer> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.update_container_list_item, null);
        }

        UpdateContainer p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.update_name);
            TextView tt2 = (TextView) v.findViewById(R.id.version);
            TextView tt3 = (TextView) v.findViewById(R.id.build_time);

            if (tt1 != null) {
                tt1.setText(p.name);
            }

            if (tt2 != null) {
                tt2.setText(p.version);
            }

            if (tt3 != null) {
                tt3.setText(p.buildtime);
            }
        }

        return v;
    }
}
