package utils;

/**
 * Created by sarahkraynick on 2016-12-04.
 */

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import sensornetworks.com.sensornetwork.WiFiDirectActivity;

public class ClientSocketHandler extends Thread {
    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private SensorDataManager data;
    private InetAddress mAddress;
    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }
    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    WiFiDirectActivity.SERVER_PORT), 5000);
            Log.d(TAG, "Launching the I/O handler");
            data = new SensorDataManager(socket, handler);
            new Thread(data).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }
    public SensorDataManager getSensorData() {
        return data;
    }
}