package com.zzb.rxlocationhelper.location;

import android.location.Location;

/**
 * Created by ZZB on 2016/10/18.
 */

public interface RequestLocationCallback {

    void onLocationChanged(Location location);

    void onGetLocationFailed();
}
