package com.zzb.rxlocationhelper.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.zzb.rxlocationhelper.location.RxLocationHelper.Builder;
import com.zzb.rxlocationhelper.util.FP;
import com.zzb.rxlocationhelper.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ZZB on 2016/10/18.
 */

public class LocationModel implements LocationListener {
    private static final String TAG = "LocationModel";
    //默认距离变化更新经纬度
    private static final int DEFAULT_UPDATE_DISTANCE_IN_METERS = Integer.MAX_VALUE;
    //默认时间间隔更新经纬度
    private static final long DEFAULT_UPDATE_TIME_INTERVAL = Long.MAX_VALUE;
    private static final String[] FALLBACK_PROVIDERS = new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER};
    private Set<String> mFailedProviders = new HashSet<>();
    private int mFallbackProviderIndex = 0;
    private RxLocationHelper.Builder mBuilder;
    private RequestLocationCallback mRequestLocationCallback;
    private ILocationManager mLocationManager;
    private boolean mIsTracingLocation;
    private String mProvider;
    private Location mCacheLocation;
    private List<String> mAllProviders = new ArrayList<>();

    public LocationModel(Builder builder, RequestLocationCallback requestLocationCallback) {
        mBuilder = builder;
        mRequestLocationCallback = requestLocationCallback;
        initLocation(builder.getContext().getApplicationContext());

    }

    private void initLocation(Context context) {
        mLocationManager = getLocationManager(context);
        List<String> allProviders = mLocationManager.getAllProviders();
        if (allProviders != null) {
            mAllProviders.addAll(allProviders);
        }
    }

    public void requestLocationUpdate() {
        Logger.debug(TAG, "requestLocationUpdate");
        if (!mIsTracingLocation) {
            _requestLocationUpdate(getFirstProvider());
        }

    }

    private void _requestLocationUpdate(String provider) {
        Logger.debug(TAG, "_requestLocationUpdate");
        if (mIsTracingLocation) {
            Logger.debug(TAG, "_requestLocationUpdate, is tracing, return");
            return;
        }
        mIsTracingLocation = true;
        if (FP.empty(provider)) {
            mIsTracingLocation = false;
            onUpdateLocationFailed(provider);
            return;
        }
        boolean isRequestUpdateSuccess = false;
        if (canUseCache()) {
            onLocationChanged(mCacheLocation);
            return;
        } else {
            mCacheLocation = null;
        }
        try {
            mLocationManager.requestLocationUpdates(provider, getUpdateTimeInterval(), getUpdateDistance(), this);
            isRequestUpdateSuccess = true;
        } catch (Exception e) {
            Logger.error(TAG, e);
            isRequestUpdateSuccess = false;
        }
        if (isRequestUpdateSuccess) {
//            mIsTracingLocation = true;
        } else {
            mIsTracingLocation = false;
            onUpdateLocationFailed(provider);
        }

    }

    public void stopTracing() {
        mIsTracingLocation = false;
        mLocationManager.removeUpdates(this);
    }

    private boolean canUseCache() {
        return !mBuilder.isForceUpdateOnRequest() && mCacheLocation != null;
    }


    @Override
    public void onLocationChanged(Location location) {
        Logger.debug(TAG, "onLocationChanged");
        if (!mIsTracingLocation) {
            Logger.debug(TAG, "onLocationChanged but not tracing");
            return;
        }
        if (location != null) {
            Logger.debug(TAG, "onLocationChanged, location:" + location.toString());
            mCacheLocation = location;
            mRequestLocationCallback.onLocationChanged(location);
        } else {
            Logger.error(TAG, "onLocationChanged failed");
            onUpdateLocationFailed(null);
        }
        mIsTracingLocation = false;
    }

    //获取位置失败
    private void onUpdateLocationFailed(String failedProvider) {
        Logger.error(TAG, "onUpdateLocationFailed");
        mFailedProviders.add(failedProvider);
        boolean hasTryOtherProvider = tryToUpdateByOtherProviders();
        if (!hasTryOtherProvider) {
            mRequestLocationCallback.onGetLocationFailed();
            mIsTracingLocation = false;
        }
    }

    /**
     * @return true 有用其他Provider重新请求Location, 否则返回false
     */
    private boolean tryToUpdateByOtherProviders() {
        if (!mBuilder.isTryOtherProviderOnFailed()) {
            return false;
        }
        String provider = getNextValidProvider();
        if (FP.empty(provider)) {
            return false;
        } else {
            _requestLocationUpdate(getNextValidProvider());
            return true;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Logger.debug(TAG, String.format("onStatusChanged, provider: %s, status: %d", provider, status));
        if (status == LocationProvider.OUT_OF_SERVICE) {
            clearIfCacheProviderNotWork(provider);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Logger.debug(TAG, String.format("onStatusChanged, provider: %s", provider));
    }

    @Override
    public void onProviderDisabled(String provider) {
        Logger.debug(TAG, String.format("onProviderDisabled, provider: %s", provider));
        clearIfCacheProviderNotWork(provider);
    }

    //如果之前有效的provider被禁用了，清除provider缓存
    private void clearIfCacheProviderNotWork(String notWorkProvider) {
        Logger.debug(TAG, String.format("clearIfCacheProviderWorks, notWorkProvider: %s", notWorkProvider));
        if (FP.eq(notWorkProvider, mProvider)) {
            mProvider = null;
        }
    }

    private int getUpdateDistance() {
        return mBuilder.getUpdateDistanceInMeters() > 0 ? mBuilder.getUpdateDistanceInMeters() : DEFAULT_UPDATE_DISTANCE_IN_METERS;
    }

    private long getUpdateTimeInterval() {
        return mBuilder.getUpdateTimeIntervalInMillis() > 0 ? mBuilder.getUpdateTimeIntervalInMillis() : DEFAULT_UPDATE_TIME_INTERVAL;
    }

    private String getNextValidProvider() {
        if (!hasValidProviders()) {
            return null;
        } else {
            if (mFallbackProviderIndex < FALLBACK_PROVIDERS.length) {
                boolean isGetIndexSuccess = true;
                while (!isValidProvider(FALLBACK_PROVIDERS[mFallbackProviderIndex]) || mFailedProviders.contains(FALLBACK_PROVIDERS[mFallbackProviderIndex])) {
                    mFallbackProviderIndex++;
                    if (mFallbackProviderIndex >= FALLBACK_PROVIDERS.length) {
                        isGetIndexSuccess = false;
                        break;
                    }
                }
                if (isGetIndexSuccess) {
                    return FALLBACK_PROVIDERS[mFallbackProviderIndex];
                }
            }
            return null;
        }

    }

    private boolean isValidProvider(String provider) {
        return mAllProviders.contains(provider);
    }

    @Nullable//第一次请求使用的Provider
    private String getFirstProvider() {
        if (!hasValidProviders()) {
            return null;
        } else {
            String bestProvider = getBestProvider();
            if (isValidProvider(bestProvider)) {
                return bestProvider;
            } else {
                return getNextValidProvider();
            }
        }
    }

    //本机是不是有可用的providers
    private boolean hasValidProviders() {
        return mAllProviders != null && mAllProviders.size() > 0;
    }

    @Nullable
    public String getBestProvider() {
        String bestProvider = mLocationManager.getBestProvider();
        Logger.debug(TAG, "getBestProvider: " + bestProvider);
        return bestProvider;
    }

    private ILocationManager getLocationManager(Context context) {
        ILocationManager testingManager = mBuilder.getLocationManagerForTesting();
        return testingManager == null ? new LocationManagerWrapper(context) : testingManager;
    }
}
