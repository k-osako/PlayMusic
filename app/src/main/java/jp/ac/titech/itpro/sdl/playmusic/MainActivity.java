package jp.ac.titech.itpro.sdl.playmusic;

import android.Manifest;
import android.app.Activity;
import android.os.Handler;
//import android.bluetooth.BluetoothServerSocket;
import android.content.IntentFilter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.media.AudioManager;
import android.os.Message;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private final static String TAG = "MainActivity";
    private MediaPlayer mp = null;

    //bluetooth
    private BluetoothAdapter bluetoothadapter;
    private BluetoothServer bluetoothserver;

    private final Handler handler = new Handler() {
        // ハンドルメッセージ
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothServer.MESSAGE_STATECHANGE:
                    switch (msg.arg1) {
                        case BluetoothServer.STATE_CONNECTED:
                            // addText("接続完了");
                            break;
                        case BluetoothServer.STATE_CONNECTING:
                            // addText("接続中");
                            break;
                        case BluetoothServer.STATE_NONE:
                            // addText("未接続");
                            break;
                    }
                    break;
                case BluetoothServer.MESSAGE_READ:
                    // byte[] readBuf = (byte[]) msg.obj;
                    // addText(new String(readBuf, 0, msg.arg1));
                    break;
            }
        }
    };

    // オプションメニューID
    private static final int MENUID_SEARCH = 0;
    // リクエスト定数
    public static final int REQUEST_CONNECTDEVICE = 1;
    private final static int REQCODE_ENABLE_BT = 1111;
    private final static int REQCODE_PERMISSIONS = 2222;
    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //TextView textView = (TextView) findViewById(R.id.text_View);
        mp = MediaPlayer.create(this, R.raw.torisetsu);

        bluetoothadapter = BluetoothAdapter.getDefaultAdapter();

        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(broadcastReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        playbutton = (Button)findViewById(R.id.play_button);
        playbutton.setOnClickListener(this);

        stopbutton = (Button)findViewById(R.id.stop_button);
        stopbutton.setOnClickListener(this);

        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothadapter == null) {
            Toast.makeText(this, "toast_bt_is_not_available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setupBT();
    }

    private Button playbutton;
    private Button stopbutton;
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action == null) return;

            switch (action){
                case Intent.ACTION_HEADSET_PLUG:
                    int state = intent.getIntExtra("state", -1);
                    if(state == 0) {//イヤホンが外された
                    }else if(state>0) {//イヤホンが装着された
                        Log.e(TAG, "Intent.action_headset_plug");
                    }
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    Log.e(TAG, "Audiomanager.action_audio_becoming_noisy");
                    break;
                default:
                    break;
            }
        }
    };

    public void onClick(View v){
        if(v==playbutton){
            if(mp!=null) mp.start();
        }
        else if(v==stopbutton){
            if(mp!=null) mp.pause();
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG, "onSTART");
        if (false == bluetoothadapter.isEnabled()) {
            // Bluetoothが無効になっているので、有効にする要求を発生さえる。
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQCODE_ENABLE_BT);
        }
        else {
            if (null == bluetoothserver) {
                // BluetoothServer作成
                bluetoothserver = new BluetoothServer(this, handler);
            }
        }
    }

    // アクティビティ破棄時
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != bluetoothserver) {
            bluetoothserver.stop();
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
//        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d(TAG, "onActivityResult"+reqCode);
        switch (reqCode) {
            case REQCODE_ENABLE_BT:
                if (resCode != Activity.RESULT_OK) {
                    Log.d(TAG, "reqcode_enable_bt");
                    Toast.makeText(this, "R.string.toast_bt_must_be_enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }break;
            case REQUEST_CONNECTDEVICE: // 端末検索
                Log.d(TAG, "request_connectdevice");
                //if (Activity.RESULT_OK == reqCode) {
                    Log.d(TAG, "address is");
                    String address = data.getExtras().getString(DeviceListActivity.EXTRANAME_DEVICEADDRESS);
                    Log.d(TAG, "address is"+address);
                    // Bluetooth接続要求
                    BluetoothDevice device = bluetoothadapter.getRemoteDevice(address);
                    if(device==null)  Log.d(TAG, "device is null");
                    else  Log.d(TAG, "device is not null");
                    bluetoothserver.connect(device);
                //}
                break;
            default:
                Log.d(TAG, "default");
        }
        Log.d(TAG, "exit of switch");
        super.onActivityResult(reqCode, resCode, data);
    }

    private void setupBT(){
        Log.d(TAG, "setupBT");
        if(!bluetoothadapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQCODE_ENABLE_BT);
    }

    // オプションメニュー生成時
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item0 = menu.add(0, MENUID_SEARCH, 0, "端末検索");
        item0.setIcon(android.R.drawable.ic_search_category_default);
        return true;
    }

    // オプションメニュー選択時
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENUID_SEARCH:
                Intent devicelistactivityIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(devicelistactivityIntent, REQUEST_CONNECTDEVICE);
                return true;
        }
        return false;
    }
}
