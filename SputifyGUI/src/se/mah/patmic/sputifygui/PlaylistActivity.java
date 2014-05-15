package se.mah.patmic.sputifygui;

import java.util.Hashtable;

import se.mah.patmic.sputifygui.TCPService.TCPBinder;
import server.Track;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PlaylistActivity extends ActionBarActivity {

	private String[] testlist = { "track 1", "track 2", "track 3", "track 4", "track 5" };
	private Hashtable<Integer, Track> playListHash;
	private ArrayAdapter<String> adapter;
	private ListView listView;
	TCPService tcpService;
	boolean bound = false;
	private Intent tcpIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		tcpIntent = new Intent(this, TCPService.class);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(connection);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		bindService(tcpIntent, connection, Context.BIND_AUTO_CREATE);

		listView = (ListView) findViewById(R.id.playlist);
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		for (int i = 0; i < testlist.length; i++) {
			adapter.add(testlist[i]);
		}
		listView.setAdapter(adapter);
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
	
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			TCPBinder binder = (TCPBinder) arg1;
			tcpService = binder.getService();
			bound = true;
		}
	};
}
