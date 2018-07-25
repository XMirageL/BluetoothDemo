package com.android.blutoothdemo;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SelectDevicePopupWindow extends PopupWindow {
    private TextView tv_cancel;
    private ListView lv_deviceList;
    private View view;

    public SelectDevicePopupWindow(Context context ,
                                   AdapterView.OnItemClickListener listener, DeviceListAdapter adapter) {
        super(context);
        //新建View
        LayoutInflater layoutInflater = (LayoutInflater) context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.dialog_select_device, null);
        tv_cancel =  view.findViewById(R.id.tvbtn_cancel);
        lv_deviceList = view.findViewById(R.id.lv_deviceList);
        //设置监听器和适配器
        lv_deviceList.setAdapter(adapter);
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        lv_deviceList.setOnItemClickListener(listener);

        //将view绑定
        this.setContentView(view);
        //设置高和宽
        this.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        this.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        //设置焦点
        this.setFocusable(true);
        //颜色
        ColorDrawable dw = new ColorDrawable(0x00000000);
        this.setBackgroundDrawable(dw);
        this.setAnimationStyle(R.style.PopupAnimation);
    }

}
