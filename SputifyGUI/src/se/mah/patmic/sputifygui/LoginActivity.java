package se.mah.patmic.sputifygui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * The first activity in the Spütify app, it's used get login credentials from the user
 * 
 * @author Michel Falk
 * 
 */
public class LoginActivity extends ActionBarActivity {
	private String ipAddress = "195.178.234.227";
	private int portNr = 57005;
	private EditText editUser;
	private EditText editPassword;
	private Button loginButton;
	private String username;
	private String password;
	private TCPConnection tcpConnection;
	private ConnectivityManager connMgr;
	private AlertDialog currentAlertDialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		// Tells the tcp connection to connect to the server
		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		tcpConnection = TCPConnection.INSTANCE;
		tcpConnection.connect(connMgr, ipAddress, portNr);

		// Get the input fields for username and password
		editUser = (EditText) findViewById(R.id.Edit_UserName);
		editPassword = (EditText) findViewById(R.id.Edit_Password);

		// Gives the button a listener that starts the login procedure when clicked
		loginButton = (Button) findViewById(R.id.button_login);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				username = editUser.getText().toString();
				password = editPassword.getText().toString();

				login();
			}
		});
	}

	/**
	 * This method attempts to log in, it takes the login credentials from the class variables
	 * <code>username</code> and <code>password</code> to simplify retrying to log in when the
	 * server is slow, this method has to be run on the main thread
	 */
	private void login() {

		// If there is an alert dialog showing, closes it
		if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
			currentAlertDialog.cancel();
		}

		// If app is still trying to connect to the server, shows an alert dialog to tell this to
		// the user and starts a
		// thread that will try to log in again as soon as a connection has been established
		if (tcpConnection.getConnectStatus() == TCPConnection.ATTEMPTING_TO_CONNECT) {
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Connecting")
					.setMessage("Still trying to connect to server").setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_alert).show();
			new Thread(new waitConnectionThread()).start();
		}

		// If app is not connected to the server, shows an alert dialog to tell this to the user
		// with the option to try
		// to connect again
		else if (tcpConnection.getConnectStatus() == TCPConnection.NOT_CONNECTED) {
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Not connected")
					.setMessage("Could not connect to server")
					.setPositiveButton(R.string.reconnect_button, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							tcpConnection.connect(connMgr, ipAddress, portNr);
							login();
						}
					}).setNegativeButton(android.R.string.cancel, null).setIcon(android.R.drawable.ic_dialog_alert)
					.show();
		}

		// If the app is connected try to log in, shows an alert dialog to tell this to the user and
		// start a thread that
		// will wait for the login attempt to yield a result
		else if (tcpConnection.getConnectStatus() == TCPConnection.CONNECTED) {
			tcpConnection.login(username, password);
			new Thread(new waitForLoginConfirmationThread()).start();
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Logging in")
					.setMessage("Attempting to log in on server").setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_alert).show();
		}
	}

	/**
	 * Called when a login attempt has yielded a result, sends the user to the next activity if
	 * login attempt succeded or tells the user if it failed, this method has to be run on the main
	 * thread
	 */
	private void loginResult() {

		// If there is an alert dialog showing, closes it
		if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
			currentAlertDialog.cancel();
		}

		// If the login attempt succeeded start the next activity
		if (tcpConnection.getLoginStatus() == TCPConnection.LOGGED_IN) {
			Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
			startActivity(intent);
		}

		// If the login attempt failed, tell the user with an alert dialog
		else if (tcpConnection.getLoginStatus() == TCPConnection.NOT_LOGGED_IN) {
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Login failed")
					.setMessage("Make sure you spelled your username & password correctly")
					.setNeutralButton(android.R.string.ok, null).setIcon(android.R.drawable.ic_dialog_alert).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
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
	 * For making a thread that waits until a login attempt yields a result, then calls the
	 * <code>loginResult()</code> on the main thread
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class waitForLoginConfirmationThread implements Runnable {
		public void run() {
			while (tcpConnection.getLoginStatus() == TCPConnection.LOGGING_IN) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					loginResult();
				}
			});
		}
	}

	/**
	 * For making a thread that waits until a connection attempt yields a result and then tries to
	 * log in again by calling <code>login()</code> on the main thread
	 * 
	 * @author Michel Falk
	 * 
	 */
	private class waitConnectionThread implements Runnable {
		public void run() {
			while (tcpConnection.getConnectStatus() == TCPConnection.ATTEMPTING_TO_CONNECT) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					login();
				}
			});
		}
	}
}
