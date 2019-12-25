package com.example.basicbluetoothchat_enhancedserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_DISCOVERABILITY_ENABLE = 2;
    private static final int REQUEST_COARSE_LOCATION_PERMISSION = 3;
    private static final int TIME_DURATION_FOR_DISCOVERABILITY = 90;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;
    private String deviceNameComponentToSearch;

    private TextView textView;
    private EditText editText;
    private Button sendButton, searchBTButton;

    private ManageMyConnectedSocket manageMyConnectedSocket;
    private boolean connectedToBluetoothDevice = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.sendButton);
        searchBTButton = findViewById(R.id.searchBTButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();

                Log.d(TAG, "Edittext message: " + message);


                if(!message.isEmpty() && connectedToBluetoothDevice){
                    manageMyConnectedSocket.sendMessageToBluetooth(message);
                }
            }
        });

        searchBTButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBluetoothDevice();
            }
        });
        searchBTButton.setVisibility(View.INVISIBLE);

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
        bluetoothAdapter.setName(getString(R.string.Server_BT_Chat));

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, TIME_DURATION_FOR_DISCOVERABILITY); //turns on for 300 seconds
        startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABILITY_ENABLE);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(BT_BroadcastReceiver, intentFilter);

        searchBTButton.setVisibility(View.VISIBLE);
    }


    private void connectToBluetoothDevice() {
        deviceNameComponentToSearch = getString(R.string.Server_BT_Chat);
        lookAtPairedDevices(deviceNameComponentToSearch);
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


    private BroadcastReceiver BT_BroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "BT_BroadcastReceiver");
            String action = intent.getAction();

            ArrayList<BluetoothDevice> potentialFaceDevices = new ArrayList<>();

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
                            Toast.makeText(context, "The device is in discoverable mode", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "The device is in discoverable mode.");
                            break;
                        case (BluetoothAdapter.SCAN_MODE_CONNECTABLE):
                            Toast.makeText(context, "The device isn't in discoverable mode but can still receive connections", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "The device isn't in discoverable mode but can still receive connections.");
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            Log.d(TAG, "The device isn't in discoverable mode and cannot receive connections.");
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d(TAG, "Bluetooth device connected");
                    Toast.makeText(context, "Bluetooth device connected", Toast.LENGTH_SHORT).show();
                    connectedToBluetoothDevice = true;
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Bluetooth device disconnected");
                    Toast.makeText(context, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                    connectedToBluetoothDevice = false;
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult, requestCode = " + requestCode);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Bluetooth enabled");
                    turnOnDiscoverability();
                }
                else if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Unable to start Bluetooth");
                }
                break;

            case REQUEST_BT_DISCOVERABILITY_ENABLE:
                if (resultCode == TIME_DURATION_FOR_DISCOVERABILITY) {
                    Log.d(TAG, "Discoverability enabled");
                }
                else if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Unable to begin discoverability");
                    Toast.makeText(this, "The device isn't in discoverable mode but can still receive connections", Toast.LENGTH_SHORT).show();
                }
                lookForIncomingConnections();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void lookForIncomingConnections(){
        AcceptThread acceptIncomingConnectionThread = new AcceptThread();
        acceptIncomingConnectionThread.start();
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

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            Log.d(TAG, "Creating a thread for accepting incoming connections");

            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                String NAME = getString(R.string.app_name);
                UUID MY_UUID = UUID.fromString(getString(R.string.UUID));
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                Log.d(TAG, "Trying to create a Bluetooth Server Socket");
            } catch (IOException e) {
                Log.d(TAG, "Socket's listen() method failed");
            }
            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket;
            Log.d(TAG, "Listening until exception occurs or a socket is returned...");
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.d(TAG, "Socket's accept() method failed");
                    break;
                }

                if (socket != null) {
                    Log.d(TAG, "A connection was accepted!");
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket = new ManageMyConnectedSocket(socket);
                    manageMyConnectedSocket.start();
                    try {
                        mmServerSocket.close();
                        Log.d(TAG, "Socket's close() method successful");
                    } catch (IOException e) {
                        Log.d(TAG, "Socket's close() method failed");
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            Log.d(TAG, "Closing the connect socket and causing the thread to finish.");
            try {
                mmServerSocket.close();
                Log.d(TAG, "Closed the connect socket");
            } catch (IOException e) {
                Log.d(TAG, "Could not close the connect socket");
            }
        }
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
            manageMyConnectedSocket = new ManageMyConnectedSocket(mmSocket);
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
                    switch (msg.what) {
                        case MESSAGE_READ:
                            byte[] readBuffer = (byte[]) msg.obj;
                            // construct a string from the valid bytes in the buffer
                            String readMessage = new String(readBuffer, 0, msg.arg1);
                            Log.d(TAG, "MESSAGE_READ");
                            Log.d(TAG, readMessage);
                            textView.setText(readMessage);
                            break;
                        case MESSAGE_WRITE:
                            byte[] writeBuffer = (byte[]) msg.obj;
                            String writeMessage = new String(writeBuffer);
                            Log.d(TAG, "MESSAGE_WRITE");
                            Log.d(TAG, writeMessage);
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
