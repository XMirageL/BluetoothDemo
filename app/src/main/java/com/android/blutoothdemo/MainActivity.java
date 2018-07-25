package com.android.blutoothdemo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BluetoothDemo";
    private long mPressedTime = 0;
    //组件
    private Button btn_link, btn_send;
    private TextView tv_state, tv_device;
    private SelectDevicePopupWindow selectDevicePopupWindow;
    private ProgressDialog progressDialog;
    private EditText et_send;
    private ListView lv_message;
    private ChatAdapter chatAdapter;
    private ArrayList<MessageForChat> messageForChatList;

    //蓝牙
    private BluetoothService bluetoothService;
    private DeviceListAdapter deviceListAdapter;
    private List<BluetoothDevice> deviceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //沉浸状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //亮色状态栏背景,状态栏字体变成黑色
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

        }
        bandViews();
        initialization();
    }

    /**
     * 设置添加屏幕的背景透明度
     * 双击返回退出程序
     */
    @Override
    public void onBackPressed() {
        long mNowTime = System.currentTimeMillis();//获取第一次按键时间
        if ((mNowTime - mPressedTime) > 2000) {//比较两次按键时间差
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            mPressedTime = mNowTime;
        } else {//退出程序
            this.finish();
            System.exit(0);
        }
    }

    private void bandViews() {
        btn_link = findViewById(R.id.btn_link);
        btn_link.setOnClickListener(this);
        btn_send = findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        et_send = findViewById(R.id.et_send);
        tv_state = findViewById(R.id.tv_state);
        tv_device = findViewById(R.id.tv_device);
        lv_message = findViewById(R.id.lv_message);

        messageForChatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageForChatList);
        lv_message.setAdapter(chatAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_link:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                // 隐藏软键盘
                assert imm != null;
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                if (!bluetoothService.isBluetoothEnabled()) {
                    Toast.makeText(MainActivity.this, "蓝牙未打开", Toast.LENGTH_SHORT).show();
                    break;
                }
                //初始化数据和适配器
                deviceList = new ArrayList<>();
                deviceListAdapter = new DeviceListAdapter(deviceList, MainActivity.this);
                //将数据传入以获得已配对的设备
                bluetoothService.startScanBluth(deviceList);
                showSelectDevice();
                break;
            case R.id.btn_send:
                String string = et_send.getText().toString();
                bluetoothService.sendMessage(string);
                Log.d(TAG, "发送消息:  " + string);
                break;
        }
    }


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MSG_CONNECT_SUCCESS:
                    tv_state.setText("已连接");
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    tv_device.setText(device.getName() + "(" + device.getAddress() + ")");
                    dismissSelectDevice();
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    btn_link.setText("重连");
                    break;
                case BluetoothService.MSG_CONNECT_FAILED:
                    Toast.makeText(MainActivity.this, "连接失败,请重试", Toast.LENGTH_SHORT).show();
                    tv_state.setText("连接失败,请重连");
                    btn_link.setText("重连");
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    break;
                case BluetoothService.MSG_WAIT_CONNECT:
                    dismissSelectDevice();
                    device = (BluetoothDevice) msg.obj;
                    Toast.makeText(MainActivity.this, device.getName() + "正在请求连接", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MSG_READ_STRING:
                    String string = msg.obj.toString();
                    Log.d(TAG, "收到消息: " + string);
                    messageForChatList.add(new MessageForChat(false, string));
                    chatAdapter.notifyDataSetChanged();
                    lv_message.smoothScrollToPosition(messageForChatList.size());
                    break;
                case BluetoothService.MSG_WRITE_STRING:
                    string = msg.obj.toString();
                    Log.d(TAG, "发送消息: " + string);
                    messageForChatList.add(new MessageForChat(true, string));
                    chatAdapter.notifyDataSetChanged();
                    lv_message.smoothScrollToPosition(messageForChatList.size());
                    et_send.setText("");
                    break;
                case BluetoothService.MSG_START_CONNECT:
                    progressDialog.setMessage("正在连接");
                    progressDialog.show();
                    break;
                case BluetoothService.MSG_DIS_CONNECT:
                    tv_state.setText("设备未连接");
                    tv_device.setText("点击连接即可选择设备");
                    btn_link.setText("连接");
                    Toast.makeText(MainActivity.this, "连接已经断开", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.EXIT_APP:
                    finish();
                    break;
            }
        }
    };


    /**
     * 设置添加屏幕的背景透明度
     *
     * @param bgAlpha 透明度
     */
    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }


    /**
     * 弹出选择设备窗口
     */
    private void showSelectDevice() {
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = deviceList.get(i);
                tv_state.setText("正在连接");
                tv_device.setText(bluetoothDevice.getName() + "(" + bluetoothDevice.getAddress() + ")");
                bluetoothService.connectBluetoothDevice(bluetoothDevice);
            }
        };
        selectDevicePopupWindow = new SelectDevicePopupWindow(MainActivity.this, listener
                , deviceListAdapter);

        //设置消失事件
        selectDevicePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(1f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //亮色状态栏背景,状态栏字体变成黑色
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        });

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //弹出位置
        selectDevicePopupWindow.showAtLocation(findViewById(R.id.rl_main),
                Gravity.BOTTOM, 0, 0);
        //背景半透明
        backgroundAlpha(0.8f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //亮色状态栏背景,状态栏字体变成黑色
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /**
     * 隐藏选择设备窗口
     */
    private void dismissSelectDevice() {
        if (selectDevicePopupWindow != null && selectDevicePopupWindow.isShowing()) {
            selectDevicePopupWindow.dismiss();
        }
    }


    /**
     * 广播接收器
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        // 收到的广播类型
        String action;
        // 从intent中获取设备
        BluetoothDevice device;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                //找到设备
                case BluetoothDevice.ACTION_FOUND: {
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        if (!deviceList.contains(device)) {
                            Log.e(TAG, "找到蓝牙设备：" + device.getName() + ":" + device.getAddress());
                            deviceList.add(device);
                        }
                        deviceListAdapter.notifyDataSetChanged();
                    }
                    break;
                }
                //结束查找设备
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    Log.d(TAG, "onReceive: 结束查找设备,找到"+deviceList.size());
                    break;
                }
                //开始查找设备
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    Log.d(TAG, "onReceive: 开始查找设备");
                    break;
                }
                //配对状态改变
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    //配对状态
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDING://正在配对
                            progressDialog.setMessage("正在配对...");
                            progressDialog.show();
                            Log.e(TAG, "onReceive: 正在配对...");
                            break;
                        case BluetoothDevice.BOND_BONDED://配对结束
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Log.e(TAG, "onReceive: 配对成功");
                            bluetoothService.connectBluetoothDevice(device);
                            break;
                        case BluetoothDevice.BOND_NONE://取消配对未配对
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Log.e(TAG, "onReceive: 取消配对");
                        default:
                            break;
                    }
                    break;
                }
                //蓝牙开启状态改变
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    switch (state) {
                        //蓝牙关闭则退出本应用
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e(TAG, "蓝牙关闭");
                            Toast.makeText(MainActivity.this, "蓝牙已关闭!!!", Toast.LENGTH_SHORT).show();
                            bluetoothService.reOpenBluetooth();
                            break;
                        //蓝牙打开后再开启服务端
                        case BluetoothAdapter.STATE_ON:
                            bluetoothService.startServerThread();
                            //允许其他设备检测
                            Intent mIntent = new Intent(
                                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            startActivityForResult(mIntent, 1);

                            Log.e(TAG, "蓝牙打开");
                            break;
                    }
                    break;
                }

            }
        }
    };


    /**
     * 初始化
     */
    private void initialization() {
        // 提示框
        //初始化加载提示框
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("正在初始化,请给予程序必要的权限");
        progressDialog.show();
        bluetoothService = new BluetoothService(this, handler);

        //注册异步搜索蓝牙设备的广播
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙开关发生了变化
        intent.addAction(BluetoothDevice.ACTION_FOUND);//搜索发现设备
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//搜索完成
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//搜索开始
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//绑定状态改变
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
        registerReceiver(receiver, intent);
        // 注册广播
        Log.d(TAG, "startDiscovery: 注册广播");

        //6.0以上定义获取基于地理位置的动态权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                bluetoothService.openBluetooth();
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        } else {
            bluetoothService.openBluetooth();
        }

        if(bluetoothService.isBluetoothEnabled()){
            Intent mIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(mIntent, 1);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            //允许其他设备检测回调 1 是调用时传的code
            if (resultCode == RESULT_OK) {
                Log.e(TAG,"允许本地蓝牙被附近的其它蓝牙设备发现");
                Toast.makeText(this, "允许本地蓝牙被附近的其它蓝牙设备发现", Toast.LENGTH_SHORT)
                        .show();
            } else if (resultCode == RESULT_CANCELED) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提示")
                        .setMessage("应用开启蓝牙开放检测才能正常运行,是否重新开启")
                        .setCancelable(false)
                        .setNegativeButton("退出应用", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setPositiveButton("重新开启", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent mIntent = new Intent(
                                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                                startActivityForResult(mIntent, 1);
                            }
                        })
                        .show();
            }
        }

    }




    /**
     * 请求权限的回调
     *
     * @param requestCode  1
     * @param permissions  32
     * @param grantResults 2
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                //请求位置权限的回调,判断用户是否给了权限
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "获得权限");
                    bluetoothService.openBluetooth();
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")
                            .setMessage("应用需要位置权限才能正常运行,是否重新授权")
                            .setCancelable(false)
                            .setNegativeButton("退出应用", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setPositiveButton("重新授权", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                                }
                            })
                            .show();
                }
                break;
        }
    }

}
