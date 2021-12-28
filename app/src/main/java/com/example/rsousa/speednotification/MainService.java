package com.example.rsousa.speednotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class MainService extends Service {
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location locationLast;

	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;
	private int id = 1;
	private int n = 0;
	private float speed_km_h = 0;
	private float err_speed_km_h = 0;
	static float ERR_LOW_LIMIT = .5f;
	static float ERR_HIGH_LIMIT = .01f;
	static float DT_LOW_LIMIT = 2;
	static float DT_HIGH_LIMIT = 15;
	static float DIST_LOW_LIMIT = 10;
	static float DIST_HIGH_LIMIT = 250;
	static float SEC_PER_HOUR = (60 * 60);
	static float M_PER_KM = 1000;
	static float NSEC_PER_SEC = 1000000000;
	static int LOCATION_ARRAY_SIZE = 16;
	static long LOCATION_MIN_UPDATE_PERIOD_NS = 1000000000;

	@Override
	public void onCreate() {
		// The service is being created
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent resultIntent = new Intent(this, MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		//stackBuilder = stackBuilder.addParentStack((Activity)MainActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		builder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.notification_icon)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setLocalOnly(true)
			.setContentTitle("Speed (km/h)")
			.setContentIntent(resultPendingIntent);

		notificationManager.notify(id, builder.build());

		locationLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {

			public void onLocationChanged(Location location) {

				/* No location set yet */
				if (locationLast == null) {
					locationLast = location;
					return;
				}

				float dist_m = location.distanceTo(locationLast);

				/* If this location has better accuracy, and is the same position (within the error margin), replace previous one */
				if ((location.getAccuracy() < locationLast.getAccuracy()) && (dist_m < locationLast.getAccuracy())) {
					locationLast = location;
					return;
				}

				float err_m = location.getAccuracy() + locationLast.getAccuracy();

				float dt_s = ((float) (location.getElapsedRealtimeNanos() - locationLast.getElapsedRealtimeNanos())) / NSEC_PER_SEC;

				if (((dist_m > DIST_LOW_LIMIT) && (dt_s > DT_LOW_LIMIT) && ((dist_m * ERR_LOW_LIMIT > err_m))) ||
					(dist_m > DIST_HIGH_LIMIT) || (dt_s > DT_HIGH_LIMIT) || (dist_m * ERR_HIGH_LIMIT > err_m)) {
					speed_km_h = (dist_m / M_PER_KM) / (dt_s / SEC_PER_HOUR);
					err_speed_km_h = (err_m / M_PER_KM) / (dt_s / SEC_PER_HOUR);

					if (speed_km_h * ERR_LOW_LIMIT < err_speed_km_h)
						speed_km_h = 0;

					locationLast = location;
				}

				String _speed;

				/* Hack to always get a different ticker (and make it display in the notification bar) */
				if ((n % 2) == 0)
					_speed = String.format("%.1f (%.1f)", speed_km_h, err_speed_km_h);
				else
					_speed = String.format("%.1f (%.1f) ", speed_km_h, err_speed_km_h);

				String _speed_full = String.format("%.1f %.1f %.1f %.1f %.1f (%.1f)",
					dist_m, err_m, locationLast.bearingTo(location), dt_s, speed_km_h, err_speed_km_h);

				builder.setTicker(_speed);
//				builder.setNumber(n);
				builder.setContentText(_speed_full);

				notificationManager.notify(id, builder.build());
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
//				String info = String.format("%s status %d", provider, status);

//				builder.setNumber(n);
//				builder.setContentInfo(info);

//				notificationManager.notify(id, builder.build());
//				n++;
			}

			public void onProviderEnabled(String provider) {
				String info = String.format("%s enabled", provider);

				builder.setContentText(info);

//				builder.setNumber(n);
//				builder.setContentInfo(info);

				notificationManager.notify(id, builder.build());
//				n++;
			}

			public void onProviderDisabled(String provider) {
				String info = String.format("%s disabled", provider);

				builder.setContentText(info);

//				builder.setNumber(n);
//				builder.setContentInfo(info);
//				builder.setContentText("");

				notificationManager.notify(id, builder.build());
//				n++;
			}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// A client is binding to the service with bindService()
		return null;
	}

	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed

		locationManager.removeUpdates(locationListener);

		notificationManager.cancel(id);
	}
}
