package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

/**
 * This class will manage connection to other bluetooth devices and incomming
 * and outgoing messages.
 * 
 * @author Patrik
 * 
 */
public class BluetoothService {
	// Debugging
	private static final String TAG = "BluetoothService";

	// Konstaner
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String mServerAdress; // Mac adressen till server ( i detta
											// fall vår bluetooth modul.)

	private final BluetoothAdapter mBTAdapter = BluetoothAdapter
			.getDefaultAdapter(); // Hämtar en referens till mobilens Bluetooth
									// telefon, om denna är null stöder mobilen
									// ej bluetooth

	// Konstaner som indikerar bluetooth State
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// devices

	// Klassvariabler
	private ConnectThread mConnectThread;
	private manageConnectionThread mManagConnectionThread;
	private int mState; // Anvdnds för att visa vilken state bluetooth befinner
						// sig i.
//	private Handler mHandler; // Handler för att skicka meddeleande tillbaka
								// till Activityn som har gui
	private Context mContext; // Contexten som man vill att servicen ska vara
								// connectad till.

	/**
	 * A Constructor that prepares a new Session.
	 * 
	 * @param applicationContext
	 *            The context that hosts the gui.
	 * @param handler
	 *            A Handler that sends messages back to the activity
	 */
	public BluetoothService(Context applicationContext) {
		mState = STATE_NONE;
//		mHandler = handler;
		mContext = applicationContext;
	}

	/**
	 * The method changes the bluetooth state
	 * 
	 * @param state
	 *            One of the constants STATE_NONE, STATE_LISTEN,
	 *            STATE_CONNECTING, STATE_CONNECTED
	 */
	public synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + "--> " + state);
		mState = state;

		// Skicka tillbaka meddelande med handler

	}

	/**
	 * Returns the current state
	 * 
	 * @return An integer represting the state
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * The method starts a new Session
	 * 
	 * @return
	 */
	public synchronized void start() {
		Log.d(TAG, "start() ");

		// Avbryt tråd om någon försöker göra en connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt mManageConnectionThread om den är igång
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		setState(STATE_NONE); // Ingeting görs just nu.
	}

	/**
	 * Initiating a connection to a bluetooth device
	 * 
	 * @param device
	 *            The device to make a connection with.
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "Trying to iniitiate connection to: " + device);

		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}

		}

		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		mConnectThread = new ConnectThread(device);
		mConnectThread.start(); // Startar en connection
		setState(STATE_CONNECTING); // Enheterna försöker skapa en connection
	}

	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		// Avbryt tr�den som har gjort en ansluting
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt tr�d som har ansluting om s�dan finns
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		// Starta en ny manageConnectThread
		mManagConnectionThread = new manageConnectionThread(socket);
		mManagConnectionThread.start();

		// Skicka tillbaka meddlenade till UI

	}

	public manageConnectionThread getmManagConnectionThread() {
		return mManagConnectionThread;
	}

	/**
	 * The method is used to stop all threads
	 */
	public synchronized void stop() {
		Log.d(TAG, "stop");

		// Avbryt ConnectThread
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Avbryt manageConnected thread
		if (mManagConnectionThread != null) {
			mManagConnectionThread.cancel();
			mManagConnectionThread = null;
		}

		setState(STATE_NONE);
	}

//	public void write(byte[] out) {
//		// Create temporary object
//		manageConnectionThread r;
//		// Synchronize a copy of the ConnectedThread
//		synchronized (this) {
//			if (mState != STATE_CONNECTED)
//				return;
//			r = mManagConnectionThread;
//		}
//		// Perform the write unsynchronized
//		r.write(out);
//	}

	/**
	 * Sends a message back to the ui that the connection has been lost
	 */
	private void connectionLost() {
		setState(STATE_NONE);

		// TODO S�nd tillbaka till ui
	}

	/**
	 * Sends a message back to the ui that the connection attempt failed
	 */
	private void connectionFailed() {
		setState(STATE_NONE);

		// TODO S�nd tillbaka till UI
	}

	/**
	 * This Thread is used to attempt to make a connection with a remote
	 * bluetooth device
	 * 
	 * @author Patrik
	 * 
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null; // Anv�nder en tempor�r socket s� f�r
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
				// Ett blockerand anrop. Kommer bara att �terv�nda ifall ett
				// execption kastas eller lyckad ansluting
				mmSocket.connect();
			} catch (IOException e) {
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"Unable to close socket, during connection failure",
							e2);
				}

			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}

			// Starta manageConnection thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Socket failed to close during canel", e);
			}

		}
	}

	/**
	 * This thread is used to listen for incomming data and send data.
	 * 
	 * @author Patrik
	 * 
	 */
	public class manageConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public manageConnectionThread(BluetoothSocket socket) {
			Log.d(TAG, "create manageConnectionThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// H�mta utstr�m och instr�m fr�n bluetoohsocket
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
			write("Connection Sucessfull".getBytes()); // Skriver ett meddelande till bluetooth moduen

			byte[] buffer = new byte[1024]; // En buffert f�r att lagra den data
											// som ska skickas. Storleken
											// beh�ver nog �ndras.

			int bytes; // En r�knare f�r att kunna ha l�ngden av meddelandet man
						// skickar

			// S� l�nge som tr�den �r aktiv ska den lyssna efter inkommande
			// meddelande
			while (true) {
				try {
					bytes = mmInStream.read();
				} catch (IOException e) {
					Log.e(TAG, "Connection Lost", e);
					connectionLost();
					break; // Man kan inte l�sa fr�n instr�mmen om det inte
							// finns n�gon ansluting.
				}
				// TODO Skicka meddelande till gui.
			}
		}

		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				// Share the sent message back to the UI Activity
				// mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1,
				// buffer)
				// .sendToTarget();
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
