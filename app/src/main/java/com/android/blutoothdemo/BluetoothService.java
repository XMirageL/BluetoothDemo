package com.android.blutoothdemo;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";

    private Context context;
    private Handler handler;

    //蓝牙
    private BluetoothSocket bluetoothSocket, socketFormServer;
    private BluetoothServerSocket bluetoothServerSocket;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectedThread connectedThread;
    private BluetoothAdapter bluetoothAdapter;

    //请求
    public static final int EXIT_APP = -1;//退出应用
    public static final int MSG_WAIT_CONNECT = 0;//等待连接
    public static final int MSG_CONNECT_SUCCESS = 1;//连接成功
    public static final int MSG_WRITE_STRING = 2;//发送消息
    public static final int MSG_READ_STRING = 3;//接收消息
    public static final int MSG_CONNECT_FAILED = 4;//连接失败
    public static final int MSG_START_CONNECT = 5;//开始连接
    public static final int MSG_DIS_CONNECT = 6;//断开连接

    public BluetoothService(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBluetoothEnabled(){
        if(bluetoothAdapter == null){
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }


    /**
     * 搜索蓝牙的方法
     */
    void startScanBluth(List<BluetoothDevice> deviceList){
        if(isBluetoothEnabled()){
            //获得已经绑定的设备
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bond : bondedDevices) {
                if (!deviceList.contains(bond)) {
                    deviceList.add(bond);
                }
            }
            // 搜索蓝牙的方法
            // 判断是否在搜索,如果在搜索，就取消搜索
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            // 开始搜索
            bluetoothAdapter.startDiscovery();
        }
    }

    /**
     * 打开蓝牙
     */
    void openBluetooth() {
        // 开启蓝牙
        //1、初始化蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 2、判断设备是否支持蓝牙功能
        if (bluetoothAdapter == null) {
            //设备不支持蓝牙功能
            Toast.makeText(context, "当前设备不支持蓝牙功能！", Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(context)
                    .setTitle("提示")
                    .setMessage("应用需要蓝牙才能正常运行,当前设备不支持蓝牙功能！")
                    .setCancelable(false)
                    .setNegativeButton("退出应用", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Message message = new Message();
                            message.what = EXIT_APP;
                            handler.sendMessage(message);
                        }
                    })
                    .show();
        }
        // 3、打开设备的蓝牙功能
        if (bluetoothAdapter!=null && !bluetoothAdapter.isEnabled()) {
            boolean enable = bluetoothAdapter.enable(); //返回值表示 是否成功打开了蓝牙设备
            if (enable) {
                Toast.makeText(context, "打开蓝牙功能成功！", Toast.LENGTH_SHORT).show();
            } else {
                reOpenBluetooth();
            }
        } else {
            // 启动服务器端
            startServerThread();
        }
    }

    //重新打开蓝牙
    void reOpenBluetooth() {
        new AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("应用需要蓝牙才能正常运行,是否重新打开")
                .setCancelable(false)
                .setNegativeButton("退出应用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Message message = new Message();
                        message.what = EXIT_APP;
                        handler.sendMessage(message);
                    }
                })
                .setPositiveButton("重新打开", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openBluetooth();
                    }
                })
                .show();
    }

    //连接指定设备
    public void connectBluetoothDevice(BluetoothDevice bluetoothDevice){
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        //配对
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            //如果这个设备取消了配对，则尝试配对
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bluetoothDevice.createBond();
            }
        } else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            //如果这个设备已经配对完成，则尝试连接
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ClientThread clientThread = new ClientThread(bluetoothDevice);
            clientThread.start();
        }
    }

    //启动服务端
    public void startServerThread(){
        // 启动服务器端
        ServerThread serverThread = new ServerThread();
        serverThread.start();
    }

    //发送消息
    public void sendMessage(String msgStr){
        if (connectedThread == null) {
            Toast.makeText(context, "还没有连接", Toast.LENGTH_SHORT).show();
        } else{
            if (msgStr.length() > 0) {
                connectedThread.write(msgStr);
            } else {
                Toast.makeText(context, "请输入消息", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class ClientThread extends Thread {
        ClientThread(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
            } catch (IOException e) {
                Log.e(TAG, "ClientThread: ", e);
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothSocket != null) {
                        try {
                            Log.d(TAG, "run: isConnected" + bluetoothSocket.isConnected());
                            Message message = new Message();
                            //开始连接
                            message.what = MSG_START_CONNECT;
                            message.obj = bluetoothSocket.getRemoteDevice();
                            handler.sendMessage(message);
                            bluetoothSocket.connect();
                            Log.d(TAG, "run: isConnected" + bluetoothSocket.isConnected());
                            if (bluetoothSocket.isConnected()) {
                                message = new Message();
                                //连接成功
                                message.what = MSG_CONNECT_SUCCESS;
                                message.obj = bluetoothSocket.getRemoteDevice();
                                handler.sendMessage(message);
                                connectedThread = new ConnectedThread(bluetoothSocket);
                                connectedThread.start();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "run: ", e);
                            Message message = new Message();
                            //连接失败
                            message.what = MSG_CONNECT_FAILED;
                            handler.sendMessage(message);
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }


    //服务端线程
    private class ServerThread extends Thread {
        ServerThread() {
            Log.d(TAG, "ServerThread: 构件服务端");
            try {
                bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().
                        listenUsingRfcommWithServiceRecord("test", BT_UUID);
            } catch (Exception e) {
                Log.e(TAG, "ServerThread: construct", e);
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            while (bluetoothServerSocket != null) {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    try {
                        Log.e(TAG, "run: serverThread  等待连接");
                        socketFormServer = bluetoothServerSocket.accept();
                        Message message = new Message();
                        //有设备请求连接
                        message.what = MSG_WAIT_CONNECT;
                        message.obj = socketFormServer.getRemoteDevice();
                        handler.sendMessage(message);
                    } catch (IOException e) {
                        Log.e(TAG, "run: ServerThread", e);
                        e.printStackTrace();
                        break;
                    }
                }
                if (socketFormServer != null) {
                    Log.d(TAG, "run: connect success");
                    connectedThread = new ConnectedThread(socketFormServer);
                    connectedThread.start();
                    //连接成功
                    Message message = new Message();
                    message.what = MSG_CONNECT_SUCCESS;
                    message.obj = socketFormServer.getRemoteDevice();
                    handler.sendMessage(message);
                } else {
                    //连接失败
                    Message message = new Message();
                    message.what = MSG_CONNECT_FAILED;
                    handler.sendMessage(message);
                }
            }
        }
    }


    private class ConnectedThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        private OutputStream outputStream;
        private InputStream inputStream;
        byte[] bytes = new byte[1024];


        ConnectedThread(BluetoothSocket bluetoothSocket) {
            this.bluetoothSocket = bluetoothSocket;
            try {
                outputStream = this.bluetoothSocket.getOutputStream();
                inputStream = this.bluetoothSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            int i;
            if (inputStream == null)
                return;
            do {
                try {
                    i = inputStream.read(bytes);
                    Message message = new Message();
                    message.what = MSG_READ_STRING;
                    message.obj = new String(bytes, 0, i);
                    handler.sendMessage(message);
                } catch (IOException e) {
                    Message message = new Message();
                    message.what = MSG_DIS_CONNECT;
                    handler.sendMessage(message);
                    e.printStackTrace();
                    break;
                }

            } while (i != 0);
        }

        void write(String string) {
            byte[] bytes;
            bytes = string.getBytes();
            try {
                outputStream.write(bytes);
                Message message = new Message();
                message.what = MSG_WRITE_STRING;
                message.obj = string;
                handler.sendMessage(message);
            } catch (IOException e) {
                //连接已经断开
                Message message = new Message();
                message.what = MSG_DIS_CONNECT;
                handler.sendMessage(message);
                e.printStackTrace();
            }
        }
    }

}
