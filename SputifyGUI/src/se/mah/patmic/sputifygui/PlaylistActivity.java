package se.mah.patmic.sputifygui;

import java.util.Hashtable;
import java.util.Set;

import server.Track;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PlaylistActivity extends ActionBarActivity {

	public final static String EXTRA_TRACK_NAME = "se.mah.patmic.sputifygui.TRACK_NAME";

	private Track[] tracklist;
	private Hashtable<Integer, Track> playListHash;
	private ArrayAdapter<String> adapter;
	private ListView listView;
	private TCPConnection tcpConnection;
	private AlertDialog currentAlertDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		tcpConnection = TCPConnection.INSTANCE;
		showPlayList(tcpConnection.getPlayList());
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

			listView.setAdapter(adapter);

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					String title = (String) parent.getItemAtPosition(position);
					if (tracklist[position].getName().equalsIgnoreCase(title)) {
						tcpConnection.requestTrack(tracklist[position].getId());

						Intent intent = new Intent(PlaylistActivity.this, PlayActivity.class);
						intent.putExtra(EXTRA_TRACK_NAME, tracklist[position].getName());
						startActivity(intent);

					} else {
						new AlertDialog.Builder(PlaylistActivity.this).setTitle("Track ID Error")
								.setMessage("Track ID Error").setNeutralButton(android.R.string.ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				}
			});
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
