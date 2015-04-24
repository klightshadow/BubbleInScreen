package com.lightshadow.bubbleinscreen.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by lightshadow on 2014/12/18.
 */
public class AcceptThread extends Thread {

    private static final String TAG = "AcceptThread";
    private static final boolean D = true;
    private static final String NAME = "The Pet";
    private static final UUID MY_UUID = UUID.fromString("e29f535e-e0c7-4af5-a01d-650a63318047");
    
    private final BluetoothServerSocket btServerSocket;
    public AcceptThread(BluetoothAdapter mAdapter) {
        BluetoothServerSocket tmp = null;
    // Create a new listening server socket
    try {
        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
    } catch (IOException e) {
        Log.e(TAG, "listen() failed", e);
    }
    btServerSocket = tmp;
}
    public void run() {
        if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
        setName("AcceptThread");
        BluetoothSocket socket = null;
        // Listen to the server socket if we're not connected
        while (true) { //mState != STATE_CONNECTED
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket = btServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "accept() failed", e);
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                /*synchronized (BluetoothChatService.this) {
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }*/
            }
        }
        if (D) Log.i(TAG, "END mAcceptThread");
    }
    public void cancel() {
        if (D) Log.d(TAG, "cancel " + this);
        try {
            btServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of server failed", e);
        }
    }
}
