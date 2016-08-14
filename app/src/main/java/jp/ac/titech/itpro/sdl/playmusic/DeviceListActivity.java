package jp.ac.titech.itpro.sdl.playmusic;

/**
 * Created by kayo on 2016/07/15.
 */
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity implements AdapterView.OnItemClickListener {

    private final static int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
    private final static int FP = LinearLayout.LayoutParams.FILL_PARENT;

    public static String EXTRANAME_DEVICEADDRESS = "deviceaddress";

    private ArrayAdapter<String> arrayadapterstringDevice;
    private BluetoothAdapter bluetoothadapter;

    // ブロードキャストレシーバー
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Bluetooth端末発見
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.BOND_BONDED != device.getBondState()) {
                    arrayadapterstringDevice.add(device.getName() + "\n" + device.getAddress());
                }
            }
            // Bluetooth端末検索終了
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                android.util.Log.e("", "Bluetooth端末検索完了");
            }
        }
    };

    // アクティビティ起動時
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // タイトルなし
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 戻り値の初期化
        setResult(Activity.RESULT_CANCELED);

        // レイアウトの作成
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);

        // デバイス配列
        arrayadapterstringDevice = new ArrayAdapter<String>(this, R.layout.rowdata);

        // リストビュー
        ListView listView = new ListView(this);
        listView.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
        listView.setAdapter(arrayadapterstringDevice);
        layout.addView(listView);
        listView.setOnItemClickListener(this);

        // ブロードキャストレシーバー
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        // Bluetooth端末のリストアップ
        bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> boundeddevices = bluetoothadapter.getBondedDevices();
        if (0 < boundeddevices.size()) {
            for (BluetoothDevice device : boundeddevices) {
                arrayadapterstringDevice.add(device.getName() + "\n" + device.getAddress());
            }
        }
        if (bluetoothadapter.isDiscovering()) {
            bluetoothadapter.cancelDiscovery();
        }
        bluetoothadapter.startDiscovery();
    }

    // アクティビティ破棄時
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != bluetoothadapter) {
            bluetoothadapter.cancelDiscovery();
        }
        unregisterReceiver(receiver);
    }

    // クリック時
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
        // Bluetooth端末の検索のキャンセル
        bluetoothadapter.cancelDiscovery();

        // 戻り値の設定
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);
        Intent intent = new Intent();
        //Log.d(TAG, "address="+ address );
        intent.putExtra(EXTRANAME_DEVICEADDRESS, address);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
