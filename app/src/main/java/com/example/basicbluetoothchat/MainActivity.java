package com.example.basicbluetoothchat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main-Activity";
    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpBluetooth();
    }

    private void setUpBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            Log.d(TAG, "This device does not support Bluetooth");
        }
        else if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            connectToBluetoothDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(TAG, "Bluetooth enabled");
            connectToBluetoothDevice();
        }
        if (resultCode == RESULT_CANCELED){
            Log.d(TAG, "Unable to start Bluetooth");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToBluetoothDevice(){

        Log.d(TAG, bluetoothAdapter.getName());
        deviceOldName = bluetoothAdapter.getName();
        bluetoothAdapter.setName("Robochotu_remote");

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(BT_BroadcastReceiver, intentFilter);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        ArrayList<BluetoothDevice> pairedDevices = lookAtPairedDevices("TAHA");

        if(pairedDevices != null){
            Log.d(TAG, "Found some paired devices with the name you inserted:");
            for(int i = 0; i < pairedDevices.size(); i++){
                Log.d(TAG, pairedDevices.get(i).getName() + "; " + pairedDevices.get(i).getAddress());
            }
        }
        else{
            Log.d(TAG, "Gonna have to look for devices.");
        }
    }

    private BroadcastReceiver BT_BroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BT_BroadcastReceiver");
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth turned on!");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth turned off!");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth is turning on!");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth is turning off!");
                        break;
                }
            }
        }
    } ;

    private ArrayList<BluetoothDevice> lookAtPairedDevices(String nameToSearch){

        Log.d(TAG, "Currently paired devices:");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {

            ArrayList<BluetoothDevice> pairedDevicesWithName = new ArrayList<>();

            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName + "; " + deviceHardwareAddress);
                if(deviceName.contains(nameToSearch)){
                    pairedDevicesWithName.add(device);
                }
             }
            if(pairedDevicesWithName.size() > 1){
                return pairedDevicesWithName;
            }
            else{
                return null;
            }

            }
        return null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, bluetoothAdapter.getName());
        bluetoothAdapter.setName(deviceOldName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(BT_BroadcastReceiver);
    }
}
