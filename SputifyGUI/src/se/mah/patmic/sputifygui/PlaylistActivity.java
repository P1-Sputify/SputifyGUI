package se.mah.patmic.sputifygui;

import java.util.Hashtable;
import java.util.Set;

import server.Track;
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
			// TODO no playlist case
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
}
