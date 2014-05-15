package se.mah.patmic.sputifygui;

import se.mah.patmic.sputifygui.TCPService.TCPBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
	TCPService tcpService;
	boolean bound = false;
	private Intent tcpIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		tcpIntent = new Intent(this, TCPService.class);

		editUser = (EditText) findViewById(R.id.Edit_UserName);
		editPassword = (EditText) findViewById(R.id.Edit_Password);

		loginButton = (Button) findViewById(R.id.button_login);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String inUsername = editUser.getText().toString();
				String inPassword = editPassword.getText().toString();
				int res = tcpService.login(inUsername, inPassword);

				while (res == TCPService.ATTEMPTING_TO_CONNECT) {
					// TODO add connecting to server message
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					res = tcpService.login(inUsername, inPassword);
				}
				if (res == TCPService.NOT_CONNECTED) {
					// TODO add not connected error
					return;
				} else if (res == TCPService.LOGGED_IN){
					Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
					// Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
					startActivity(intent);
				} else if (res == TCPService.NOT_LOGGED_IN) {
					// TODO wrong user/pass message
				}
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(connection);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		bindService(tcpIntent, connection, Context.BIND_AUTO_CREATE);
		tcpService.connectToServer(ipAddress, portNr);
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

	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			bound = false;
		}

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			TCPBinder binder = (TCPBinder) arg1;
			tcpService = binder.getService();
			bound = true;
		}
	};
}
