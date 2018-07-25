package com.android.blutoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class DeviceListAdapter extends BaseAdapter{
    private List<BluetoothDevice> deviceList;
    private Context context;

    public DeviceListAdapter(List<BluetoothDevice> list, Context context) {
        this.deviceList = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int i) {
        return deviceList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = null;
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.item_device,viewGroup,false);
            holder = new ViewHolder();
            holder.tv_sName = (TextView) view.findViewById(R.id.tv_sName);
            holder.tv_sMac = (TextView) view.findViewById(R.id.tv_sMac);
            holder.tv_sType = (TextView) view.findViewById(R.id.tv_sType);
            holder.tv_sState = (TextView) view.findViewById(R.id.tv_sState);
            view.setTag(holder);   //将Holder存储到convertView中
        }else{
            holder = (ViewHolder) view.getTag();
        }
        //详细参考：http://blog.csdn.net/mirkowu/article/details/53862842
        BluetoothDevice blueDevice = deviceList.get(i);
        String deviceName = blueDevice.getName();
        holder.tv_sName.setText(TextUtils.isEmpty(deviceName) ? "未知设备" : deviceName);
        holder.tv_sMac.setText(blueDevice.getAddress());
        //设备的蓝牙设备类型（DEVICE_TYPE_CLASSIC 传统蓝牙 常量值：1, DEVICE_TYPE_LE  低功耗蓝牙 常量值：2
        //DEVICE_TYPE_DUAL 双模蓝牙 常量值：3. DEVICE_TYPE_UNKNOWN：未知 常量值：0）
        int deviceType = blueDevice.getType();
        String dType = "";
        if (deviceType == 0) {
            dType = "未知类型";
        } else if (deviceType == 1) {
            dType = "传统蓝牙";
        } else if (deviceType == 2) {
            dType = "低功耗蓝牙";
        } else if (deviceType == 3) {
            dType = "双模蓝牙";
        }
        holder.tv_sType.setText(dType);

        //设备的状态（BOND_BONDED：已绑定 常量值：12, BOND_BONDING：绑定中 常量值：11, BOND_NONE：未匹配 常量值：10）
        int deviceState = blueDevice.getBondState();
        String dState = "";
        if (deviceState == 10) {
            dState = "未配对";
        } else if (deviceState == 11) {
            dState = "配对中";
        } else if (deviceState == 12) {
            dState = "已配对";
        }
        holder.tv_sState.setText(dState);

        return view;
    }
    static class ViewHolder{
        TextView tv_sName;
        TextView tv_sMac;
        TextView tv_sType;
        TextView tv_sState;
    }
}
