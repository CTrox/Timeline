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
  private Double sum;

  LatLng getLatLng() {
    return latLng;
  }

  void setLatLng(LatLng latLng) {
    this.latLng = latLng;
    this.sum = latLng.latitude + latLng.longitude;
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

  public Double getSum() {
    return sum;
  }

}
