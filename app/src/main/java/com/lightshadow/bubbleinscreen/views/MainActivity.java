package com.lightshadow.bubbleinscreen.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListPopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.lightshadow.bubbleinscreen.R;
import com.lightshadow.bubbleinscreen.adapters.DevicesAdapter;
import com.lightshadow.bubbleinscreen.services.BluetoothChatService;
import com.lightshadow.bubbleinscreen.services.BubbleInScreen;
import com.lightshadow.bubbleinscreen.util.BluetoothConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener {

    private static final boolean D = true;
    private static final String TAG = "MainActivity";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connetDevice;
    private Switch bluetoothSwitch;
    private Button serviceStart, showDevices, showPairDevice, messageSend;
    private SharedPreferences spf;
    private DevicesAdapter deviceAdapter;
    private ListPopupWindow listPopup;
    private BluetoothChatService btChatServer;
    private TextView messageShow;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    private static final String BLUETOOTH = "bluetooth";
    private static final UUID THE_UUID = UUID.fromString("e29f535e-e0c7-4af5-a01d-650a63318047");
    private static Boolean isRegister = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothSwitch = (Switch)findViewById(R.id.sw_blueTooth);
        showDevices = (Button)findViewById(R.id.btn_showDevices);
        showPairDevice = (Button)findViewById(R.id.btn_showPairDevice);
        serviceStart = (Button)findViewById(R.id.btn_serviceStart);

        messageSend = (Button)findViewById(R.id.btn_messageSend);
        messageShow = (TextView)findViewById(R.id.tv_messageShow);

        bluetoothSwitch.setOnCheckedChangeListener(this);
        listPopup = new ListPopupWindow(MainActivity.this);
        deviceAdapter = new DevicesAdapter(MainActivity.this);

        serviceStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if("Xiaomi".equals(Build.MANUFACTURER)) {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("小米使用者請開啟懸浮視窗設定");
                    builder.setPositiveButton("設定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri packageURI = Uri.parse("package:" + MainActivity.this.getPackageName());
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);
                        }
                    });
                    builder.setNeutralButton("已設定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startService(new Intent(MainActivity.this, BubbleInScreen.class));
                            finish();
                        }
                    });
                    builder.setNegativeButton("才不要咧~~", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, "好吧", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.show();
                } else {
                    startService(new Intent(MainActivity.this, BubbleInScreen.class));
                    finish();
                }

            }
        });

        showDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceAdapter.clearList();
                bluetoothAdapter.startDiscovery();

                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                MainActivity.this.registerReceiver(bluetoothReceiver, intentFilter);

                intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                MainActivity.this.registerReceiver(bluetoothReceiver, intentFilter);

                intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                MainActivity.this.registerReceiver(bluetoothReceiver, intentFilter);

                intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                MainActivity.this.registerReceiver(bluetoothReceiver, intentFilter);

                listPopup.setAdapter(deviceAdapter);
                listPopup.setAnchorView(showDevices);
                isRegister = true;
                listPopup.show();
            }
        });

        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                BluetoothDevice selectDevice = (BluetoothDevice)deviceAdapter.getItem(position);
                connetDevice = selectDevice;
                showPairDevice.setVisibility(View.VISIBLE);
                btChatServer.connect(selectDevice, true);
                /*try {
                    Method method = selectDevice.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(selectDevice, (Object[]) null);

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }*/
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, selectDevice.getAddress());
                setResult(Activity.RESULT_OK, intent);
                listPopup.dismiss();

            }
        });

        showPairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showPairDevice.setText("No paired");
                    Method method = connetDevice.getClass().getMethod("removeBond", (Class[]) null);
                    method.invoke(connetDevice, (Object[]) null);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        messageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String yo = "YO!";
                byte[] send = yo.getBytes();
                btChatServer.write(send);
            }
        });

    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(btDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    deviceAdapter.addList(btDevice);
                } else {
                    connetDevice = btDevice;
                    showPairDevice.setText(btDevice.getName() + "\n" + btDevice.getAddress());
                    showPairDevice.setVisibility(View.VISIBLE);
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                MainActivity.this.setSupportProgressBarIndeterminate(false);
                //setTitle("Select Devices");
                if(deviceAdapter.getCount() == 0) {
                    Toast.makeText(MainActivity.this, "No Device", Toast.LENGTH_SHORT).show();
                }
            } else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(1000);
                Toast.makeText(MainActivity.this, "Disconnect", Toast.LENGTH_SHORT).show();
            }

            //Log.e("action", action);
            //Log.e("intent", intent.toString());
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(MainActivity.this, "Paired", Toast.LENGTH_SHORT).show();
                    showPairDevice.setText(btDevice.getName() + "\n" + btDevice.getAddress());
                    showPairDevice.setVisibility(View.VISIBLE);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(MainActivity.this, "Unpaired", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
    }

    @Override
    protected void onDestroy() {
        if(isRegister){
            unregisterReceiver(bluetoothReceiver);
        }
        super.onDestroy();
    }

    private void loadPreferences() {
        spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean bluetooth =  spf.getBoolean(BLUETOOTH, false);
        if(bluetooth){
            bluetoothSwitch.setChecked(true);
            bluetoothAdapter.enable();
            showDevices.setVisibility(View.VISIBLE);
        } else {
            bluetoothSwitch.setChecked(false);
            bluetoothAdapter.disable();
            showDevices.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView == bluetoothSwitch) {
            if(bluetoothAdapter != null) {
                if(isChecked) {
                    savePreferences(BLUETOOTH, true);
                    bluetoothAdapter.enable();

                    btChatServer = new BluetoothChatService(this, handler);
                    /*Device discoverable*/
                    bluetoothDiscoverable();

                    Handler mHandler = new Handler();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btChatServer.start();
                        }
                    }, 5000);

                    /*Device start discover*/
                    showDevices.setVisibility(View.VISIBLE);

                } else {
                    btChatServer.stop();
                    savePreferences(BLUETOOTH, false);
                    bluetoothAdapter.disable();

                    showDevices.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(MainActivity.this, "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bluetoothDiscoverable() {
        Intent bluetoothDiscover = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivity(bluetoothDiscover);
    }
    private void savePreferences(String name, boolean status) {
        spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        spf.edit().putBoolean(name, status).commit();
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    messageShow.setText(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    break;
                case MESSAGE_TOAST:
                    break;
            }
        }
    };
}
