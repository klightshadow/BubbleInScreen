package com.lightshadow.bubbleinscreen.adapters;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lightshadow.bubbleinscreen.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lightshadow on 2014/12/17.
 */
public class DevicesAdapter extends BaseAdapter {

    LayoutInflater layoutInflater;
    private List<BluetoothDevice> list = new ArrayList<BluetoothDevice>();

    public DevicesAdapter(Activity activity) {
        layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addList(BluetoothDevice btDevice) {
        list.add(btDevice);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class viewHolder {
        TextView deviceName;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        viewHolder holder;
        if(convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_devicelist, null);
            holder = new viewHolder();
            holder.deviceName = (TextView)convertView.findViewById(R.id.item_tv_deviceName);
            convertView.setTag(holder);
        } else {
            holder = (viewHolder)convertView.getTag();
        }

        holder.deviceName.setText(list.get(position).getName());
        return convertView;
    }

    public void clearList() {
        if(!list.isEmpty()) {
            list.clear();
            notifyDataSetChanged();
        }
    }
}
