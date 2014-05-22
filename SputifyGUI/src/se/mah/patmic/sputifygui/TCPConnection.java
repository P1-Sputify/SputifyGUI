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

public enum TCPConnection {
	INSTANCE;

	private boolean commThreadStarted = false;
	private Socket socket = null;
	private ConnectivityManager connMgr = null;
	private String ipAddress = null;
	private int portNr;

	private String user = null;
	private String pass = null;

	private Hashtable<Integer, Track> playList = null;
	private int trackRequest;
	private int currentTrack;
	private int downloadingTrackNr;
	private byte[] track = null;

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

	private int playListStatus = LIST_NOT_RECIEVED;
	public final static int LIST_NOT_RECIEVED = 31;
	public final static int LIST_DOWNLOADING = 32;
	public final static int LIST_RECIEVED = 33;

	private int requestedTrackStatus = TRACK_NOT_RECIEVED;
	public final static int TRACK_NOT_RECIEVED = 41;
	public final static int TRACK_DOWNLOADING = 42;
	public final static int TRACK_RECIEVED = 43;

	public void connect(ConnectivityManager connMgr, String ipAddress, int portNr) {
		this.connMgr = connMgr;
		this.ipAddress = ipAddress;
		this.portNr = portNr;
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
				connectStatus = NOT_CONNECTED;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!commThreadStarted) {
			new Thread(new communicationThread()).start();
			commThreadStarted = true;
		}
	}

	public void login(String user, String pass) {
		this.user = user;
		this.pass = pass;
		loginStatus = NOT_LOGGED_IN;
	}

	public Hashtable<Integer, Track> getPlayList() {
		return playList;
	}

	public void requestTrack(int trackNr) {
		trackRequest = trackNr;
		requestedTrackStatus = TRACK_NOT_RECIEVED;
	}

	public byte[] getRequestedTrack() {
		if (requestedTrackStatus == TRACK_RECIEVED) {
			return track;
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

	public int getPlayListStatus() {
		return playListStatus;
	}

	public int getRequestedTrackStatus() {
		return requestedTrackStatus;
	}

	private class communicationThread implements Runnable {
		public void run() {
			while (true) {
				if (socket != null && socket.isConnected()) {
					if (loginStatus != LOGGED_IN && user != null && pass != null) {
						loginStatus = LOGGING_IN;
						String[] loginArray = new String[2];
						loginArray[0] = user;
						loginArray[1] = pass;

						try {
							oos.writeObject(loginArray);
							String reply = (String) ois.readObject();
							if (reply.equalsIgnoreCase("login success")) {
								loginStatus = LOGGED_IN;
							} else if (reply.equalsIgnoreCase("login failed")) {
								loginStatus = NOT_LOGGED_IN;
								user = pass = null;
							} else {
								loginStatus = NOT_LOGGED_IN;
								user = pass = null;
								System.out.println("Server sent unkown reply: " + reply);
							}
						} catch (IOException | ClassNotFoundException e) {
							e.printStackTrace();
							loginStatus = NOT_LOGGED_IN;
							user = pass = null;
						}
					} else { // loginStatus == LOGGED_IN && socket connected
						if (playListStatus != LIST_RECIEVED) {
							playListStatus = LIST_DOWNLOADING;
							try {
								oos.writeObject("send playlist");
								Object obj = ois.readObject();
								if (obj instanceof Hashtable) {
									playList = (Hashtable<Integer, Track>) obj;
									playListStatus = LIST_RECIEVED;
								} else {
									playListStatus = LIST_NOT_RECIEVED;
								}
							} catch (IOException | ClassNotFoundException e) {
								e.printStackTrace();
								playListStatus = LIST_NOT_RECIEVED;
							}
						} else if (requestedTrackStatus == TRACK_NOT_RECIEVED) {
							if (track != null && currentTrack == trackRequest) {
								requestedTrackStatus = TRACK_RECIEVED;
							} else {
								downloadingTrackNr = trackRequest;
								requestedTrackStatus = TRACK_DOWNLOADING;
								try {
									oos.writeObject(Integer.valueOf(downloadingTrackNr));
									Object obj = ois.readObject();
									if (obj instanceof byte[]) {
										track = (byte[]) obj;
										currentTrack = downloadingTrackNr;
										if (currentTrack == trackRequest) {
											requestedTrackStatus = TRACK_RECIEVED;
										} else {
											requestedTrackStatus = TRACK_NOT_RECIEVED;
										}
									} else {
										requestedTrackStatus = TRACK_NOT_RECIEVED;
									}
								} catch (IOException | ClassNotFoundException e) {
									e.printStackTrace();
									requestedTrackStatus = TRACK_NOT_RECIEVED;
								}
							}
						}
					}
				} else { // socket not connected
					connectStatus = ATTEMPTING_TO_CONNECT;
					try {
						if (connMgr != null) {
							NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
							while (networkInfo == null || !networkInfo.isConnected()) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								networkInfo = connMgr.getActiveNetworkInfo();
							}
							socket = new Socket(InetAddress.getByName(ipAddress), portNr);
							ois = new ObjectInputStream(socket.getInputStream());
							oos = new ObjectOutputStream(socket.getOutputStream());
							connectStatus = CONNECTED;
							loginStatus = NOT_LOGGED_IN;
						} else {
							connectStatus = NOT_CONNECTED;
							commThreadStarted = false;
							return;
						}
					} catch (IOException e) {
						connectStatus = NOT_CONNECTED;
					}
				}
			}
		}
	}
}
