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

/**
 * A singleton that gives access to the server connection
 * 
 * @author Michel
 * 
 */
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
	private boolean incomingTrackRequest = false;
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

	/**
	 * Call this method to start connecting to the server
	 * 
	 * @param connMgr
	 *            the ConnectivityManager of the operating system
	 * @param ipAddress
	 *            IP-address of the server you want to connect to
	 * @param portNr
	 *            port to connect on
	 */
	public void connect(ConnectivityManager connMgr, String ipAddress, int portNr) {
		this.connMgr = connMgr;
		this.ipAddress = ipAddress;
		this.portNr = portNr;

		// closes old sockets
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
				connectStatus = NOT_CONNECTED;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// starts the communication thread if it hasn't been started yet
		if (!commThreadStarted) {
			new Thread(new communicationThread()).start();
			commThreadStarted = true;
		}
	}

	/**
	 * give the connection a username and password to connect to when a connection is available
	 * 
	 * @param user
	 *            the username to log in with
	 * @param pass
	 *            the password to log in with
	 */
	public void login(String user, String pass) {
		this.user = user;
		this.pass = pass;
		loginStatus = LOGGING_IN;
	}

	/**
	 * @return the playlist that has been downloaded, if none has been downloaded returns null
	 */
	public Hashtable<Integer, Track> getPlayList() {
		return playList;
	}

	/**
	 * Request a track to download from the server
	 * 
	 * @param trackNr
	 *            the tracks id number
	 */
	public void requestTrack(int trackNr) {
		trackRequest = trackNr;
		incomingTrackRequest = true;
		requestedTrackStatus = TRACK_DOWNLOADING;
	}

	/**
	 * @return the requested track, if it's not downloaded yet, returns null
	 */
	public byte[] getRequestedTrack() {
		if (requestedTrackStatus == TRACK_RECIEVED) {
			return track;
		} else {
			return null;
		}
	}

	/**
	 * @return the status of connection to server, can be TCPConnection.NOT_CONNECTED,
	 *         TCPConnection.ATTEMPTING_TO_CONNECT or TCPConnection.CONNECTED
	 */
	public int getConnectStatus() {
		return connectStatus;
	}

	/**
	 * @return the login status, can be TCPConnection.NOT_LOGGED_IN, TCPConnection.LOGGING_IN or
	 *         TCPConnection.LOGGED_IN
	 */
	public int getLoginStatus() {
		return loginStatus;
	}

	/**
	 * @return the status of the playlist, can be TCPConnection.LIST_NOT_RECIEVED,
	 *         TCPConnection.LIST_DOWNLOADING or TCPConnection.LIST_RECIEVED
	 */
	public int getPlayListStatus() {
		return playListStatus;
	}

	/**
	 * @return the status of the requested track, can be TCPConnection.TRACK_NOT_RECIEVED,
	 *         TCPConnection.TRACK_DOWNLOADING or TCPConnection.TRACK_RECIEVED
	 */
	public int getRequestedTrackStatus() {
		return requestedTrackStatus;
	}

	/**
	 * This runnable will be started on a background thread, and do all the communication with the
	 * server
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class communicationThread implements Runnable {
		public void run() {
			while (true) {

				// Checks if the socket is connected
				if (socket != null && socket.isConnected()) {

					// If connected but not logged in
					if (loginStatus != LOGGED_IN) {

						// check if a login request has been made
						if (user != null && pass != null) {
							loginStatus = LOGGING_IN;
							String[] loginArray = new String[2];
							loginArray[0] = user;
							loginArray[1] = pass;

							try {
								// send login request to server
								oos.writeObject(loginArray);

								// wait for server reply
								String reply = (String) ois.readObject();

								// if login success
								if (reply.equalsIgnoreCase("login success")) {
									loginStatus = LOGGED_IN;
								}

								// if login failed, remove invalid login credentials to prevent
								// automatically attempting to log in again
								else if (reply.equalsIgnoreCase("login failed")) {
									loginStatus = NOT_LOGGED_IN;
									user = pass = null;
								}

								// just in something unexpected is received
								else {
									loginStatus = NOT_LOGGED_IN;
									user = pass = null;
									System.out.println("Server sent unkown reply: " + reply);
								}
							} catch (IOException | ClassNotFoundException e) {
								e.printStackTrace();
								loginStatus = NOT_LOGGED_IN;
								user = pass = null;
							}
						}
					}

					// if connected to server and logged in
					else {

						// if no playlist is recieved yet
						if (playListStatus != LIST_RECIEVED) {
							playListStatus = LIST_DOWNLOADING;
							try {
								// request a playlist from the server
								oos.writeObject("send playlist");

								// wait until playlist is recieved
								Object obj = ois.readObject();

								// make sure that a playlist was recieved
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
						}
						// If connected, logged in and playlist is recieved
						else if (requestedTrackStatus != TRACK_RECIEVED) {
							// If track is not recieved yet, check if there is a request
							if (incomingTrackRequest) {

								// check if the same track was requested again, no need to download
								// again since its still in the buffer
								if (track != null && currentTrack == trackRequest) {
									requestedTrackStatus = TRACK_RECIEVED;
								}

								// if a new track was requested
								else {
									downloadingTrackNr = trackRequest;
									requestedTrackStatus = TRACK_DOWNLOADING;
									try {
										// request the track from server
										oos.writeObject(Integer.valueOf(downloadingTrackNr));

										// receive the track from server
										Object obj = ois.readObject();

										// check that the correct form of data was recieved
										if (obj instanceof byte[]) {
											track = (byte[]) obj;

											// check if the downloaded track is the right one or if
											// another request has come in
											currentTrack = downloadingTrackNr;
											if (currentTrack == trackRequest) {
												requestedTrackStatus = TRACK_RECIEVED;
												incomingTrackRequest = false;
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
							
							// let thread sleep if there is no incoming track request
							else {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} 
						
						// let thread sleep if track is received
						else {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}

				// socket not connected
				else {
					connectStatus = ATTEMPTING_TO_CONNECT;
					try {
						if (connMgr != null) {

							// wait until the android device has an internet connection
							NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
							while (networkInfo == null || !networkInfo.isConnected()) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								networkInfo = connMgr.getActiveNetworkInfo();
							}

							// connect to the server
							socket = new Socket(InetAddress.getByName(ipAddress), portNr);

							// create streams
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
