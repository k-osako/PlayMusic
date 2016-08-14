package jp.ac.titech.itpro.sdl.playmusic;



/**
 * Created by kayo on 2016/07/15.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

public class BluetoothServer {
    private final static String TAG = "BluetoothServer";
    public static final int MESSAGE_STATECHANGE = 1;
    public static final int MESSAGE_READ = 2;

    // private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private BluetoothAdapter bluetoothadapter;
    private Handler handler;
    private int state;



    // Bluetoothの接続要求
    private class ConnectThread extends Thread {
        private BluetoothDevice bluetoothdevice;
        private BluetoothSocket bluetoothsocket;

        // コンストラクタ
        public ConnectThread(BluetoothDevice bluetoothdevice) {
            try {
                this.bluetoothdevice = bluetoothdevice;
                bluetoothsocket = bluetoothdevice.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e) {
            }
        }

        // 処理
        public void run() {
            bluetoothadapter.cancelDiscovery();
            try {
                bluetoothsocket.connect();
            }
            catch (IOException e) {
                setState(STATE_NONE);
                try {
                    bluetoothsocket.close();
                }
                catch (IOException e2) {
                }
                return;
            }
            synchronized (BluetoothServer.this) {
                connectthread = null;
            }
            connected(bluetoothsocket, bluetoothdevice);
        }

        // キャンセル
        public void cancel() {
            try {
                bluetoothsocket.close();
            }
            catch (IOException e) {
            }
        }
    }

    // Bluetooth接続完了後の処理
    private class ConnectedThread extends Thread {
        private BluetoothSocket bluetoothsocket;

        // コンストラクタ
        public ConnectedThread(BluetoothSocket bluetoothsocket) {
            this.bluetoothsocket = bluetoothsocket;
        }

        // 処理
        public void run() {
            byte[] buf = new byte[1024];
            int bytes;
            while (true) {
                try {
                    InputStream input = bluetoothsocket.getInputStream();
                    bytes = input.read(buf);
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buf).sendToTarget();
                }
                catch (IOException e) {
                    setState(STATE_NONE);
                    break;
                }
            }
        }

        // 書き込み
        public void write(byte[] buf) {
            try {
                OutputStream output = bluetoothsocket.getOutputStream();
                output.write(buf);
            }
            catch (IOException e) {
            }
        }

        // キャンセル
        public void cancel() {
            try {
                bluetoothsocket.close();
            }
            catch (IOException e) {
            }
        }
    }

    private ConnectThread connectthread;
    private ConnectedThread connectedthread;

    // コンストラクタ
    public BluetoothServer(Context context, Handler handler) {
        this.bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        state = STATE_NONE;
    }

    // 状態の指定
    private synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MESSAGE_STATECHANGE, state, -1).sendToTarget();
    }

    // 状態の取得
    public synchronized int getState() {
        return state;
    }

    // Bluetoothの接続要求
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "state_connecting state"+state);
        Log.d(TAG, "state_connecting device"+device);
        if (STATE_CONNECTING == state) {
            Log.d(TAG, "state_connecting==state"+device);
            if (null != connectthread) {
                Log.d(TAG, "null!=connectthread");
                connectthread.cancel();
                connectthread = null;
            }
        }
        if (null != connectedthread) {
            Log.d(TAG, "null!=connectthread2");
            connectedthread.cancel();
            connectedthread = null;
        }
        connectthread = new ConnectThread(device);
        connectthread.run();
        Log.d(TAG, "state_connecting");
        setState(STATE_CONNECTING);
    }

    // Bluetooth接続完了後の処理
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (null != connectthread) {
            connectthread.cancel();
            connectthread = null;
        }
        if (null != connectedthread) {
            connectedthread.cancel();
            connectedthread = null;
        }
        connectedthread = new ConnectedThread(socket);
        connectedthread.start();
        setState(STATE_CONNECTED);
    }

    // Bluetoothの切断
    public synchronized void stop() {
        if (null != connectthread) {
            connectthread.cancel();
            connectthread = null;
        }
        if (null != connectedthread) {
            connectedthread.cancel();
            connectedthread = null;
        }
        setState(STATE_NONE);
    }

    // 書き込み
    public void write(byte[] out) {
        ConnectedThread connectedthread;
        synchronized (this) {
            if (STATE_CONNECTED != state) {
                return;
            }
            connectedthread = this.connectedthread;
        }
        connectedthread.write(out);
    }
}
