package com.zzb.rxlocationhelper.location;

import android.content.Context;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;

import java.util.List;

/**
 * Created by ZZB on 2016/10/18.
 */

public class LocationManagerWrapper implements ILocationManager {

    private Context mContext;
    private LocationManager mLocationManager;

    public LocationManagerWrapper(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }


    @Override
    public void requestLocationUpdates(String provider, long updateTime, int updateDistance, LocationListener listener) {
        mLocationManager.requestLocationUpdates(provider, updateTime, updateDistance, listener);
    }

    @Override
    public List<String> getAllProviders() {
        return mLocationManager.getAllProviders();
    }



    @Override
    public void removeUpdates(LocationListener listener) {
        mLocationManager.removeUpdates(listener);
    }

    @Override
    public String getBestProvider() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//高精度
        criteria.setAltitudeRequired(false);//不要求海拔
        criteria.setBearingRequired(false);//不要求方位
        criteria.setCostAllowed(false);//是否允许运营商计费
        criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗
        criteria.setSpeedRequired(false);// 是否提供速度信息
        String locationProvider = mLocationManager.getBestProvider(criteria, true);
        return locationProvider;
    }
}
