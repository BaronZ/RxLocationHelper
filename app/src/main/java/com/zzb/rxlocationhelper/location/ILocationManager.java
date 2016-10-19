package com.zzb.rxlocationhelper.location;

import android.location.LocationListener;

import java.util.List;

/**
 * Created by ZZB on 2016/10/18.
 */

public interface ILocationManager {

    void requestLocationUpdates(String provider, long updateTime, int updateDistance, LocationListener listener);
    List<String> getAllProviders();
//    String getBestProvider(Criteria criteria, boolean enabledOnly);
    void removeUpdates(LocationListener listener);
    String getBestProvider();
}
