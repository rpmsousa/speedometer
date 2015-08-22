package com.example.rsousa.speednotification;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

class LocationHandler implements LocationListener {
	static int LOCATION_ARRAY_SIZE = 16;
	static long LOCATION_MIN_UPDATE_PERIOD_NS = 1000000000;

	private LocationManager locationManager;
	private Location locationPrevious = null;
	private Location locationFirst = null;
	private Location[] locationArray= new Location[LOCATION_ARRAY_SIZE];
	private int locationW = 0;
	private int locationN = 0;
	private int locationR = 0;
	private int count = 0;
	private int locationPreviousW = 0;
	private TextView mView;

	LocationHandler(Activity activity, TextView view) {
		locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
		mView = view;
	}

	@Override
	public void finalize() throws Throwable {
		locationManager.removeUpdates(this);

		super.finalize();
	}

	void locationPrint(TextView view, Location location1, Location location2) {
		String info;

		if (location2 == null)
			info = String.format("%d %f %f %.1f\n", count, location1.getLatitude(), location1.getLongitude(), location1.getAccuracy());
		else
			info = String.format("%d %f %f %.1f %.1f %.1f %.1f\n", count, location1.getLatitude(), location1.getLongitude(), location1.getAccuracy(),
				location2.distanceTo(location1), location2.bearingTo(location1), location1.getAccuracy()/location2.distanceTo(location1)*180.0/Math.PI);

		view.append(info);
	}

	Location locationFirst() {
		return locationFirst;
	}

	void locationAdd(Location location) {
		locationArray[locationW] = location;
		locationPrevious = location;
		locationPreviousW = locationW;

		if (locationFirst == null)
			locationFirst = location;

		if (locationN < LOCATION_ARRAY_SIZE)
			locationN++;
		else {
			locationR++;
			if (locationR >= LOCATION_ARRAY_SIZE)
				locationR = 0;
		}

		locationW++;
		if (locationW >= LOCATION_ARRAY_SIZE)
			locationW = 0;

		locationPrint(mView, location, locationFirst());
	}

	void locationUpdate(Location location) {
		if (locationPrevious == null)
			locationAdd(location);
		else {
			if (locationFirst == locationPrevious)
				locationFirst = location;
			
			locationArray[locationPreviousW] = location;

			locationPrint(mView, location, locationFirst());
		}
	}

	public void onLocationChanged(Location location) {

		float dist_m, err_m, dt_ns;

		count++;

		if (locationPrevious == null) {
			locationAdd(location);

			return;
		}

		dist_m = location.distanceTo(locationPrevious);
		err_m = location.getAccuracy() + locationPrevious.getAccuracy();
		dt_ns = location.getElapsedRealtimeNanos() - locationPrevious.getElapsedRealtimeNanos();

		if ((location.getAccuracy() < locationPrevious.getAccuracy()) && (dist_m < locationPrevious.getAccuracy())) {
			locationUpdate(location);
			return;
		}

		// No more than one update per second
		if (dt_ns < LOCATION_MIN_UPDATE_PERIOD_NS)
			return;

		// Wait until distance is above accuracy
		if (dist_m < err_m)
			return;

		locationAdd(location);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}
}


public class MainActivity extends Activity {

	private TextView textView1, textView2;
	private CheckBox checkbox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		textView1 = (TextView) findViewById(R.id.text1);
		textView2 = (TextView) findViewById(R.id.text2);
		checkbox = (CheckBox) findViewById(R.id.notification_toggle);

		textView1.setText("OnCreate\n");

		new LocationHandler(this, textView2);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// The activity is about to become visible.

		textView1.append("OnStart\n");

		Intent intent = new Intent(this, MainService.class);

		this.startService(intent);
		checkbox.setChecked(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").

		textView1.append("OnResume\n");
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Another activity is taking focus (this activity is about to be "paused").

		textView1.append("OnPause\n");
	}

	@Override
	protected void onStop() {
		super.onStop();
		// The activity is no longer visible (it is now "stopped")

		textView1.append("OnStop\n");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.

		textView1.append("OnDestroy\n");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		textView1.append("OnSaveInstanceState\n");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		textView1.append("onRestoreInstanceState\n");
	}

	public void onCheckboxClicked(View view) {
		// Is the view now checked?
		boolean isChecked = ((CheckBox) view).isChecked();

		Intent intent = new Intent(this, MainService.class);

		if (isChecked) {
			textView2.setText("on\n");
			this.startService(intent);
		} else {
			textView2.setText("off\n");
			this.stopService(intent);
		}

/*
		// Check which checkbox was clicked
		switch(view.getId()) {
			case R.id.checkbox_meat:
				if (checked)
				// Put some meat on the sandwich
				else
				// Remove the meat
				break;
			case R.id.checkbox_cheese:
				if (checked)
				// Cheese me
				else
				// I'm lactose intolerant
				break;
			// TODO: Veggie sandwich
		}
*/
	}
}
