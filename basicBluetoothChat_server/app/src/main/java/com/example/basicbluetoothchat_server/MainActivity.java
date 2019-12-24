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
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_DISCOVERABILITY_ENABLE = 2;
    private static final int TIME_DURATION_FOR_DISCOVERABILITY = 300;
    BluetoothAdapter bluetoothAdapter;
    private String deviceOldName;

    private TextView textView;
    private EditText editText;
    private Button button;

    private ManageMyConnectedSocket manageMyConnectedSocket;
    private boolean connectedToBluetoothDevice = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);
        button = findViewById(R.id.sendButton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();

                Log.d(TAG, "Edittext message: " + message);


                if(!message.isEmpty() && connectedToBluetoothDevice){
                    manageMyConnectedSocket.sendMessageToBluetooth(message);
                }
            }
        });

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
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
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
