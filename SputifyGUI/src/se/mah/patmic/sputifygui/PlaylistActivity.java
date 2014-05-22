package se.mah.patmic.sputifygui;

import java.util.Hashtable;
import java.util.Set;

import server.Track;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		tcpConnection = TCPConnection.INSTANCE;
		playListHash = tcpConnection.getPlayList();
		if (playListHash != null) {
			tracklist = new Track[playListHash.size()];
			Set<Integer> keys = playListHash.keySet();
			listView = (ListView) findViewById(R.id.playlist);
			adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
			int counter = 0;
			for (Integer key : keys) {
				tracklist[counter++] = playListHash.get(key);
			}

			for (int i = 0; i < tracklist.length; i++) {
				adapter.add(tracklist[i].getName());
			}
			listView.setAdapter(adapter);
		} else {
			new AlertDialog.Builder(this).setTitle("No playlist").setMessage("Could not retrieve playlist from server")
					.setNeutralButton(android.R.string.ok, null).setIcon(android.R.drawable.ic_dialog_alert).show();
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
		int currentByte;
		
		public sendSamples(int sampleRate, int sampleDepth, int length, byte[] data) {
			this.sampleRate = sampleRate;
			this.sampleDepth = sampleDepth;
			this.length = length;
			this.data = data;
		}
		
		public byte calculateAverage(int first, int last) {
			byte[] test ;
			byte average = 0;
			
			
			return average;
		}
		
		@Override
		public void run() {
		// TODO Auto-generated method stub
		
		}
	}
}
