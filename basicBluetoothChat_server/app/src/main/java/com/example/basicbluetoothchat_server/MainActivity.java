package com.example.basicbluetoothchat_server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_DISCOVERABILITY_ENABLE = 2;
    private static final int TIME_DURATION_FOR_DISCOVERABILITY = 300;
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
        bluetoothAdapter.setName(getString(R.string.Robochotu_face));

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, TIME_DURATION_FOR_DISCOVERABILITY); //turns on for 300 seconds
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
                    lookForIncomingConnections();
                }
                else if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Unable to begin discoverability");
                }
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
            BluetoothSocket socket = null;
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
                    manageMyConnectedSocket(socket);
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

    public void manageMyConnectedSocket(BluetoothSocket socket){

        final int MESSAGE_READ = 0;
        final int MESSAGE_WRITE = 1;
        final int MESSAGE_TOAST = 2;

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {

                switch (msg.what){
                    case MESSAGE_READ:
                        Log.d(TAG, "MESSAGE_READ");
                        break;
                    case MESSAGE_WRITE:
                        Log.d(TAG, "MESSAGE_WRITE");
                        break;
                    case MESSAGE_TOAST:
                        Log.d(TAG, "MESSAGE_TOAST");
                        break;
                }
                super.handleMessage(msg);
            }
        };

        MyBluetoothService myBluetoothService = new MyBluetoothService(socket, handler);

    }

}
