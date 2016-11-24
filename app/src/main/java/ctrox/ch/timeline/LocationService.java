package ctrox.ch.timeline;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Location IntentService
 * Gets called on location updates and stores it in db
 */

public class LocationService extends IntentService {
  public static final String ACTION = "ACTION_LOCATION";
  private static final String TAG_LOCATIONTRACKER = "LocationTracker";
  private Database mDatabase;

  public LocationService() {
    super(LocationService.class.getName());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mDatabase = setupCouchbase();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent.getAction().equals(ACTION) && intent.getExtras() != null) {
      Log.i(TAG_LOCATIONTRACKER, "Received location update");
      Bundle bundle = intent.getExtras();
      Location location = (Location) bundle.get(android.location.LocationManager
              .KEY_LOCATION_CHANGED);
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
