package com.example.bluetoothtesting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main Activity";
    private BluetoothAdapter bluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 1;
    private final int COARSE_LOCATION_PERMISSION_REQUEST = 2;
    private String deviceOldName;
    private String MAC_ADDRESS = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpBluetooth();

        if( bluetoothAdapter!=null ) {

            registerReceiver(mBTStateChangeBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

//            MAC_ADDRESS = lookAtPairedDevices();
            lookAtPairedDevices();
            Log.d(TAG, "Paired device: " + MAC_ADDRESS);

            if (MAC_ADDRESS.isEmpty()) {

                Log.d(TAG, "Going to start looking for devices");
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

                registerReceiver(mBTDeviceFoundBroadcastReceiver, intentFilter);

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                        alertDialog.setTitle("Alert");
                        alertDialog.setMessage("Alert message to be shown");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_REQUEST);
                                    }
                                });
                        alertDialog.show();

                    } else {
                        // No explanation needed; request the permission
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_REQUEST);
                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    // Permission has already been granted
                    if (bluetoothAdapter.isDiscovering())
                        bluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Start discovery: " + bluetoothAdapter.startDiscovery());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case COARSE_LOCATION_PERMISSION_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the task you need to do.
                    if (bluetoothAdapter.isDiscovering())
                        bluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Start discovery: " + bluetoothAdapter.startDiscovery());
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions denied. Cannot look for face device.", Toast.LENGTH_SHORT).show();
                }
                return;
        }

        // other 'case' lines to check for other
        // permissions this app might request.

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setUpBluetooth(){

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Log.d(TAG, bluetoothAdapter.getName());
        deviceOldName = bluetoothAdapter.getName();
        bluetoothAdapter.setName("Robochotu_remote");
    }

    private String lookAtPairedDevices(){

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceName: " + deviceName + ", device MAC Address: " + deviceHardwareAddress);
//                if(deviceName.equals("HC-05")){
//                    return device.getAddress();
//                }
            }
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
        }
        if (resultCode == RESULT_CANCELED)
            Toast.makeText(this, "Unable to enable Bluetooth", Toast.LENGTH_SHORT).show();

        super.onActivityResult(requestCode, resultCode, data);
    }

    private BroadcastReceiver mBTDeviceFoundBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "mBTDeviceFoundBroadcastReceiver");

            String action = intent.getAction();

            switch (action){
                case BluetoothDevice.ACTION_FOUND:
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(TAG, deviceName);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "Discovery started");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "Discovery finished");
                    break;
            }
        }
    };

    private BroadcastReceiver mBTStateChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "mBTStateChangeBroadcastReceiver");


            String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {

                    case BluetoothAdapter.STATE_OFF:
//                        Toast.makeText(context, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
//                        Toast.makeText(context, "Bluetooth turning off ...", Toast.LENGTH_SHORT).show();
                        break;

                    case BluetoothAdapter.STATE_ON:
//                        Toast.makeText(context, "Bluetooth turning on ...", Toast.LENGTH_SHORT).show();
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
//                        Toast.makeText(context, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                        break;
                }

            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if( bluetoothAdapter!=null ){
            Log.d(TAG, bluetoothAdapter.getName());
            bluetoothAdapter.setName(deviceOldName);
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if( bluetoothAdapter!=null ){
            bluetoothAdapter.cancelDiscovery();
            unregisterReceiver(mBTDeviceFoundBroadcastReceiver);
            unregisterReceiver(mBTStateChangeBroadcastReceiver);
        }
    }
}
