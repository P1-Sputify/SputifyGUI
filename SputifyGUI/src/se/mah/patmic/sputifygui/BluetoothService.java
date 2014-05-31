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
 * and outgoing messages. This class is a designed with a singelton design pattern
 * and there can only be one unicqe instance of this object.
 * 
 * @author Patrik
 * 
 */
public class BluetoothService {
	private static BluetoothService uniqInstance;
	private static final String TAG = "BluetoothService";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Konstaner som indikerar bluetooth State
	public static final int STATE_NONE = 0; // Ingetting händer
	public static final int STATE_CONNECTING = 1; // Mobilen försöker ansluta till någon bluetooth enhets
	public static final int STATE_CONNECTED = 2; // Nu finns en Anslutning
	
	private ConnectThread mConnectThread;
	private manageConnectionThread mManageConnectionThread;
	private int mState;
	
	
	
	/**
	 * Gets a referemce to the BluetoothService object. If there is no object a new one will be created
	 * 
	 * @return
	 * 		A bluetooth Service object
	 */
	public static synchronized BluetoothService getBluetoothService() {
		if(uniqInstance == null) {
			uniqInstance = new BluetoothService();
		}
		Log.i(TAG, "Returned Bluetooth Sevice");
		return uniqInstance;
	}

	/**
	 * This method sets a integer that is representing the current state of the object
	 * @param state
	 * 		An integer. Use the constants STATE_NONE, STATE_CONNECTING or STATE_CONNECTED
	 */
	public synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + "--> " + state);
		mState = state;
	}

	/**
	 * Returns a integer with a integer that is representing the state of the object. You can use the constansts STATE_NONE, STATE_CONENCTED or STATE_STATE_CONNECTING
	 * to compare with
	 * @return
	 * 		An integer contatining the state of the object
	 */
	public synchronized int getState() {
		return mState;
	}
	
	
	/**
	 * This method is used to start the bluetooth connection. and sets the state to NONE
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
	 * The method is used to create a connection thread and attempt to connect to a bluetooth Device
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "Trying to initiate connection to: " + device);
		start();

		mConnectThread = new ConnectThread(device);
		mConnectThread.start(); // Startar en connection
		setState(STATE_CONNECTING); // Enheterna försöker skapa en connection
	}
	
	/**
	 * The method is used to create the thread that is used to recieve and send data over bluetooth
	 * @param socket
	 * 		A reference bluetoothSocket object
	 * @param device
	 * 		The reference to the BluetoothDevice object you want to connect to
	 */
	public synchronized void startManageConnectionThread(BluetoothSocket socket,
			BluetoothDevice device) {
		start();

		// Starta en ny manageConnectThread
		mManageConnectionThread = new manageConnectionThread(socket);
//		mManageConnectionThread.start();
	}
	
	/**
	 * This method is used to indicate that the connection has been lost and stops all threads
	 */
	private void connectionLost() {
		Log.d(TAG, "The bluetooth connection was lost");
		setState(STATE_NONE);
		start(); // Denna används för att garbage collectorn ska ta bort oanänvda resurser och så att man kan göra ny connection.
	}

	/**
	 * This method is used to indicate that the connection failed
	 */
	private void connectionFailed() {
		setState(STATE_NONE);
		Log.d(TAG, "The Bluetooth connection failed");
		start();
	}
	
	/**
	 * Use this method to send the values over bluetooth
	 * @param message
	 * 		An integer contating the value you want to send
	 */
	public void write(int value) {
			mManageConnectionThread.write(value);
		
	}

	/**
	 *	This class is used to make a connection between two bluetooth devices. It extends the thread class.
	 * @author Patrik
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		/**
		 * The constructor sets up the object before running the thread. It tries to create a bluetoothsocket
		 * @param device
		 * 		The reference to a bluetoothDevice object
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
		 * This method should be called when you are done with the socket.
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
	 * This class is used to manage reciving and send data
	 * @author Patrik
	 *
	 */
	private class manageConnectionThread extends Thread{
		private final BluetoothSocket mmSocket;
//		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		/**
		 * The constructor sets upp the socket and the out and in stream for the data.
		 * @param socket
		 * 		A reference to a bluetoothDevice socket
		 */
		public manageConnectionThread(BluetoothSocket socket) {
			Log.d(TAG, "create manageConnectionThread");
			mmSocket = socket;
//			InputStream tmpIn = null;
			OutputStream tmpOut = null;
//			byte[] buffer = new byte[1024];
//			int bytes;

			// Hämta utström och inström från bluetoohsocket
			try {
//				tmpIn = mmSocket.getInputStream();
				tmpOut = mmSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp streams not created", e);
			}

			mmOutStream = tmpOut;
//			mmInStream = tmpIn;
		}

//		@Override
//		public void run() {
//			Log.i(TAG, "Run manageConnectionThread");
//			setState(STATE_CONNECTED); // Nu ska man kunna skicka
////			while(true) { // Man ska hela tiden ligga och titta efter inkoommande data
////				try {
////					bytes = mmInStream.read();
////				} catch(IOException e) {
////					Log.e(TAG, "Connection lost");
////					connectionLost();
////				}	
////			}
//		}
		/**
		 * This method sends a value over bluetooth
		 * @param message
		 */
		public void write(int value) {
			try {
				mmOutStream.write(value);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
		/**
		 * This method is used to close the socket.
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
