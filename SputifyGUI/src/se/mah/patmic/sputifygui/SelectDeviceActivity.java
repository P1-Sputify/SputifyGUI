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
import android.widget.AdapterView.OnItemClickListener;

/**
 * Klassen används för att välja ett item i en lista. I detta fall ska man v�lja
 * en bluetooth enhet och skicka tillbaka adressen.
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
	 * Anropas när aktiviten skapas. Skapar listan och lägger till items i
	 * listan.
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
			// TODO Something
		}
	}

	/**
	 * En lysnnare för vad som ska hända när man klickar på ett item i listan.
	 * Skickar tillbaka en sträng som innehåller en adress till en bluetooth
	 * device till den aktiviten som startade denna.
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

			// Skapar en intent så att man kan skicka tillbaka data
			Intent intent = new Intent(SelectDeviceActivity.this,
					PlaylistActivity.class);
			intent.putExtra(EXTRA_DEVICE_ADRESS, address);
			Log.d(TAG,
					"Data in intent"
							+ intent.getExtras().getString(EXTRA_DEVICE_ADRESS));
			
			mBtService = BluetoothService.getBluetoothService();
			mBtService.connect(mBtAdapter.getRemoteDevice(address));
			startActivity(intent);
		}
	};

	/**
	 * Metoden används för att kontrollera om mobilen har bluetooth och ifall
	 * den är aktivierad. Den kan även aktivera bluetooth om inte så är fallet
	 * 
	 * @return En boolean som är false om mobilen inte har bluetooth annars true
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
