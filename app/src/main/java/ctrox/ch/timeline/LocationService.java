package ctrox.ch.timeline;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by cyrill on 11/19/16.
 */

public class LocationService extends Service {
  private LocationManager mLocationManager;
  private Database mDatabase;

  private static final String TAG_LOCATIONTRACKER = "LocationTracker";

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    mLocationManager = (LocationManager) this.getSystemService
            (Context.LOCATION_SERVICE);
    mDatabase = setupCouchbase();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    LocationListener locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(final android.location.Location location) {
        Log.i(TAG_LOCATIONTRACKER, "Location changed, lat: " + location.getLatitude() + " long: "
                + location.getLongitude());
        // Insert location into document
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("timestamp", location.getTime());
        properties.put("latitude", location.getLatitude());
        properties.put("longitude", location.getLongitude());
        properties.put("accuracy", location.getAccuracy());
        Document document = mDatabase.createDocument();
        try {
          document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.i(TAG_LOCATIONTRACKER, "Location status changed!");
      }

      @Override
      public void onProviderEnabled(String s) {
        Log.i(TAG_LOCATIONTRACKER, "Location provider enabled!");
      }

      @Override
      public void onProviderDisabled(String s) {
        Log.i(TAG_LOCATIONTRACKER, "Location provider disabled!");
      }
    };
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
      Log.e(TAG_LOCATIONTRACKER, "Location Permission unavailable");
    } else {
      mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
    }

    return START_STICKY;
  }

  private Database setupCouchbase() {
    String dbname = "location";
    DatabaseOptions options = new DatabaseOptions();
    options.setCreate(true);

    Manager manager = null;
    try {
      manager = new Manager(new AndroidContext(getApplicationContext()), Manager.DEFAULT_OPTIONS);
    } catch (IOException e) {
      e.printStackTrace();
    }
    Database database = null;
    try {
      database = manager.openDatabase(dbname, options);
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }
    return database;
  }

}
