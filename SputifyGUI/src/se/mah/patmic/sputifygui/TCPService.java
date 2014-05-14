package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class TCPService extends Service {
	private final IBinder binder = new TCPBinder();
	private Socket socket = null;
	private String ipAddress;
	private int portNr;

	private int connectStatus = NOT_CONNECTED;
	public final static int NOT_CONNECTED = 11;
	public final static int ATTEMPTING_TO_CONNECT = 12;
	public final static int CONNECTED = 13;

	private int loginStatus = NOT_LOGGED_IN;
	public final static int NOT_LOGGED_IN = 21;
	public final static int LOGGING_IN = 22;
	public final static int LOGGED_IN = 23;

	private int playlistStatus = NOT_RECIEVED;
	public final static int NOT_RECIEVED = 31;
	public final static int DOWNLOADING = 32;
	public final static int RECIEVED = 33;

	public class TCPBinder extends Binder {
		TCPService getService() {
			return TCPService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public void connectToServer(String ip, int port) {
		ipAddress = ip;
		portNr = port;
		Thread thread = new Thread(new ConnectTCPThread());
		thread.start();
	}

	public int login(String user, String pass) {
		while(connectStatus == ATTEMPTING_TO_CONNECT){}
		if (connectStatus == NOT_CONNECTED) {
			return NOT_CONNECTED;
		} else {
			if (loginStatus == LOGGED_IN) {
				return LOGGED_IN;
			} else {
				return 0;
				//TODO finish login
			}

		}
	}

	private class ConnectTCPThread implements Runnable {
		public void run() {
			try {
				connectStatus = ATTEMPTING_TO_CONNECT;
				socket = new Socket(InetAddress.getByName(ipAddress), portNr);
				connectStatus = CONNECTED;
			} catch (IOException e) {
				connectStatus = NOT_CONNECTED;
			}
		}
	}
}
