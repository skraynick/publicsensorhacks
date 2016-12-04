package sensornetworks.com.sensornetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.FloatProperty;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import broadcastReceivers.WiFiDirectBroadcastReceiver;
import utils.ClientSocketHandler;
import utils.GroupOwnerSocketHandler;
import sensornetworks.com.sensornetwork.WiFiDirectSensorFragment.MessageTarget;

import static sensornetworks.com.sensornetwork.DeviceDetailFragment.progressDialog;

public class WiFiDirectActivity extends AppCompatActivity implements MessageTarget, SensorEventListener, DeviceListFragment.DeviceActionListener, WifiP2pManager.ConnectionInfoListener {

    private SensorManager mSensorManager;

    public static final String TAG = "wifidirect";

    // and we will populate this list
    private List<Sensor> deviceSensors;
    private Sensor mLight;

    WifiP2pManager.Channel mChannel;

    private final IntentFilter intentFilter = new IntentFilter();

    WifiP2pManager mManager;

    private BroadcastReceiver receiver = null;

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    public static final int SERVER_PORT = 4545;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // we need to retrieve the system service on the parent activity
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // this is how to list all sensors
        deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        // TODO list the information here
        // get the listview from our layout
        ListView listView = (ListView) this.findViewById(R.id.list_sensors);

        // and populate it with the most basic view available for listviews, a single text view
        // only made final so we can refer to it in our anonymuous innerclass for clickListener impl
        final ArrayAdapter<Sensor> listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, deviceSensors.toArray(new Sensor[deviceSensors.size()]));
        // set it to our listview
        listView.setAdapter(listAdapter);

        p2pIntentFiltersSetup();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        stringFromJNI();

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
            }
        });

    }

    private void p2pIntentFiltersSetup() {
        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.atn_direct_enable:
                if (mManager != null && mChannel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
            case R.id.atn_direct_discover:

                /*if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }*/
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            case (android.R.id.home):

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        float lux = event.values[0];
        TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        //Log.i("SENSORS", Float.toString(lux));
        //tv.setText(Float.toString(lux));
        // Do something with this sensor value.
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        receiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(receiver);
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                //fragment.getView().setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        findViewById(R.id.frag_list).setVisibility(View.INVISIBLE);
        fragment.showDetails(device);
    }

    @Override
    public void cancelDisconnect() {

    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo p2pInfo) {
        Log.i(TAG, "Connection info " + p2pInfo);

        Thread handler = null;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
                        ((MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    ((MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }
        /*chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container_root, chatFragment).commit();
        statusTxtView.setVisibility(View.GONE);*/

    }


}
