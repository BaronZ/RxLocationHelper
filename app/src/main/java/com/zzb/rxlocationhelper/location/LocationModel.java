package com.zzb.rxlocationhelper.location;

import com.zzb.rxlocationhelper.location.RxLocationHelper.Builder;

/**
 * Created by ZZB on 2016/10/18.
 */

public class LocationModel {
    private RxLocationHelper.Builder mBuilder;
    private RequestLocationCallback mRequestLocationCallback;

    public LocationModel(Builder builder, RequestLocationCallback requestLocationCallback) {
        mBuilder = builder;
        mRequestLocationCallback = requestLocationCallback;
    }

    private void initLocation() {

    }

    public void requestLocationUpdate() {

    }
}
