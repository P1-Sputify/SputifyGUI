package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class will manage connection to other bluetooth devices and incomming
 * and outgoing messages.
 * 
 * @author Patrik
 * 
 */
public class BluetoothService {
	private static BluetoothService uniqInstance;
	private static final String TAG = "BluetoothService";
	private final BluetoothAdapter mBTAdapter; // Om denna är null så stödjer inte mobilen bluetooth
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String mAdress; // Adressen till Bluetooth modulen
	// Konstaner som indikerar bluetooth State
	public static final int STATE_NONE = 0; // Ingetting händer
	public static final int STATE_CONNECTING = 1; // Mobilen försöker ansluta till någon bluetooth enhets
	public static final int STATE_CONNECTED = 2; // Nu finns en Anslutning
	private ConnectThread mConnectThread;
	private manageConnectionThread mManageConnectionThread;
	private int mState;
	
	/**
	 * Konstruktor som skapar sätter upp bluetoothService objektet
	 */
	private BluetoothService() {
		Log.i(TAG, "Creating Bluetooth Sevice");
		 mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		 start();
	}
	
	/**
	 * Hämtar en referens till BluetoothService objektet.
	 * @return
	 * 		Ett bluetoothService object
	 */
	public static synchronized BluetoothService getBluetoothService() {
		if(uniqInstance == null) {
			uniqInstance = new BluetoothService();
		}
		Log.i(TAG, "Returned Bluetooth Sevice");
		return uniqInstance;
	}

	/**
	 * Använd denna metod för att sätta vilket läge objektet befinner sig i.
	 * 
	 * @param state
	 * 		Använd någon av konstanterna STATE_CONNECTED, STATE_CONNECTING, STATE_NONE
	 *            
	 */
	public synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + "--> " + state);
		mState = state;
	}

	/**
	 * Retunerar objektets nuvarande läge. Använd konstanterna STATE_CONNECTED, STATE_CONNECTING, STATE_NONE
	 * för att jämnföra vad som retuneras
	 * @return
	 * 		En integer som representerar vilket läge som objektet befinner sig i.
	 * 		
	 */
	public synchronized int getState() {
		return mState;
	}
	
	
	/**
	 * Metoden används när en ny ansluting ska göras. Den tar bort de nuvrande anslutningarna.
	 */
	public synchronized void start() {
		Log.d(TAG, "start() ");

		// Avbryt tråd om någon försöker göra en connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt mManageConnectionThread om den är igång
		if (mManageConnectionThread != null) {
			mManageConnectionThread.cancel();
			mManageConnectionThread = null;
		}

		setState(STATE_NONE); // Ingeting görs just nu.
	}

	/**
	 * Metoden försöker göra en anslutning till Bluetooth enheten som man anger
	 * @param device
	 * 		En referens till det bluetoothDevice objekt man vill ansluta till.
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "Trying to initiate connection to: " + device);
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}

		}

		if (mManageConnectionThread != null) {
			mManageConnectionThread.cancel();
			mManageConnectionThread = null;
		}

		mConnectThread = new ConnectThread(device);
		mConnectThread.start(); // Startar en connection
		setState(STATE_CONNECTING); // Enheterna försöker skapa en connection
	}
	
	/**
	 * Startar den tråd som har hand om skicking och ta emot av data
	 * @param socket
	 * 		Den socket som skapats av connectionThread.
	 * @param device
	 * 		Måste kontrollera om denna parameter ens behövs.
	 */
	public synchronized void startManageConnectionThread(BluetoothSocket socket,
			BluetoothDevice device) {
		// Avbryt tråden som har gjort en ansluting
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt tråd som har ansluting om sådan finns
		if (mManageConnectionThread != null) {
			mManageConnectionThread.cancel();
			mManageConnectionThread = null;
		}

		// Starta en ny manageConnectThread
		mManageConnectionThread = new manageConnectionThread(socket);
		mManageConnectionThread.start();
	}

	/**
	 * Denna metod används för att stoppa alla anslutningar.
	 */
	public synchronized void stop() {
		Log.d(TAG, "stop");

		// Avbryt ConnectThread
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt manageConnected thread
		if (mManageConnectionThread != null) {
			mManageConnectionThread.cancel();
			mManageConnectionThread = null;
		}

		setState(STATE_NONE);
	}
	
	/**
	 * Denna metod används för att logga ifall anslutingen förlorats.
	 */
	private void connectionLost() {
		Log.d(TAG, "The bluetooth connection was lost");
		setState(STATE_NONE);
	}

	/**
	 * Denna metod används för att logga att anslutningen misslyckades 
	 */
	private void connectionFailed() {
		setState(STATE_NONE);
		Log.d(TAG, "The Bluetooth connection failed");
	}

	/**
	 * Denna tråd används för att skapa en anslutnign mellan mobilen och en Bluetooth enhet
	 * @author Patrik
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		/**
		 * Konstruktorn sätter upp tråden. Den försöker en socket
		 * @param device
		 * 		En referens till den bluetooth enhet man vill ansluta till
		 */
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null; // Använder en temporär socket så för
										// att inte tilldela den
										// "riktiga variablen f�r en socket null"
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				Log.i(TAG, "Socket Creation Sucessfull");
				
			} catch (IOException e) {
				Log.e(TAG, "Socket Creation Failed");
			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			Log.i(TAG, "mConnectThread started");
			setName("ConnectThread");
			try {
				mmSocket.connect(); // Detta anropen blockerar tråden. Den kommer bara att ge en timeout eller en lyckad anslutning
				setState(STATE_CONNECTING);
			} catch (IOException e) {
				connectionFailed();
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"Unable to close socket, during connection failure",
							e2);
				}

			}

			// Återställer tråden eftersom vi har anslutnigen
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}

			// Starta manageConnection thread
			startManageConnectionThread(mmSocket, mmDevice);
		}
		
		/**
		 * Denna metod ska anropas när man är klar med socketen. Kan lämpligtvis anropas i onDestroy eller liknade
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket failed to close during canel", e);
			}

		}
	}
	
	/**
	 * Denna tråd används för att hantera överförning av data över bluetooth
	 * @author Patrik
	 *
	 */
	public class manageConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		/**
		 * Konstruktorn sätter upp tråden
		 * @param socket
		 * 		Den socket man vill använda för att föra över datan
		 */
		public manageConnectionThread(BluetoothSocket socket) {
			Log.d(TAG, "create manageConnectionThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Hämta utström och inström från bluetoohsocket
			try {
				tmpIn = mmSocket.getInputStream();
				tmpOut = mmSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp streams not created", e);
			}

			mmOutStream = tmpOut;
			mmInStream = tmpIn;
		}

		@Override
		public void run() {
			Log.i(TAG, "Run manageConnectionThread");
			setState(STATE_CONNECTED); // Nu ska man kunna skicka
			write("Bajs på dig".getBytes());
			byte[] buffer = new byte[1024]; // En buffert som används för att ta emot data

			int bytes; // En räknare som innehåller längden på det som tagit emots

			// Så länge som tråden är aktiv ska den lyssna efter inkommande
			// meddelande. Detta kommer alltså blockera allt annat i Run metoden.
			while (true) {
				try {
					bytes = mmInStream.read();
				} catch (IOException e) {
					Log.e(TAG, "Connection Lost", e);
					connectionLost();
					break; // Man kan inte läsa från inströmmen om det inte
							// finns någon ansluting.
				}
				// TODO Skicka meddelande till gui.
			}
		}
		
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
