package ctrox.ch.timeline;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Location Record Class
 */

class LocationRecord {
  private LatLng latLng;
  private Date datetime;
  private Double accuracy;

  LatLng getLatLng() {
    return latLng;
  }

  void setLatLng(LatLng latLng) {
    this.latLng = latLng;
  }

  Date getDatetime() {
    return datetime;
  }

  void setDatetime(Date datetime) {
    this.datetime = datetime;
  }

  void setDatetime(Long timestamp) {
    this.datetime = new Date(timestamp);
  }

  Double getAccuracy() {
    return accuracy;
  }

  void setAccuracy(Double accuracy) {
    this.accuracy = accuracy;
  }
}
