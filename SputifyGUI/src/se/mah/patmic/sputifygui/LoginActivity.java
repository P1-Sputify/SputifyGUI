package se.mah.patmic.sputifygui;

import android.content.Context;
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
	private TCPConnection tcpConnection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		tcpConnection = TCPConnection.INSTANCE;
		tcpConnection.connect(connMgr, ipAddress, portNr);

		editUser = (EditText) findViewById(R.id.Edit_UserName);
		editPassword = (EditText) findViewById(R.id.Edit_Password);

		loginButton = (Button) findViewById(R.id.button_login);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String inUsername = editUser.getText().toString();
				String inPassword = editPassword.getText().toString();

				if (tcpConnection.getConnectStatus() == TCPConnection.ATTEMPTING_TO_CONNECT) {
					// TODO add connecting to server message

					return;
				} else if (tcpConnection.getConnectStatus() == TCPConnection.NOT_CONNECTED) {
					// TODO add not connected error
					return;
				} else if (tcpConnection.getConnectStatus() == TCPConnection.CONNECTED) {
					tcpConnection.login(inUsername, inPassword);
					// TODO add logging in message
					while (tcpConnection.getLoginStatus() == TCPConnection.LOGGING_IN) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					if (tcpConnection.getLoginStatus() == TCPConnection.LOGGED_IN) {
						Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
						// Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
						startActivity(intent);
					} else if (tcpConnection.getLoginStatus() == TCPConnection.NOT_LOGGED_IN) {
						// TODO wrong user/pass message
					}
				}
			}
		});
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
}
