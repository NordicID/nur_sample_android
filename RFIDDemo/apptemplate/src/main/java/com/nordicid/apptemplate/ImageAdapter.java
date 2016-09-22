package com.nordicid.apptemplate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ImageAdapter extends BaseAdapter {
    private SubAppList list;

    public ImageAdapter(SubAppList list) {
        this.list = list;
    }

    @Override
	public int getCount() {
        return list.getVisibleApps().size();
    }

    @Override
	public Object getItem(int position) {
        return null;
    }

    @Override
	public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    @Override
	@SuppressLint("InflateParams")
	public View getView(int position, View convertView, ViewGroup parent) {
        
    	View gridItem;
    	int orientation = list.getResources().getConfiguration().orientation;
    	//int currentOpenSubAppIndex = list.getCurrentOpenSubAppIndex();
        
        if (convertView == null) {
        	gridItem = LayoutInflater.from(list.getContext()).inflate(R.layout.subapplist_item, null);
        } 
        else {
        	gridItem = convertView;
        }
        
        RelativeLayout container = (RelativeLayout) gridItem.findViewById(R.id.item_container);
        ImageView icon = (ImageView) gridItem.findViewById(R.id.item_icon);
        TextView name = (TextView) gridItem.findViewById(R.id.item_name);
        
        gridItem.setSelected(true);
        
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)list.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        
        int itemSize = list.getGridView().getColumnWidth();
        int iconSize;
        
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        	iconSize = (int) (itemSize / 1.5);
        	name.setTextSize(20);
        }
        else {
        	iconSize = (int) (itemSize / 1.7);
        	name.setTextSize(16);
        }
        
        AbsListView.LayoutParams containerParams = (AbsListView.LayoutParams) container.getLayoutParams();
        
        //if not recycled view
        if (containerParams == null) {
        	container.setLayoutParams(new AbsListView.LayoutParams(itemSize,itemSize));
        }
        else {
        	containerParams.height = itemSize;
            container.setLayoutParams(containerParams);
        }
        
        RelativeLayout.LayoutParams imageViewParams = (RelativeLayout.LayoutParams) icon.getLayoutParams();
        
        if (imageViewParams == null) {
        	icon.setLayoutParams(new RelativeLayout.LayoutParams(iconSize,iconSize));
        }
        else {
        	imageViewParams.width = iconSize;
        	imageViewParams.height = iconSize;
        	icon.setLayoutParams(imageViewParams);
        }
        
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        final SubApp app = list.getVisibleApp(position);
        //sets the highlighting for subapp icon if needed

        icon.setImageDrawable(list.getResources().getDrawable(app.getTileIcon()));
        name.setText(app.getAppName());
        
        return container;
    }
  
   
}
