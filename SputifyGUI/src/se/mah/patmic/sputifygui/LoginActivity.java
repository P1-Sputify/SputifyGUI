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
	private String ipAddress = ""; //TODO add ip-address
	private int portNr = 57005;
	private String username = "test";
	private String password = "pw";
	private EditText editUser;
	private EditText editPassword;
	private Button loginButton;
	TCPService tcpService;
    boolean bound = false;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		Intent intent = new Intent(this, TCPService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

		editUser = (EditText)findViewById(R.id.Edit_UserName);
		editPassword = (EditText)findViewById(R.id.Edit_Password);
		
		loginButton = (Button)findViewById(R.id.button_login);
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String inUsername = editUser.getText().toString();
				String inPassword = editPassword.getText().toString();
				if(inUsername.equals(username) && inPassword.equals(password)) {
					Intent intent = new Intent(LoginActivity.this, SelectDeviceActivity.class);
//					Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
					startActivity(intent);
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
