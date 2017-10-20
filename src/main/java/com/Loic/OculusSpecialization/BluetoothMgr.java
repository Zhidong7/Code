package com.Loic.OculusSpecialization;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by 胡敏浩 on 2017/10/14.
 */
public class BluetoothMgr {
    private int ENABLE_BLUETOOTH=2;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket = null;
    private String bluetoothAddress="98:D3:32:20:C9:D4";//蓝牙模块mac地址
    public static TextView mtext = null;

    public static UUID getMyUuidSecure() {
        return MY_UUID_SECURE;
    }

    private static final UUID MY_UUID_SECURE=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int UPDATE_TEXT=1;
    private Handler blueToothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TEXT:
                    mtext.setText("方向："+msg.obj);
                    //angel=""+msg.obj;
                    break;
                default:
                    break;
            }
        }
    };

    void init(){
        mtext.setText("ready");
    }

    void fresh(){
        ConnectedThread mThread = new ConnectedThread(bluetoothSocket);
        mThread.start();
    }

    private class ConnectedThread extends Thread{
        InputStream inputStream = null;
        OutputStream outputStream = null;

        public ConnectedThread(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tmpin = null;
            OutputStream tmpout = null;
            try{
                tmpin= socket.getInputStream();
                tmpout = socket.getOutputStream();
            }catch (IOException e) {

            }
            inputStream = tmpin;
            outputStream = tmpout;
        }

        public void run (){
            byte[] buffer = new byte[1];
            int bytes;
            Log.d("run the thread","im ok");
            while (true)
            {
                Log.d("run in the while","fine");
                try{
                    bytes = inputStream.read(buffer);

                    String buffer_str = BinaryToHexString(buffer);
                    blueToothHandler.obtainMessage(UPDATE_TEXT,bytes,-1,buffer_str).sendToTarget();
                    Log.d("fangxiang",""+bytes);
                }catch (IOException e){
                    Log.d("run in the while","i didn't get it");
                    break;
                }
            }
        }

        public String BinaryToHexString(byte[] bytes){

            String hexStr =  "0123456789ABCDEF";
            String result = "";
            String hex = "";
            for(int i=0;i<bytes.length;i++){
                //字节高4位
                hex = String.valueOf(hexStr.charAt((bytes[i]&0xF0)>>4));
                //字节低4位
                hex += String.valueOf(hexStr.charAt(bytes[i]&0x0F));
                result +=hex+" ";  //这里可以去掉空格，或者添加0x标识符。
            }
            return result;
        }
        //public void write
    }

    public int getENABLE_BLUETOOTH() {
        return ENABLE_BLUETOOTH;
    }

    public void setENABLE_BLUETOOTH(int ENABLE_BLUETOOTH) {
        this.ENABLE_BLUETOOTH = ENABLE_BLUETOOTH;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }
}
