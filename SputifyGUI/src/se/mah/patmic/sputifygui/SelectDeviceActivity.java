package se.mah.patmic.sputifygui;

import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This class is used to display a list for choosing a bluetooth device that is paried with the phone.
 * 
 * @author Patrik
 * 
 */
public class SelectDeviceActivity extends Activity {
	private static final int REQUEST_BT_ENABLE = 4;

	private static String TAG = "SelectDeviceActivity";

	// För att hämta adressen till den valda enheten
	public static String EXTRA_DEVICE_ADRESS = "se.mah.ad1107.device_adress";

	// Används för att listan
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ListView listView;
	private BluetoothService mBtService;
	

	/**
	 * This method is called when the activity is created.
	 * The list is loaded with items and we also try to enable bluetooth if it´s not
	 * already activited.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_device);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter != null) {
			if (!mBtAdapter.isEnabled()) {
				if (enableBt()) {
					initList();
				}
			} else {
				initList();
			}
		} else if (mBtAdapter == null) {
			Toast.makeText(this, "Device not supporting bluetooth", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(SelectDeviceActivity.this,
					PlaylistActivity.class);
			startActivity(intent);
		}
	}

	/**
	 * A listner for the list. The user chooses a item that is representing a bluetooth device object
	 * and the app will try to connect to that device.
	 */
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

			// Hämta Mac adressen för enhenten man har valt. Den är de sista 17
			// tecknena i TextViewn
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17); // Adressen är
																	// de sista
																	// 17
																	// teckena i
																	// strängen
			Log.d(TAG, info + "\n" + address);

			Intent intent = new Intent(SelectDeviceActivity.this,
					PlaylistActivity.class);
			
			mBtService = BluetoothService.getBluetoothService();
			mBtService.connect(mBtAdapter.getRemoteDevice(address));
			startActivity(intent);
		}
	};

	/**
	 * The method is used to enable bluetooth.
	 * @return
	 * 		If the method suceeded to enable bluetooth true otherwise false
	 */
	public boolean enableBt() {
		if (BluetoothAdapter.getDefaultAdapter() == null) {
			return false; // Ifall mobilen inte har bluetooth retunras false.
		}
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBtAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(mBtAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
		}
		return true;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BT_ENABLE && resultCode == RESULT_OK) {
			initList();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	/**
	 *  The method loads the list with items
	 */
	public void initList() {
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		listView = (ListView) findViewById(R.id.paired_device_list);
		listView.setAdapter(mPairedDevicesArrayAdapter);

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			// Lägg till Meddelende ifall det inte finns några parade enheter
		}
		listView.setOnItemClickListener(mDeviceClickListener);
	}

}
