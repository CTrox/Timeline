package ctrox.ch.timeline;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Registers location updates on boot (if allowed)
 */

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i("BootReceiver", "received boot broadcast");
    Intent serviceIntent = new Intent(context, LocationService.class);
    serviceIntent.setAction(LocationService.ACTION);
    PendingIntent pendingIntent = PendingIntent.getService(context, 1,
            serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    LocationManager locationManager = (LocationManager) context.getSystemService(Context
            .LOCATION_SERVICE);
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
      Log.i("BootReceiver", "registering location updates");
      locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
              LocationService.MIN_TIME, LocationService.MIN_DISTANCE, pendingIntent);
    }
  }
}
