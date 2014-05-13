package se.mah.patmic.sputifygui;

import java.util.Set;

import android.app.Activity;
import android.app.ListActivity;
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
 * Klassen anv�nds f�r att v�lja ett item i en lista. I detta fall ska man v�lja
 * en bluetooth enhet och skicka tillbaka adressen.
 * 
 * @author Patrik
 * 
 */
public class SelectDeviceActivity extends Activity {
	private static String TAG = "SelectDeviceActivity";

	// F�r att h�mta adressen till den valda enheten
	public static String EXTRA_DEVICE_ADRESS = "se.mah.ad1107.device_adress";

	// Anv�nds f�r att listan
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ListView listView;
	private BluetoothService

	/**
	 * Anropas n�r aktiviten skapas. Skapar listan och l�gger till items i
	 * listan.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_device);

		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		listView = (ListView) findViewById(R.id.paired_device_list);
		listView.setAdapter(mPairedDevicesArrayAdapter);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		setResult(RESULT_CANCELED);

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			// L�gg till Meddelende ifall det inte finns n�gra parade enheter
		}
		listView.setOnItemClickListener(mDeviceClickListener);
	}

	/**
	 * En lysnnare f�r vad som ska h�nda n�r man klickar p� ett item i listan.
	 * Skickar tillbaka en str�ng som inneh�ller en adress till en bluetooth
	 * device till den aktiviten som startade denna.
	 */
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

			// H�mta Mac adressen f�r enhenten man har valt. Den �r de sista 17
			// tecknena i TextViewn
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17); // Adressen �r
																	// de sista
																	// 17
																	// teckena i
																	// str�ngen
			Log.d(TAG, info + "\n" + address);

			// Skapar en intent s� att man kan skicka tillbaka data
			Intent intent = new Intent(SelectDeviceActivity.this, PlaylistActivity.class);
			intent.putExtra(EXTRA_DEVICE_ADRESS, address);
			Log.d(TAG,
					"Data in intent"
							+ intent.getExtras().getString(EXTRA_DEVICE_ADRESS));
			

//			// S�tt resultatet till ok och skicka tillbaka datan
//			setResult(RESULT_OK, intent);
//			finish();
			
			//   Starta en ny activity skicka med adressen och ett bluetooth service
			startActivity(intent);
		}
	};

}

