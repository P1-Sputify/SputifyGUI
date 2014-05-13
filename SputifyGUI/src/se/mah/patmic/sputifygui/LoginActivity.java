package se.mah.patmic.sputifygui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends ActionBarActivity {
	private String username = "test";
	private String password = "pw";
	private EditText editUser;
	private EditText editPassword;
	private Button loginButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
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
}
