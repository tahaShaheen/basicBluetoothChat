package com.example.basicbluetoothchat_server;

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
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main-Activity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_DISCOVERABILITY_ENABLE = 2;
    BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpBluetooth();
    }

    private void setUpBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "This device does not support Bluetooth");
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            turnOnDiscoverability();
        }
    }

    private void turnOnDiscoverability() {
        Log.d(TAG, bluetoothAdapter.getName());
        deviceOldName = bluetoothAdapter.getName();
        bluetoothAdapter.setName("Robochotu_remote");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); //turns on for 300 seconds
        startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABILITY_ENABLE);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(BT_BroadcastReceiver, intentFilter);
    }

    private BroadcastReceiver BT_BroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BT_BroadcastReceiver");
            String action = intent.getAction();

            switch (action){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
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
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    switch(scanMode){
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            Log.d(TAG, "The device is in discoverable mode.");
                            break;
                        case (BluetoothAdapter.SCAN_MODE_CONNECTABLE):
                            Log.d(TAG, "The device isn't in discoverable mode but can still receive connections.");
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.d(TAG, "The device isn't in discoverable mode and cannot receive connections.");
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Bluetooth enabled");
                    turnOnDiscoverability();
                }
                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Unable to start Bluetooth");
                }
                break;

            case REQUEST_BT_DISCOVERABILITY_ENABLE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Discoverability enabled");
                }
                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Unable to begin discoverability");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, bluetoothAdapter.getName());
        bluetoothAdapter.setName(deviceOldName);
    }

}
