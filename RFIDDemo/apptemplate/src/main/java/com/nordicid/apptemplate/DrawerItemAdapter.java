package com.nordicid.apptemplate;

import java.util.ArrayList;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DrawerItemAdapter extends BaseAdapter {
	
	private Context context;
	private ArrayList<String> items;

	public DrawerItemAdapter(Context context, ArrayList<String> items) {
		this.context = context;
		this.items = items;
	}
	
	@Override
	public int getCount() {
		int count = items.size();
		return count;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View drawerItem;
		
		if (convertView == null) {
			drawerItem = LayoutInflater.from(context).inflate(R.layout.drawer_list_item, null);
		}
		else {
			drawerItem = convertView;
		}
		
		TextView title = (TextView) drawerItem.findViewById(R.id.drawer_item_title);
		title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
		String text = items.get(position).toString();
		title.setText(text);
        
		return drawerItem;
	}

}
