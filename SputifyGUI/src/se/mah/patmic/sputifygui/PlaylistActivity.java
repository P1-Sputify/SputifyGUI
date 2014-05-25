package se.mah.patmic.sputifygui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Set;

import server.Track;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PlaylistActivity extends ActionBarActivity {

	private Track[] tracklist;
	private Hashtable<Integer, Track> playListHash;
	private ArrayAdapter<String> adapter;
	private ListView listView;
	private TCPConnection tcpConnection;
	private AlertDialog currentAlertDialog = null;
	private final String TAG = "PlayListActivity";
	private BluetoothService btService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		tcpConnection = TCPConnection.INSTANCE;
		btService = BluetoothService.getBluetoothService();
		showPlayList(tcpConnection.getPlayList());
		
		new Thread(new sendSamples()).start();
	}

	private void showPlayList(Hashtable<Integer, Track> playListHash) {
		if (playListHash != null) {
			tracklist = new Track[playListHash.size()];
			Set<Integer> keys = playListHash.keySet();
			listView = (ListView) findViewById(R.id.playlist);
			adapter = new ArrayAdapter<>(PlaylistActivity.this, android.R.layout.simple_list_item_1);
			int counter = 0;
			for (Integer key : keys) {
				tracklist[counter++] = playListHash.get(key);
			}

			for (int i = 0; i < tracklist.length; i++) {
				adapter.add(tracklist[i].getName());
			}
			
			// TODO Add listener
			
			listView.setAdapter(adapter);
		} else {
			new Thread(new waitingForDownloadThread()).start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.playlist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Denna tråd kommer att används för att räkna ut medelvärted av amplituder 
	 * under ett viss intervall och sedan anropa Bluetoothservice för att skicka 
	 * detta värde till arduino.
	 * @author Patrik
	 *
	 */
	private class sendSamples implements Runnable {
		int sampleRate; // Antalet samples per sekunder
		int sampleDepth; // Antalet bitar i varje sample
		int length; // Antal sampels. Tid i sekunder * sampleRate
		byte[] data; // En byte array med datan
		int currentByte = 0;
		InputStream in;
		byte old = 0;
		
		
//		public sendSamples(int sampleRate, int sampleDepth, int length, byte[] data, int updateFreq) {
//			this.sampleRate = sampleRate;
//			this.sampleDepth = sampleDepth;
//			this.length = length;
//			this.data = data;
//		}
		
		public sendSamples() {
			initilizeFileReader();
		}
		
		/**
		 * Metoden används för att räkna ut medelvärdet
		 * @param first
		 * 		Det första värdet i intervallet
		 * @param last
		 * 		Det sista värdet i intervallet
		 * @return
		 * 		En byte som innehåller medelvärtet.
		 */
		public int calculateAverage(byte[] values){
			int sum = 0;
			for(int i = 0; i < values.length; i++) {
				sum += Math.abs(values[i]);
			}
			sum = sum / 800;
//			System.out.println(sum);
			return sum;
		}
		
		
		/**
		 * Metoden används för att iniitiera en fileReader
		 */
		public void initilizeFileReader() {
			File sdcard = Environment.getExternalStorageDirectory();
			Log.i(TAG, sdcard.getPath());
			File file = new File("/storage/emulated/0/b240.raw"); // ÄNDRA HÄR FÖR VILKEN FIL
			in = null; // In strömmen som vi kommer använda
			try {
				in = new BufferedInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				Log.e(TAG, " in initilizeFileReader and File not found");
				e.printStackTrace();
			}
//			finally{
//				if (in != null) {
//					try {
//						in.close();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
		}
		
		public byte readByte(int startIndex) {
			int bytesToBeRead = 1;
			byte[] data = new byte[1];
			for(int i = 0; i < data.length; i++) {
				try {
					data[i] = (byte)(in.read());
//					System.out.println(data[i]);; // Använder Absolutbelopp för att annars kommer det att gå mot 0
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			currentByte += 1;
			byte returnValue = data[0];
			return returnValue;
		}
		
		@Override
		public void run() {
			while(true) {
//				byte average = (byte)calculateAverage(readByte(currentByte));
				byte something = readByte(currentByte);
//				System.out.println(average);
				btService.write(something);
				try {
					Thread.sleep(4);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.e(TAG, "Thread SleepInterrupted");
				}
			}
		}
	}

	private class waitingForDownloadThread implements Runnable {
		public void run() {
			if (tcpConnection.getPlayListStatus() == TCPConnection.LIST_NOT_RECIEVED) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(PlaylistActivity.this).setTitle("No playlist")
								.setMessage("Playlist not recieved from server")
								.setNeutralButton(android.R.string.ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				});
				while (tcpConnection.getPlayListStatus() == TCPConnection.LIST_NOT_RECIEVED) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			if (tcpConnection.getPlayListStatus() == TCPConnection.LIST_DOWNLOADING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(PlaylistActivity.this).setTitle("Downloading")
								.setMessage("Downloading playlist from server").setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				});
				while (tcpConnection.getPlayListStatus() == TCPConnection.LIST_DOWNLOADING) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
						currentAlertDialog.cancel();
					}
					showPlayList(tcpConnection.getPlayList());
				}
			});
		}
	}
}
