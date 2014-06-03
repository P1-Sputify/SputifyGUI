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

	private Button buttonPlayPause; // This button is both play and pause, it
									// only changes icon
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
	private double nrOfAudioSamplesInTrack;

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
		audioTrack.stop();
		audioTrack.flush();
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

				// if the button is clicked when audio is playing, pause gets
				// called, if its not
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
		audioPlaying = false;

		// switch to play-button
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				buttonPlayPause.setBackgroundResource(R.drawable.play);
			}
		});

		// reloads the audio data so song can be played again
		audioTrack.stop();
		audioTrack = null;
		audioTrackInitiated = false;
		new Thread(new InitAudioTrackThread()).start();
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
				// If there is an alert dialog showing, closes it
				if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
					currentAlertDialog.cancel();
				}
				// Show new alert dialog
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

	/**
	 * For making a thread that waits until the audio-track has downloaded and shows matching alert
	 * dialogs
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class waitingForDownloadThread implements Runnable {
		public void run() {

			// wait until track starts downloading and show this in an alert
			// dialog
			if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_NOT_RECIEVED) {
				showErrorMessage("No track", "Track not recieved from server");
				while (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_NOT_RECIEVED) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// wait until track is done downloading and show this in an alert
			// dialog
			if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_DOWNLOADING) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// If there is an alert dialog showing, closes it
						if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
							currentAlertDialog.cancel();
						}
						// show new alert dialog, not closable by user
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

			// If there is an alert dialog showing, closes it
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
						currentAlertDialog.cancel();
					}
				}
			});

			// Download done, starting thread to try and initiate the audio
			new Thread(new InitAudioTrackThread()).start();
		}
	}

	/**
	 * For making a thread that reads the header of a WAV file and initiates an AudioTrack object
	 * accordingly
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class InitAudioTrackThread implements Runnable {
		@Override
		public void run() {

			// only run if not initiated yet
			if (!audioTrackInitiated) {

				// makes sure that the sendSamplesThread isn't running
				if (sendSamplesThread != null) {
					while (sendSamplesThread.isAlive()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				// checks if the track has been downloaded
				if (tcpConnection.getRequestedTrackStatus() == TCPConnection.TRACK_RECIEVED) {

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// If there is an alert dialog showing, closes it
							if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
								currentAlertDialog.cancel();
							}
							// show new alert dialog, not closable by user
							currentAlertDialog = new AlertDialog.Builder(PlayActivity.this).setTitle("Initializing")
									.setMessage("Initializing the audio data").setCancelable(false)
									.setIcon(android.R.drawable.ic_dialog_alert).show();
						}
					});

					int channels, encoding, samplerate, buffersize;
					int tempChannels = -1, tempSampleSize = -1;

					// gets the track from the tcp object
					audioArray = tcpConnection.getRequestedTrack();

					// Checks that the file is WAV format
					if (new String(audioArray, 0, 4).equals("RIFF") && new String(audioArray, 8, 4).equals("WAVE")) {

						// Checks that the file contains a format description
						if (new String(audioArray, 12, 4).equals("fmt ")) {

							// Checks that the file is coded as PCM
							if (audioArray[20] == 1 && audioArray[21] == 0) {

								// Checks bits/sample
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

							// Checks if the audio is mono or stereo
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

							// Read samplerate
							temp = audioArray[24] & 0xFF;
							samplerate = temp;
							temp = audioArray[25] & 0xFF;
							samplerate += temp * 0x100;
							temp = audioArray[26] & 0xFF;
							samplerate += temp * 0x10000;
							temp = audioArray[27] & 0xFF;
							samplerate += temp * 0x1000000;

							// Reads data size to determine required size for
							// the buffer
							temp = audioArray[40] & 0xFF;
							buffersize = temp;
							temp = audioArray[41] & 0xFF;
							buffersize += temp * 0x100;
							temp = audioArray[42] & 0xFF;
							buffersize += temp * 0x10000;
							temp = audioArray[43] & 0xFF;
							buffersize += temp * 0x1000000;

							// Calculates how many samples the file contains
							nrOfAudioSamplesInTrack = (buffersize * 8) / tempSampleSize / tempChannels;

							// Initiates audioTrack
							audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, samplerate, channels, encoding,
									buffersize, AudioTrack.MODE_STATIC);

							// Write audio data to audioTrack
							audioTrack.write(audioArray, 44, buffersize);

							audioTrackInitiated = true;

							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// If there is an alert dialog showing,
									// closes it
									if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
										currentAlertDialog.cancel();
									}
								}
							});
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
	 * For making a thread that will calculate averages of the amplitudes in the WAV array and send
	 * appropriately scaled results to the arduino over bluetooth
	 * 
	 * @author Patrik Larsson & Michel Falk
	 * 
	 */
	private class SendSamplesThread implements Runnable {

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

			// if the size of the samples is 16 bits
			if (sampleSize16bit) {

				// Calculate sum of the specified part of the array
				for (int i = startIndex; i < (startIndex + count) && (i + 1) < array.length; i += 2) {
					temp = array[i] & 0xFF;
					temp += array[i + 1] * 0x100;
					if (temp < 0) {
						temp *= -1;
						temp--;
					}
					sum += temp;
				}

				// calculate average
				average = (int) (sum / count);

				// scale the result
				average = (int) ((long) Math.pow(average, 3) / 80000000000l);
			}

			// if the size of the samples is 8 bits
			else {

				// Calculate sum of the specified part of the array
				for (int i = startIndex; i < (startIndex + count) && i < array.length; i++) {
					temp = array[i];
					if (temp < 0) {
						temp *= -1;
						temp--;
					}
					sum += temp;
				}

				// calculate average
				average = (int) (sum / count);

				// scale the result
				average = ((int) Math.pow(average, 4)) / 750000 - 10;
			}

			// make sure the results are within the correct range
			if (average < 0) {
				average = 0;
			}
			if (average > 255) {
				average = 255;
			}
			return average;
		}

		@Override
		public void run() {
			int audioFrameSize = 1;
			boolean sampleSize16bit = audioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT;
			final int ledFrequencyInHz = 60;

			// calculates how long the thread has to sleep between updates to
			// get the specified
			// frequency, sleep is not optimal for this, but exactness is not
			// that important in this
			// instance
			int sleepTime = (int) ((1f / ledFrequencyInHz) * 1000);

			if (audioTrack.getChannelConfiguration() == AudioFormat.CHANNEL_OUT_STEREO) {
				audioFrameSize *= 2;
			}

			if (sampleSize16bit) {
				audioFrameSize *= 2;
			}

			// calculates how many samples there are between each update of the
			// LEDs
			int nrOfSamples = audioTrack.getSampleRate() / ledFrequencyInHz * audioFrameSize;

			int i = audioTrack.getPlaybackHeadPosition() * audioFrameSize;

			// continue sending data until audio is stopped or end of array is
			// reached
			while (audioPlaying && (i < audioArray.length - 133 - 44) && audioTrack != null) {

				// sleep to get approximately the correct frequency
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// calculates average
				int average = calculateAverage(audioArray, sampleSize16bit, i + 44, nrOfSamples);

				// send result the the arduino by bluetooth
				btService.write(average);

				// update pointer
				if (audioTrack != null) {
					i = audioTrack.getPlaybackHeadPosition() * audioFrameSize;
				}
			}

			// turn off the LEDs when music stops
			btService.write(0);
		}
	}

	/**
	 * For making a thread to update the seekbar, it also calls endOfFile() when the the song is
	 * done playing
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class SeekBarThread implements Runnable {

		@Override
		public void run() {
			int max = seekBar.getMax();
			int pos;
			double percentage;
			int progress;

			// update seekbar while music is playing
			while (audioPlaying) {

				// calculate progress and set the seekbar accordingly
				pos = audioTrack.getPlaybackHeadPosition();
				percentage = pos / nrOfAudioSamplesInTrack;
				progress = (int) (max * percentage);
				seekBar.setProgress(progress);

				// if the end of file is reached, call appropriate method
				if (pos >= nrOfAudioSamplesInTrack) {
					endOfFile();
				}

				// sleep 250ms to update about 4 times every second
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
