package se.mah.patmic.sputifygui;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class TCPService extends Service {

	private final IBinder mBinder = new TCPBinder();

	public class TCPBinder extends Binder {
		TCPService getService() {
			return TCPService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
