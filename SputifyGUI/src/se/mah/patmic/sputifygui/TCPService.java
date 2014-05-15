package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import server.Track;

public class TCPService extends Service {
	private final IBinder binder = new TCPBinder();
	private Socket socket = null;
	private String ipAddress;
	private int portNr;
	
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;

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
		if (connectStatus == NOT_CONNECTED) {
			ipAddress = ip;
			portNr = port;
			Thread thread = new Thread(new ConnectTCPThread());
			thread.start();
		}
	}

	public int login(String user, String pass) {
		if (connectStatus != CONNECTED) {
			return connectStatus;
		} else {
			loginStatus = LOGGING_IN;
			String[] loginArray = new String[2];
			loginArray[0] = user;
			loginArray[1] = pass;

			try {
				oos.writeObject(loginArray);
				String reply = (String) ois.readObject();
				if (reply.equalsIgnoreCase("login success")) {
					loginStatus = LOGGED_IN;
					return loginStatus;
				} else if (reply.equalsIgnoreCase("login failed")) {
					loginStatus = NOT_LOGGED_IN;
					return loginStatus;
				}

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				loginStatus = NOT_LOGGED_IN;
				return loginStatus;
			}

			loginStatus = NOT_LOGGED_IN;
			return loginStatus;
		}
	}

	public Hashtable<Integer, Track> getPlaylist() {
		if (loginStatus == LOGGED_IN) {
			try {
				oos.writeObject("send playlist");
				Object obj = ois.readObject();
				if (obj instanceof Hashtable<?, ?>) {
					return (Hashtable<Integer, Track>) obj;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		} else {
			return null;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("Service destroyed");
	}

	private class ConnectTCPThread implements Runnable {
		public void run() {
			try {
				ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
				while (!(networkInfo != null && networkInfo.isConnected())) {
					Thread.sleep(100);
				}
				connectStatus = ATTEMPTING_TO_CONNECT;
				socket = new Socket(InetAddress.getByName(ipAddress), portNr);
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				connectStatus = CONNECTED;
			} catch (IOException | InterruptedException e) {
				connectStatus = NOT_CONNECTED;
			}
		}
	}
}
