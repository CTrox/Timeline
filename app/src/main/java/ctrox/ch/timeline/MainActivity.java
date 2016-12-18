package ctrox.ch.timeline;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {
  private static final String TAG = "MainActivity";
  // minimum timeout for location updates
  private static final int MIN_TIME = 0;
  // minimum distance to trigger location updates in meters
  private static final int MIN_DISTANCE = 10;
  private static final int FINE_LOCATION_REQUEST = 242;
  private boolean isExpanded = false;
  private Database mDatabase;
  GoogleMap mMap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Context context = this;
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    final MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    mDatabase = setupCouchbase();
    setupView(mDatabase);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (checkPermission()) {
          Snackbar.make(view, "Tracking Location...", Snackbar.LENGTH_LONG)
                  .setAction("Action", null).show();
          Log.i(TAG, "registering location updates");
          Intent serviceIntent = new Intent(context, LocationService.class);
          serviceIntent.setAction(LocationService.ACTION);
          PendingIntent pendingIntent = PendingIntent.getService(context, 1,
                  serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
          LocationManager locationManager = (LocationManager) context.getSystemService(Context
                  .LOCATION_SERVICE);
          locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MIN_TIME, MIN_DISTANCE,
                  pendingIntent);
        }
      }
    });

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.setDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    RelativeLayout datePickerButton = (RelativeLayout) findViewById(R.id.date_picker_button);
    final ImageView arrow = (ImageView) findViewById(R.id.date_picker_arrow);
    final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
    datePickerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleCalendar(appBarLayout, arrow);
      }
    });

    // Set up the CompactCalendarView
    CompactCalendarView compactCalendarView = (CompactCalendarView) findViewById(R.id
            .compactcalendar_view);
    compactCalendarView.setShouldDrawDaysHeader(true);
    compactCalendarView.setCurrentDate(new Date());
    compactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
      @Override
      public void onDayClick(Date dateClicked) {
        toggleCalendar(appBarLayout, arrow);
        if (mMap != null) {
          displayLocationHistory(mMap, getLocationRecords(mDatabase,
                  getStartOfDay(dateClicked).getTime(), getEndOfDay(dateClicked).getTime()));
        }
      }

      @Override
      public void onMonthScroll(Date firstDayOfNewMonth) {
        // TODO
      }

    });
  }

  private boolean checkPermission() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              FINE_LOCATION_REQUEST);
    } else {
      return true;
    }
    return false;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  private void toggleCalendar(AppBarLayout appBarLayout, ImageView arrow) {
    if (isExpanded) {
      ViewCompat.animate(arrow).rotation(0).start();
      appBarLayout.setExpanded(false, true);
      isExpanded = false;
    } else {
      ViewCompat.animate(arrow).rotation(180).start();
      appBarLayout.setExpanded(true, true);
      isExpanded = true;
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

  private void setupView(Database database) {
    com.couchbase.lite.View locationView = database.getView("locations");
    locationView.setMap(new Mapper() {
      @Override
      public void map(Map<String, Object> document, Emitter emitter) {
        emitter.emit(document.get("timestamp"), null);
      }
    }, "6.0");
  }

  @Override
  public void onMapReady(GoogleMap map) {
    mMap = map;
    // Get location LatLng's from DB and display on map
    List<LocationRecord> locationRecords = getLocationRecords(mDatabase, getStartOfDay(new Date()).getTime()
            , getEndOfDay(new Date()).getTime());
    displayLocationHistory(map, locationRecords);
  }


  private List<LocationRecord> getLocationRecords(Database database, Long startTime, Long endTime) {
    Query query = database.getView("locations").createQuery();
    query.setStartKey(startTime);
    query.setEndKey(endTime);
    QueryEnumerator result = null;
    try {
      result = query.run();
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }
    List<LocationRecord> locationList = new ArrayList<>();
    for (Iterator<QueryRow> it = result; it.hasNext(); ) {
      LocationRecord locationRecord = new LocationRecord();
      QueryRow row = it.next();
      Document document = row.getDocument();
      Object timestamp = document.getProperty("timestamp");
      Object latitude = document.getProperty("latitude");
      Object longitude = document.getProperty("longitude");
      Object accuracy = document.getProperty("accuracy");
      // check if record exists
      if (timestamp != null) {
        locationRecord.setDatetime((long)timestamp);
        locationRecord.setLatLng(new LatLng((double)latitude, (double)longitude));
        locationRecord.setAccuracy((double)accuracy);
        locationList.add(locationRecord);
      }
    }
    return locationList;
  }

  private void displayLocationHistory(GoogleMap map, List<LocationRecord> locationList) {
    // Clear current lines and points
    map.clear();
    List<LatLng> points = new ArrayList<>();
    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    for (LocationRecord location : locationList) {
      points.add(location.getLatLng());
      builder.include(location.getLatLng());
    }
    Polyline line = map.addPolyline(new PolylineOptions()
            .width(10)
            .color(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent)));
    line.setPoints(points);

    if (!locationList.isEmpty()) {
      // get size of display for camera
      DisplayMetrics displaymetrics = new DisplayMetrics();
      getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
      // zoom camera so all location records are shown
      map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), (int) (displaymetrics
                .widthPixels * 0.8), (int) (displaymetrics.heightPixels * 0.8), 0));
    }
  }

  public Date getEndOfDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return calendar.getTime();
  }

  public Date getStartOfDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case FINE_LOCATION_REQUEST: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Intent serviceIntent = new Intent(this, LocationService.class);
          startService(serviceIntent);
        } else {

          // permission denied, boo! Disable the
          // functionality that depends on this permission.
        }
        return;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }
  }
  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.nav_settings) {

    } else if (id == R.id.nav_share) {

    }
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }
}
