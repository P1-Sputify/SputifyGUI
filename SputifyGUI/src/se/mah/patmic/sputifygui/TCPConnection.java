package se.mah.patmic.sputifygui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import server.Track;

public class TCPConnection {
	private ConnectivityManager connMgr;
	private Socket socket = null;
	private String ipAddress;
	private int portNr;
	private String user;
	private String pass;
	private Hashtable<Integer, Track> playList;

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

	public TCPConnection(ConnectivityManager connMgr, String ipAddress, int portNr) {
		this.connMgr = connMgr;
		this.ipAddress = ipAddress;
		this.portNr = portNr;
		connectStatus = ATTEMPTING_TO_CONNECT;

		new Thread(new ConnectTCPThread()).start();
	}

	public void login(String user, String pass) {
		this.user = user;
		this.pass = pass;
		loginStatus = LOGGING_IN;
		
		new Thread(new LoginThread()).start();
	}

	public Hashtable<Integer, Track> getPlaylist() {
		if (loginStatus == LOGGED_IN) {
			try {
				oos.writeObject("send playlist");
				Object obj = ois.readObject();
				if (obj instanceof Hashtable) {
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

	public int getConnectStatus() {
		return connectStatus;
	}

	public int getLoginStatus() {
		return loginStatus;
	}

	private class ConnectTCPThread implements Runnable {
		public void run() {
			try {
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
				while (networkInfo == null || !networkInfo.isConnected()) {
					Thread.sleep(500);
					networkInfo = connMgr.getActiveNetworkInfo();
				}

				socket = new Socket(InetAddress.getByName(ipAddress), portNr);
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				connectStatus = CONNECTED;
			} catch (IOException | InterruptedException e) {
				connectStatus = NOT_CONNECTED;
			}
		}
	}

	private class LoginThread implements Runnable {
		public void run() {
			while (connectStatus == ATTEMPTING_TO_CONNECT) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (connectStatus == NOT_CONNECTED) {
				return;
			} else {
				String[] loginArray = new String[2];
				loginArray[0] = user;
				loginArray[1] = pass;

				try {
					oos.writeObject(loginArray);
					String reply = (String) ois.readObject();
					System.out.println("Hit kommer jag?");
					if (reply.equalsIgnoreCase("login success")) {
						loginStatus = LOGGED_IN;
						return;
					} else if (reply.equalsIgnoreCase("login failed")) {
						loginStatus = NOT_LOGGED_IN;
						return;
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					loginStatus = NOT_LOGGED_IN;
					return;
				}

				loginStatus = NOT_LOGGED_IN;
				return;
			}
		}
	}
}
