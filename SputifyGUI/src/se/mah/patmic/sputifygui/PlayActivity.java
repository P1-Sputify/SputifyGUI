package se.mah.patmic.sputifygui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import android.support.v7.app.ActionBarActivity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View.OnClickListener;

/**
 * This activity plays a chosen song by pressing a play button and pausing the song by pressing the
 * pause button, it also sends values to the a spütify bluetooth unit to make LEDs light up
 * 
 * @author Andreas Stridh & Michel Falk
 */
public class PlayActivity extends ActionBarActivity {

	private Button buttonPlayPause; // This button is both play and pause, it only changes icon
	private SeekBar seekBar;
	private boolean audioPlaying = false;
	private boolean audioTrackInitiated = false;
	private AudioTrack audioTrack;
	private TCPConnection tcpConnection;
	private String trackName;
	private AlertDialog currentAlertDialog = null;
	private byte[] audioArray;
	private BluetoothService btService;
	private Thread sendSamplesThread, seekBarThread;
	private double nrOfAudioFramesInTrack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);

		// gets the instances of server and bluetooth connections
		tcpConnection = TCPConnection.INSTANCE;
		btService = BluetoothService.getBluetoothService();

		// starts a tread that loads the audio data
		new Thread(new InitAudioTrackThread()).start();

		// sets the track name
		Intent startIntent = getIntent();
		trackName = startIntent.getStringExtra(PlaylistActivity.EXTRA_TRACK_NAME);

		// initialize views and give them listeners
		initViews();
		setListeners();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		// turns off music when going back to playlist
		try {
			pauseAudio();
			audioTrack.flush();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the activities views
	 */
	private void initViews() {

		// set the track name
		if (trackName != null) {
			TextView songName = (TextView) findViewById(R.id.song_title);
			songName.setText(trackName);
		}

		// give the play/pause button an icon
		buttonPlayPause = (Button) findViewById(R.id.ButtonPlayPause);
		buttonPlayPause.setBackgroundResource(R.drawable.play);

		// disable user interaction on seekbar
		seekBar = (SeekBar) findViewById(R.id.audio_seek_bar);
		seekBar.setEnabled(false);
	}

	/**
	 * Set listeners to the button
	 */
	public void setListeners() {
		buttonPlayPause.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// if the button is clicked when audio is playing, pause gets called, if its not
				// playing when the button is clicked play gets called
				if (audioPlaying) {
					pauseAudio();
				} else {
					playAudio();
				}
			}
		});
	}

	/**
	 * Start playing the music
	 */
	private void playAudio() {
		if (audioTrackInitiated) {

			// start to play audio
			audioTrack.play();
			audioPlaying = true;

			// While music is playing the pause icon will show
			buttonPlayPause.setBackgroundResource(R.drawable.pause);

			// start thread to send data to bluetooth
			sendSamplesThread = new Thread(new SendSamplesThread());
			sendSamplesThread.start();

			// start a thread to update the seekbar
			seekBarThread = new Thread(new SeekBarThread());
			seekBarThread.start();
		} else {
			showErrorMessage("Error", "File not loaded");
		}
	}

	/**
	 * Pauses the music
	 */
	private void pauseAudio() {
		if (audioTrackInitiated) {

			// pauses the audio
			audioTrack.pause();
			audioPlaying = false;

			// While music is paused the play icon will show
			buttonPlayPause.setBackgroundResource(R.drawable.play);
		} else {
			showErrorMessage("Error", "File not loaded");
		}
	}

	/**
	 * Call when whole file is played
	 */
	private void endOfFile() {

		// reloads the audio data so song can be played again
		audioTrack.reloadStaticData();

		// switch to play-button
		buttonPlayPause.setBackgroundResource(R.drawable.play);

		audioPlaying = false;
	}

	/**
	 * Display an alert dialog with an OK button, can be called from background thread
	 * 
	 * @param title
	 *            String with the title for the dialog
	 * @param message
	 *            String with the message for the dialog
	 */
	private void showErrorMessage(final String title, final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
					currentAlertDialog.cancel();
				}
				currentAlertDialog = new AlertDialog.Builder(PlayActivity.this).setTitle(title).setMessage(message)
						.setNeutralButton(android.R.string.ok, null).setIcon(android.R.drawable.ic_dialog_alert).show();
			}
		});

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
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(PlayActivity.this).setTitle("No track")
								.setMessage("Track not recieved from server")
								.setNeutralButton(android.R.string.ok, null)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
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
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						currentAlertDialog = new AlertDialog.Builder(PlayActivity.this).setTitle("Downloading")
								.setMessage("Downloading track from server").setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert).show();
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
					if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
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
					int tempChannels = -1, tempSampleSize = -1;

					// Kollar att filen är i WAV format
					if (new String(audioArray, 0, 4).equals("RIFF") && new String(audioArray, 8, 4).equals("WAVE")) {

						// Kontrollerar att filen innehåller format beskrivning
						if (new String(audioArray, 12, 4).equals("fmt ")) {

							// kontrollerar att audio är kodad som PCM
							if (audioArray[20] == 1 && audioArray[21] == 0) {

								// kontrollerar bits/sample
								if (audioArray[34] == 16 && audioArray[35] == 0) {
									encoding = AudioFormat.ENCODING_PCM_16BIT;
									tempSampleSize = 16;
								} else if (audioArray[34] == 8 && audioArray[35] == 0) {
									encoding = AudioFormat.ENCODING_PCM_8BIT;
									tempSampleSize = 8;
								} else {
									encoding = AudioFormat.ENCODING_INVALID;
									showErrorMessage("Unsuported format",
											"The bits-per-sample rate on this file is not supported");
									return;
								}
							} else {
								encoding = AudioFormat.ENCODING_INVALID;
								showErrorMessage("Unsuported format", "The encoding on this file is not supported");
								return;
							}

							// kontrollerar att audio är mono eller stereo
							if (audioArray[22] == 2 && audioArray[23] == 0) {
								channels = AudioFormat.CHANNEL_OUT_STEREO;
								tempChannels = 2;
							} else if (audioArray[22] == 1 && audioArray[23] == 0) {
								channels = AudioFormat.CHANNEL_OUT_MONO;
								tempChannels = 1;
							} else {
								channels = AudioFormat.CHANNEL_INVALID;
								showErrorMessage("Unsuported format",
										"The number of channels on this file is not supported");
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

							nrOfAudioFramesInTrack = (buffersize * 8) / tempSampleSize / tempChannels;

							// Initierar audioTrack
							audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, samplerate, channels, encoding,
									buffersize, AudioTrack.MODE_STATIC);
							audioTrack.write(audioArray, 44, buffersize);
							audioTrackInitiated = true;
						} else {
							showErrorMessage("Unsuported format", "Could not find file-header");
							return;
						}
					} else {
						showErrorMessage("Unsuported format", "Unsupported file-type");
						return;
					}
				} else {
					new Thread(new waitingForDownloadThread()).start();
				}
			}
		}
	}

	/**
	 * This thread will calculate averages of the amplitudes in the WAV array and send appropriately
	 * scaled results to the arduino over bluetooth
	 * 
	 * @author Patrik & Michel
	 * 
	 */
	private class SendSamplesThread implements Runnable {

		/**
		 * Gammal metod för att räkna ut medelvärdet, den är hårdkodad för 8000Hz 8bit mono, om den
		 * nya fungerar, ta bort denna
		 * 
		 * @param startIndex
		 *            Det första värdet i intervallet
		 * @param endIndex
		 *            Det sista värdet i intervallet
		 * @return En byte som innehåller medelvärtet.
		 */
		@Deprecated
		public int calculateAverage(byte[] array, int startIndex, int count) {
			int sum = 0;
			int temp;
			for (int i = startIndex; i < (startIndex + count) && i < array.length; i++) {
				temp = array[i];
				if (temp < 0) {
					temp *= -1;
					temp--;
				}
				sum += temp;
			}

			sum /= count;

			sum = ((int) Math.pow(sum, 4)) / 750000 - 10;
			if (sum < 0) {
				sum = 0;
			}
			if (sum > 255) {
				sum = 255;
			}
			return sum;
		}

		/**
		 * Method to calculate an average value for a part of a WAV array and scale the result down
		 * to 0-255 on an exponential scale
		 * 
		 * @param array
		 *            WAV array to be used
		 * @param sampleSize16bit
		 *            true if the array is to be read as 16 bit values, if false it will be read as
		 *            8 bit values
		 * @param startIndex
		 *            index to start calculations from
		 * @param count
		 *            number indexes to read
		 * @return result of the calculation
		 */
		public int calculateAverage(byte[] array, boolean sampleSize16bit, int startIndex, int count) {
			long sum = 0;
			int average;
			int temp;

			if (sampleSize16bit) {
				ByteBuffer bb = ByteBuffer.wrap(array, startIndex, count);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				ShortBuffer sb = bb.asShortBuffer();
				while (sb.hasRemaining()) {
					temp = sb.get();
					if (temp < 0) {
						temp *= -1;
						temp--;
					}
				}

				average = (int) (sum / count);
				average = (int) ((int) Math.pow(average, 4) / 4000000000000000l);
				if (average < 0) {
					average = 0;
				}
				if (average > 255) {
					average = 255;
				}
			} else {
				for (int i = startIndex; i < startIndex + count && i < array.length; i++) {
					temp = array[i] & 0xFF;
					sum += temp;
				}
				average = (int) (sum / count);
				average = ((int) Math.pow(average, 4)) / 12500000 - 10;
				if (average < 0) {
					average = 0;
				}
				if (average > 255) {
					average = 255;
				}
			}

			return average;
		}

		@Override
		public void run() {
			int i = audioTrack.getPlaybackHeadPosition();
			boolean sampleSize16bit = audioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT;
			final int ledFrequencyInHz = 60;

			int sleepTime = (int) ((1f / ledFrequencyInHz) * 1000);

			int nrOfSamples = audioTrack.getSampleRate() / ledFrequencyInHz;

			if (audioTrack.getChannelConfiguration() == AudioFormat.CHANNEL_OUT_STEREO) {
				nrOfSamples *= 2;
			}

			while (audioPlaying && (i < audioArray.length - 133 - 44)) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				int average = calculateAverage(audioArray, sampleSize16bit, i + 44, nrOfSamples + 44);

				btService.write(average);

				i = audioTrack.getPlaybackHeadPosition();
			}

			btService.write(0);
		}
	}

	/**
	 * Thread to update the seekbar, it also calls endOfFile() when the the song is done playing
	 * 
	 * @author Michel
	 * 
	 */
	private class SeekBarThread implements Runnable {

		@Override
		public void run() {
			int max = seekBar.getMax();
			int pos;
			double percentage;
			int progress;
			while (audioPlaying) {
				pos = audioTrack.getPlaybackHeadPosition();
				percentage = pos / nrOfAudioFramesInTrack;
				progress = (int) (max * percentage);
				seekBar.setProgress(progress);

				if (pos >= nrOfAudioFramesInTrack) {
					endOfFile();
				}

				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
