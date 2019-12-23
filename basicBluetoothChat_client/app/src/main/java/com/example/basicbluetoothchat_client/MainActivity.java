package com.example.basicbluetoothchat_client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main-Activity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_COARSE_LOCATION_PERMISSION = 2;
    BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;
    private TextView textView; String appendedString ="";
    private String deviceNameToSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceNameToSearch = getString(R.string.Robochotu_face);

        textView = findViewById(R.id.textView);
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
            connectToBluetoothDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(TAG, "Bluetooth enabled");
            connectToBluetoothDevice();
        }
        if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Unable to start Bluetooth");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void connectToBluetoothDevice() {

        Log.d(TAG, bluetoothAdapter.getName());
        deviceOldName = bluetoothAdapter.getName();
        bluetoothAdapter.setName(getString(R.string.Robochotu_remote));

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BT_BroadcastReceiver, intentFilter);

        ArrayList<BluetoothDevice> pairedDevices = lookAtPairedDevices(deviceNameToSearch);

        if (pairedDevices != null) {
            Log.d(TAG, "Found some paired devices with the name you inserted:");
            for (int i = 0; i < pairedDevices.size(); i++) {
                String deviceName = pairedDevices.get(i).getName();
                String deviceHardwareAddress = pairedDevices.get(i).getAddress();
                Log.d(TAG, deviceName + "; " + deviceHardwareAddress);
                appendedString = appendedString + deviceName + "; " +deviceHardwareAddress + "\n";
            }
            textView.setText(appendedString);
        } else {
            Log.d(TAG, "Gonna have to look for devices.");
            checkPermissionsAndLocateDevices();
        }
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
                case BluetoothDevice.ACTION_FOUND:
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(TAG, "Device found: " + deviceName + "; " + deviceHardwareAddress);
                    appendedString = appendedString + deviceName + "; " +deviceHardwareAddress + "\n";
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "Discovery started");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "Discovery finished");
                    textView.setText(appendedString);
                    break;
            }
        }
    };

    private ArrayList<BluetoothDevice> lookAtPairedDevices(String nameToSearch) {

        Log.d(TAG, "Currently paired devices:");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {

            ArrayList<BluetoothDevice> pairedDevicesWithName = new ArrayList<>();

            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName + "; " + deviceHardwareAddress);
                if (deviceName.contains(nameToSearch)) {
                    pairedDevicesWithName.add(device);
                }
            }
            if (pairedDevicesWithName.size() > 1) {
                return pairedDevicesWithName;
            } else {
                return null;
            }

        }
        return null;
    }

    private void checkPermissionsAndLocateDevices() {
        Log.d(TAG, "Going to start looking for devices");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Looks like we don't have permissions");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.d(TAG, "Permission request was previously rejected, asking for permission again with an explanation of why it is needed.");
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Permissions previously rejected");
                alertDialog.setMessage("We need access to certain permissions to find the Robochotu face device. Please click ACCEPT on the dialog that appears after this.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION_PERMISSION);
                            }
                        });
                alertDialog.show();
            } else {
                Log.d(TAG, "Asking for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION_PERMISSION);
            }
        } else {
            Log.d(TAG, "We have permissions");
            beginDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_COARSE_LOCATION_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission was granted, yay!");
                    beginDiscovery();
                } else {
                    Log.d(TAG, "permission denied, boo!");
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void beginDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "Already discovering. Cancelling.");
            Log.d(TAG, "Cancel discovery: " + bluetoothAdapter.cancelDiscovery());
        }
        Log.d(TAG, "Start discovery: " + bluetoothAdapter.startDiscovery());
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
