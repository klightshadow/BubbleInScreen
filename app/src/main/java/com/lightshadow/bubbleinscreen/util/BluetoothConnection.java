package com.lightshadow.bubbleinscreen.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by lightshadow on 2014/12/18.
 */
public class BluetoothConnection extends Thread {

    private static final String NAME = "The Pet";
    private static final UUID uuid = UUID.fromString("e29f535e-e0c7-4af5-a01d-650a63318047");

    private final BluetoothServerSocket btServerSocket;

    public BluetoothConnection( BluetoothAdapter btAdapter) {
        BluetoothServerSocket tempServerSocket = null;
        try {
            tempServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        btServerSocket = tempServerSocket;
    }

    @Override
    public void run() {
        BluetoothSocket btSocket = null;
        while (true) {
            try {
                btSocket = btServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (btSocket != null) {
                // Do work to manage the connection (in a separate thread)
                //manageConnectedSocket(socket);
                try {
                    btServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void cancel() {
        try {
            btServerSocket.close();
        } catch (IOException e) {

        }
    }
}
