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

	private int playListStatus = NOT_RECIEVED;
	public final static int NOT_RECIEVED = 31;
	public final static int DOWNLOADING = 32;
	public final static int RECIEVED = 33;

	public TCPConnection(ConnectivityManager connMgr, String ipAddress, int portNr) {
		this.connMgr = connMgr;
		this.ipAddress = ipAddress;
		this.portNr = portNr;

		new Thread(new ConnectTCPThread()).start();
	}

	public void login(String user, String pass) {
		this.user = user;
		this.pass = pass;

		new Thread(new LoginThread()).start();
	}

	public Hashtable<Integer, Track> getPlayList() {
		if (playListStatus == NOT_RECIEVED) {
			fetchPlayList();
		}
		
		while (playListStatus != RECIEVED) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return playList;
	}

	public int getConnectStatus() {
		return connectStatus;
	}

	public int getLoginStatus() {
		return loginStatus;
	}

	public int getPlayListStatus() {
		return playListStatus;
	}

	private void fetchPlayList() {
		new Thread(new fetchPlayListThread()).start();
	}

	private class ConnectTCPThread implements Runnable {
		public void run() {
			connectStatus = ATTEMPTING_TO_CONNECT;
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
			loginStatus = LOGGING_IN;
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
						fetchPlayList();
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

	private class fetchPlayListThread implements Runnable {
		public void run() {
			playListStatus = DOWNLOADING;

			while (loginStatus == LOGGING_IN) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (loginStatus == LOGGED_IN) {
				try {
					oos.writeObject("send playlist");
					Object obj = ois.readObject();
					if (obj instanceof Hashtable) {
						playList = (Hashtable<Integer, Track>) obj;
						playListStatus = RECIEVED;
					} else {
						playListStatus = NOT_RECIEVED;
					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
					playListStatus = NOT_RECIEVED;
				}
			}
		}
	}
}
