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

/**
 * An activity in the Spütify app that displays the playlist the server sends and lets the user
 * choose a song to download and play
 * 
 * @author Michel Falk
 * 
 */
public class PlaylistActivity extends ActionBarActivity {

	public final static String EXTRA_TRACK_NAME = "se.mah.patmic.sputifygui.TRACK_NAME";

	private Track[] tracklist;
	private ArrayAdapter<String> adapter;
	private ListView listView;
	private TCPConnection tcpConnection;
	private AlertDialog currentAlertDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		// Tries to show a playlist that the server has sent
		tcpConnection = TCPConnection.INSTANCE;
		showPlayList(tcpConnection.getPlayList());
	}

	/**
	 * Method to show a playlist, if the playListHash that gets sent in is null, a thread will be
	 * started that waits the server to send a playlist
	 * 
	 * @param playListHash
	 *            the playlist to be shown
	 */
	private void showPlayList(Hashtable<Integer, Track> playListHash) {

		// if the playlist is null, that means one has not been received from the server yet
		if (playListHash != null) {

			// Create a array and copy the contents of playListHash into it
			tracklist = new Track[playListHash.size()];
			Set<Integer> keys = playListHash.keySet();
			int counter = 0;
			for (Integer key : keys) {
				tracklist[counter++] = playListHash.get(key);
			}

			// Create an ArrayAdapter and gives it the names of the tracks
			adapter = new ArrayAdapter<>(PlaylistActivity.this, android.R.layout.simple_list_item_1);
			for (int i = 0; i < tracklist.length; i++) {
				adapter.add(tracklist[i].getName());
			}

			// make the ListView show the playlist
			listView = (ListView) findViewById(R.id.playlist);
			listView.setAdapter(adapter);

			// give listView a listener so users can choose a track by tapping it
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

					// Tells the server to start downloading the track the user has chosen
					tcpConnection.requestTrack(tracklist[position].getId());

					// Starts the next activity and gives it the track-name as an extra
					Intent intent = new Intent(PlaylistActivity.this, PlayActivity.class);
					intent.putExtra(EXTRA_TRACK_NAME, tracklist[position].getName());
					startActivity(intent);

				}
			});
		} else {
			// Starts a thread that will wait for a playlist, in case none was recieved from the
			// server yet
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
	 * For making a thread that waits for a playlist to be downloaded
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class waitingForDownloadThread implements Runnable {
		public void run() {

			// If the playlist has not started downloading yet or has failed to download
			if (tcpConnection.getPlayListStatus() == TCPConnection.LIST_NOT_RECIEVED) {

				runOnUiThread(new Runnable() {
					@Override
					public void run() {

						// If there is an alert dialog showing, closes it
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						
						// show alert dialog that playlist is not downloaded
						currentAlertDialog = new AlertDialog.Builder(PlaylistActivity.this).setTitle("No playlist")
								.setMessage("Playlist not recieved from server")
								.setNeutralButton(android.R.string.ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				});

				// Wait until the playlist starts downloading 
				while (tcpConnection.getPlayListStatus() == TCPConnection.LIST_NOT_RECIEVED) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// If the playlist is downloading
			if (tcpConnection.getPlayListStatus() == TCPConnection.LIST_DOWNLOADING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						
						// If there is an alert dialog showing, closes it
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						
						// show alert dialog that playlist is downloading
						currentAlertDialog = new AlertDialog.Builder(PlaylistActivity.this).setTitle("Downloading")
								.setMessage("Downloading playlist from server").setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				});
				
				// wait until download is done
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
					
					// If there is an alert dialog showing, closes it
					if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
						currentAlertDialog.cancel();
					}
					
					// new attempt to show the playlist
					showPlayList(tcpConnection.getPlayList());
				}
			});
		}
	}
}
