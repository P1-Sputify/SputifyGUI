package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.InputStream;

import android.support.v7.app.ActionBarActivity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View.OnClickListener;

/**
 * 
 * @author Andreas
 * 
 *         This class plays songs by pressing a play button and pausing the song
 *         by pressing the pause button
 */
public class PlayActivity extends ActionBarActivity {

	private Button buttonPlayStop; // This button is both play and stop, it only
									// changes icon
	private boolean audioPlaying = false;
	private boolean audioTrackInitiated = false;
	private AudioTrack audioTrack;
	private TCPConnection tcpConnection;
	private String trackName;
	private AlertDialog currentAlertDialog = null;
	private byte[] audioArray;
	private BluetoothService btService;
	private boolean sendSamplesThreadStarted = false;
	private Thread sendSamplesThread;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);

		tcpConnection = TCPConnection.INSTANCE;
		btService = BluetoothService.getBluetoothService();
		new Thread(new InitAudioTrackThread()).start();

		Intent startIntent = getIntent();
		trackName = startIntent
				.getStringExtra(PlaylistActivity.EXTRA_TRACK_NAME);

		try {
			initViews();
			setListeners();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(),
					e.getClass().getName() + " " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Create the buttons
	 */
	private void initViews() {
		if (trackName != null) {
			TextView songName = (TextView) findViewById(R.id.song_title);
			songName.setText(trackName);
		}

		buttonPlayStop = (Button) findViewById(R.id.ButtonPlayStop);
		buttonPlayStop.setBackgroundResource(R.drawable.play);
	}

	/**
	 * Set listeners to the buttons
	 */
	public void setListeners() {
		buttonPlayStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				buttonPlayStopClick();
			}
		});
	}

	/**
	 * Change the icon of buttonPlayStop
	 */
	private void buttonPlayStopClick() {
		if (!audioPlaying) {
			// While music is playing the stop icon will show
			buttonPlayStop.setBackgroundResource(R.drawable.pause);
			playAudio();
			audioPlaying = true;
		} else {
			// While music is paused the play icon will show
			buttonPlayStop.setBackgroundResource(R.drawable.play);
			pauseAudio();
			audioPlaying = false;
		}
	}

	/**
	 * Plays the music
	 */
	private void playAudio() {
		if (audioTrackInitiated) {
			audioTrack.play();
			sendSamplesThread = new Thread(new SendSamplesThread());
			sendSamplesThread.start();

		} else {
			// TODO Track not loaded
		}
	}

	private void pauseAudio() {
		if (audioTrackInitiated) {
			audioTrack.pause();
			sendSamplesThread.interrupt();
		} else {
			// TODO Track not loaded
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.play, menu);
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
			if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_NOT_RECIEVED) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (currentAlertDialog != null
								&& currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(
								PlayActivity.this).setTitle("No track")
								.setMessage("Track not recieved from server")
								.setNeutralButton(android.R.string.ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				});
				while (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_NOT_RECIEVED) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_DOWNLOADING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (currentAlertDialog != null
								&& currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(
								PlayActivity.this).setTitle("Downloading")
								.setMessage("Downloading track from server")
								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				});
				while (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_DOWNLOADING) {
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
					if (currentAlertDialog != null
							&& currentAlertDialog.isShowing()) {
						currentAlertDialog.cancel();
					}
				}
			});

			new Thread(new InitAudioTrackThread()).start();
		}
	}

	private class InitAudioTrackThread implements Runnable {
		@Override
		public void run() {
			if (!audioTrackInitiated) {
				if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_RECIEVED) {
					audioArray = tcpConnection.getRequestedTrack();
					int channels, encoding, samplerate, buffersize;

					// Kollar att filen är i WAV format
					if (new String(audioArray, 0, 4).equals("RIFF")
							&& new String(audioArray, 8, 4).equals("WAVE")) {

						// Kontrollerar att filen innehåller format beskrivning
						if (new String(audioArray, 12, 4).equals("fmt ")) {

							// kontrollerar att audio är kodad som PCM
							if (audioArray[20] == 1 && audioArray[21] == 0) {

								// kontrollerar bits/sample
								if (audioArray[34] == 16 && audioArray[35] == 0) {
									encoding = AudioFormat.ENCODING_PCM_16BIT;
								} else if (audioArray[34] == 8
										&& audioArray[35] == 0) {
									encoding = AudioFormat.ENCODING_PCM_8BIT;
								} else {
									encoding = AudioFormat.ENCODING_INVALID;
									// TODO PRINTA UT ROSTAT BRÖD: Unsuported
									// bit/sample
									return;
								}
							} else {
								encoding = AudioFormat.ENCODING_INVALID;
								// TODO PRINTA UT ROSTAT BRÖD: Unsuported
								// encoding
								return;
							}

							// kontrollerar att audio är mono eller stereo
							if (audioArray[22] == 2 && audioArray[23] == 0) {
								channels = AudioFormat.CHANNEL_OUT_STEREO;
							} else if (audioArray[22] == 1
									&& audioArray[23] == 0) {
								channels = AudioFormat.CHANNEL_OUT_MONO;
							} else {
								channels = AudioFormat.CHANNEL_INVALID;
								// TODO PRINTA UT ROSTAT BRÖD: Unsuported
								// channels
								return;
							}

							int temp;

							// Räknar ut samplerate
							temp = audioArray[24] & 0xFF;
							samplerate = temp;
							temp = audioArray[25] & 0xFF;
							samplerate += temp * 0x100;
							temp = audioArray[26] & 0xFF;
							samplerate += temp * 0x10000;
							temp = audioArray[27] & 0xFF;
							samplerate += temp * 0x1000000;

							// Räknar ut hur stor bufferten ska vara
							temp = audioArray[40] & 0xFF;
							buffersize = temp;
							temp = audioArray[41] & 0xFF;
							buffersize += temp * 0x100;
							temp = audioArray[42] & 0xFF;
							buffersize += temp * 0x10000;
							temp = audioArray[43] & 0xFF;
							buffersize += temp * 0x1000000;

							// Initierar audioTrack
							audioTrack = new AudioTrack(
									AudioManager.STREAM_MUSIC, samplerate,
									channels, encoding, buffersize,
									AudioTrack.MODE_STATIC);
							audioTrack.write(audioArray, 44, buffersize);
							audioTrackInitiated = true;
						} else {
							// TODO PRINTA UT ROSTAT BRÖD: Missing or corrupt
							// header
							return;
						}
					} else {
						// TODO PRINTA UT ROSTAT BRÖD: Unsuported file format
						return;
					}
				} else {
					new Thread(new waitingForDownloadThread()).start();
				}
			}
		}
	}

	/**
	 * Denna tråd kommer att används för att räkna ut medelvärted av amplituder
	 * under ett viss intervall och sedan anropa Bluetoothservice för att skicka
	 * detta värde till arduino.
	 * 
	 * @author Patrik
	 * 
	 */
	private class SendSamplesThread implements Runnable {

		/**
		 * Metoden används för att räkna ut medelvärdet
		 * 
		 * @param startIndex
		 *            Det första värdet i intervallet
		 * @param endIndex
		 *            Det sista värdet i intervallet
		 * @return En byte som innehåller medelvärtet.
		 */
		public int calculateAverage(byte[] array, int startIndex, int endIndex) {
			int sum = 0;
			int temp;
			for (int i = startIndex; i < endIndex; i++) {
				temp = array[i];
				if (temp < 0) {
					temp *= -1;
					temp--;
				}
				sum += temp;
			}

			sum /= 133;

			sum = ((int) Math.pow(sum, 1.18)) - 70;
			if (sum < 0) {
				sum = 0;
			}
			return sum;
		}

		@Override
		public void run() {
			int i = audioTrack.getPlaybackHeadPosition();
			while (i < audioArray.length - 133 - 44) {
				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					return;
				}

				int average = calculateAverage(audioArray, i + 44, i + 133 + 44);

				btService.write(average);

				i = audioTrack.getPlaybackHeadPosition();
			}
		}
	}
}
