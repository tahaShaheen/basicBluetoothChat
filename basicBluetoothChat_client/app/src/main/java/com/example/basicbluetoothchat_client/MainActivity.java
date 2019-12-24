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
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_COARSE_LOCATION_PERMISSION = 2;
    BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;
    private String deviceNameComponentToSearch;

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
        bluetoothAdapter.setName(getString(R.string.ROBOCHOTU_REMOTE));

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BT_BroadcastReceiver, intentFilter);

        deviceNameComponentToSearch = getString(R.string.ROBOCHOTU_FACE);

        lookAtPairedDevices(deviceNameComponentToSearch);
    }

    private BroadcastReceiver BT_BroadcastReceiver = new BroadcastReceiver() {

        ArrayList<BluetoothDevice> potentialFaceDevices = new ArrayList<>();

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
                    if(deviceName == null || deviceName.contains(deviceNameComponentToSearch)){
                        Log.d(TAG, "Found a device with name "+ deviceNameComponentToSearch);
                        potentialFaceDevices.add(device);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Log.d(TAG, "Discovery started");
                    potentialFaceDevices.clear();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(TAG, "Discovery finished");
                    lookAtDevicesFound(potentialFaceDevices, false);
                    break;
            }
        }
    };


    private void lookAtDevicesFound(final ArrayList<BluetoothDevice> devicesToDisplay, boolean deviceListIsFromPairedDevices){
        if(devicesToDisplay.isEmpty()){
            Log.d(TAG, "No devices with name " + deviceNameComponentToSearch + " found.");
                // setup the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("No " + ((deviceListIsFromPairedDevices) ? "paired" : "nearby") + " devices found");
            if ((deviceListIsFromPairedDevices)) {
                builder.setMessage("Would you like to look at nearby devices?");
            } else {
                builder.setMessage("Would you like to continue to look?");
            }
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkPermissionsAndLocateDevices();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
        }
        else {
            final ArrayList<String> listOfNames = new ArrayList<>();
            final ArrayList<String> listOfMACAddresses = new ArrayList<>();
            for (int i =0; i < devicesToDisplay.size(); i++){

                if(devicesToDisplay.get(i).getName() == null){
                    listOfNames.add(getString(R.string.NULL_NAME_ERROR));
                }
                else{
                    listOfNames.add(devicesToDisplay.get(i).getName());
                }

                listOfMACAddresses.add(devicesToDisplay.get(i).getAddress());
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if ((deviceListIsFromPairedDevices)) {
                builder.setTitle("Multiple devices paired");
            } else {
                builder.setTitle("Multiple devices found");
            }

            builder.setItems(listOfNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int deviceSelected) {
                    Log.d(TAG, deviceSelected + " was clicked. It has name " + listOfNames.get(deviceSelected) + " and MAC Address "+ listOfMACAddresses.get(deviceSelected));
                    ConnectThread connectThread = new ConnectThread(devicesToDisplay.get(deviceSelected));
                    connectThread.start();
                }
            });

            builder.setPositiveButton(((deviceListIsFromPairedDevices) ? "Look for devices nearby?" : "Look again?") , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    checkPermissionsAndLocateDevices();
                }
            });
            builder.setNegativeButton("Stop looking", null);
            builder.show();
        }
    }

    private void lookAtPairedDevices(String nameToSearch) {

        Log.d(TAG, "Currently paired devices:");
        ArrayList<BluetoothDevice> pairedDevicesWithName = new ArrayList<>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (!pairedDevices.isEmpty()) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName + "; " + deviceHardwareAddress);

                if (deviceName.contains(nameToSearch)) {
                    pairedDevicesWithName.add(device);
                }
            }
        }

        if (!pairedDevicesWithName.isEmpty()) {
            Log.d(TAG, "Found some paired devices with the name you inserted:");
            for (int i = 0; i < pairedDevicesWithName.size(); i++) {
                String deviceName = pairedDevicesWithName.get(i).getName();
                String deviceHardwareAddress = pairedDevicesWithName.get(i).getAddress();
                Log.d(TAG, deviceName + "; " + deviceHardwareAddress);
            }
        }

        lookAtDevicesFound(pairedDevicesWithName, true);
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


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "Creating a thread for trying to connect to server devices");
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                UUID MY_UUID = UUID.fromString(getString(R.string.UUID));
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(TAG, "Trying to create a Bluetooth Client Socket");
            } catch (IOException e) {
                Log.d(TAG, "Socket's create() method failed");
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {

            Log.d(TAG, "Cancel discovery because it otherwise slows down the connection");
            Log.d(TAG, "cancel discovery: " + bluetoothAdapter.cancelDiscovery());

            try {
                Log.d(TAG, "Attempting connection...");
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.d(TAG, "Unable to connect; closing the socket and returning");
                try {
                    mmSocket.close();
                    Log.d(TAG, "Socket's close() method successful");
                } catch (IOException closeException) {
                    Log.d(TAG, "Socket's close() method failed");
                }
                return;
            }

            Log.d(TAG, "Connection attempt successful!");
            ManageMyConnectedSocket manageMyConnectedSocket = new ManageMyConnectedSocket(mmSocket);
            manageMyConnectedSocket.start();
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            Log.d(TAG, "Closing the connect socket and causing the thread to finish.");
            try {
                mmSocket.close();
                Log.d(TAG, "Closed the client socket");
            } catch (IOException e) {
                Log.d(TAG, "Could not close the client socket");
            }
        }
    }

    private class ManageMyConnectedSocket extends Thread{

        private BluetoothSocket mmSocket;
        private MyBluetoothService myBluetoothService;

        public ManageMyConnectedSocket(BluetoothSocket socket) {
            mmSocket = socket;

            final int MESSAGE_READ = 0;
            final int MESSAGE_WRITE = 1;
            final int MESSAGE_TOAST = 2;

            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    byte[] bufferBytes = (byte[]) msg.obj;
                    String bufferString = "";
                    try {
                        bufferString = new String(bufferBytes, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, "Unable to convert bytes to String");
                        e.printStackTrace();
                    }
                    switch (msg.what) {
                        case MESSAGE_READ:
                            Log.d(TAG, "MESSAGE_READ");
                            Log.d(TAG, bufferString);
                            break;
                        case MESSAGE_WRITE:
                            Log.d(TAG, "MESSAGE_WRITE");
                            Log.d(TAG, bufferString);
                            break;
                        case MESSAGE_TOAST:
                            Log.d(TAG, "MESSAGE_TOAST");
                            break;
                    }
                    super.handleMessage(msg);
                }
            };

            myBluetoothService = new MyBluetoothService(mmSocket, handler);
        }

        public void sendMessageToBluetooth(String messageToSend){
            myBluetoothService.write(messageToSend);
        }
    }
}
