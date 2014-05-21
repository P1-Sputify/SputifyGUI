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
	private AlertDialog currentAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		tcpConnection = TCPConnection.INSTANCE;
		tcpConnection.connect(connMgr, ipAddress, portNr);

		editUser = (EditText) findViewById(R.id.Edit_UserName);
		editPassword = (EditText) findViewById(R.id.Edit_Password);

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

	private void login() {
		if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
			currentAlertDialog.cancel();
		}

		if (tcpConnection.getConnectStatus() == TCPConnection.ATTEMPTING_TO_CONNECT) {
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Connecting")
					.setMessage("Still trying to connect to server").setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_alert).show();
			new Thread(new waitConnectionThread()).start();
		} else if (tcpConnection.getConnectStatus() == TCPConnection.NOT_CONNECTED) {
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
		} else if (tcpConnection.getConnectStatus() == TCPConnection.CONNECTED) {
			tcpConnection.login(username, password);
			new Thread(new waitForLoginConfirmationThread()).start();
			currentAlertDialog = new AlertDialog.Builder(LoginActivity.this).setTitle("Logging in")
					.setMessage("Attempting to log in on server").setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_alert).show();
		}
	}

	private void loginResult() {
		if (currentAlertDialog != null && currentAlertDialog.isShowing()) {
			currentAlertDialog.cancel();
		}
		if (tcpConnection.getLoginStatus() == TCPConnection.LOGGED_IN) {
			Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
			// Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
			startActivity(intent);
		} else if (tcpConnection.getLoginStatus() == TCPConnection.NOT_LOGGED_IN) {
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
