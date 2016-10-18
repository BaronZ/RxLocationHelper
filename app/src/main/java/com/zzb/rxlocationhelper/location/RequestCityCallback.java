package com.zzb.rxlocationhelper.location;

/**
 * Created by ZZB on 2016/10/18.
 */

public interface RequestCityCallback {

    void onGetCitySuccess(String city);
    void onGetCityFailed();
}
