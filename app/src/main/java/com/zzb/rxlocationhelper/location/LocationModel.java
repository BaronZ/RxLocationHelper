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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.subjects.PublishSubject;

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
    private Location mCacheLocation;
    private List<String> mAllProviders = new ArrayList<>();
    private PublishSubject<Location> mLocationPublishSubject = PublishSubject.create();
    private String mLastUsefulProvider;//上一次可用的Provider
    private String mCurrentUsingProvider;//现在正在用的Provider

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
        } else {
            Logger.info(TAG, "requestLocationUpdate, is tracing location");
        }

    }

    public void stopTracing() {
        Logger.info(TAG, "stopTracing");
        setIsTracingLocation(mIsTracingLocation);
        mLocationManager.removeUpdates(this);
    }

    private void _requestLocationUpdate(String provider) {
        Logger.debug(TAG, "_requestLocationUpdate:" + provider);
        if (mIsTracingLocation) {
            Logger.debug(TAG, "_requestLocationUpdate, is tracing, return");
            return;
        }
        mCurrentUsingProvider = provider;
        setIsTracingLocation(true);
        subscribeLocationUpdate();
        if (FP.empty(provider)) {
//            onUpdateLocationFailed(provider);
            mLocationPublishSubject.onError(new TraceLocationException());
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
//            onUpdateLocationFailed(provider);
            mLocationPublishSubject.onError(new TraceLocationException());
        }

    }

    private void subscribeLocationUpdate() {
        long timeout = Math.max(0, mBuilder.getRequestUpdateTimeoutInMillis());
        mLocationPublishSubject.timeout(timeout, TimeUnit.MILLISECONDS)
                .subscribe(this::onSubscribeSuccess, this::onSubscribeFailed);
    }

    private void onSubscribeFailed(Throwable throwable) {
        Logger.error(TAG, "onSubscribeFailed:" + throwable);
        if (throwable instanceof TraceLocationException) {
            onUpdateLocationFailed(mCurrentUsingProvider);
        } else if (throwable instanceof TimeoutException) {//超时
            Logger.error(TAG, "request timeout");
            onUpdateLocationFailed(mCurrentUsingProvider);
        } else {
            onUpdateLocationFailed(mCurrentUsingProvider);
        }
    }

    private void onSubscribeSuccess(Location location) {
        mRequestLocationCallback.onLocationChanged(location);
        setIsTracingLocation(false);
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
            mLocationPublishSubject.onNext(location);
            mLastUsefulProvider = location.getProvider();
            setIsTracingLocation(false);
//            mRequestLocationCallback.onLocationChanged(location);
            stopTracingIfNeeded();
        } else {
            Logger.error(TAG, "onLocationChanged failed");
            mLocationPublishSubject.onError(new TraceLocationException());
//            onUpdateLocationFailed(null);
        }
    }

    //获取位置失败
    private void onUpdateLocationFailed(String failedProvider) {
        Logger.error(TAG, "onUpdateLocationFailed, provider:" + failedProvider);
        if (!mIsTracingLocation) {
            Logger.info(TAG, "onUpdateLocationFailed, not tracing");
            return;
        }
        mFailedProviders.add(failedProvider);
        setIsTracingLocation(false);
        boolean hasTryOtherProvider = tryToUpdateByOtherProviders();
        if (!hasTryOtherProvider) {
            mFallbackProviderIndex = 0;
            mRequestLocationCallback.onGetLocationFailed();
            stopTracingIfNeeded();
        }
    }

    private void stopTracingIfNeeded() {
        if (!mBuilder.isKeepTracing()) {
            stopTracing();
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
            _requestLocationUpdate(provider);
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
        Logger.debug(TAG, String.format("onProviderDisabled, provider: %s, currentUsingProvider: %s", provider, mCurrentUsingProvider));
        clearIfCacheProviderNotWork(provider);
        mLocationPublishSubject.onError(new TraceLocationException());
    }

    //如果之前有效的provider被禁用了，清除provider缓存
    private void clearIfCacheProviderNotWork(String notWorkProvider) {
        Logger.debug(TAG, String.format("clearIfCacheProviderWorks, notWorkProvider: %s", notWorkProvider));
        if (FP.eq(notWorkProvider, mLastUsefulProvider)) {
            mLastUsefulProvider = null;
        }
    }

    private int getUpdateDistance() {
        return mBuilder.getUpdateDistanceInMeters() > 0 ? mBuilder.getUpdateDistanceInMeters() : DEFAULT_UPDATE_DISTANCE_IN_METERS;
    }

    private long getUpdateTimeInterval() {
        return mBuilder.getUpdateTimeIntervalInMillis() > 0 ? mBuilder.getUpdateTimeIntervalInMillis() : DEFAULT_UPDATE_TIME_INTERVAL;
    }

    private String getNextValidProvider() {
        Logger.info(TAG, "getNextValidProvider, index:" + mFallbackProviderIndex);
        if (!hasValidProviders()) {
            return null;
        } else {
            if (mFallbackProviderIndex < FALLBACK_PROVIDERS.length) {
                boolean isGetIndexSuccess = true;
                //如果不是有效的provider或者已经是失败的provider，就取下一个provider
                while (!isValidProvider(FALLBACK_PROVIDERS[mFallbackProviderIndex]) || mFailedProviders.contains(FALLBACK_PROVIDERS[mFallbackProviderIndex])) {
                    mFallbackProviderIndex++;
                    Logger.info(TAG, "fallback index:" + mFallbackProviderIndex);
                    if (mFallbackProviderIndex >= FALLBACK_PROVIDERS.length) {
                        isGetIndexSuccess = false;
                        break;
                    }
                }
                if (isGetIndexSuccess) {
                    String fallbackProvider = FALLBACK_PROVIDERS[mFallbackProviderIndex];
                    mFallbackProviderIndex++;
                    return fallbackProvider;
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
            if (!FP.empty(mLastUsefulProvider)) {
                return mLastUsefulProvider;
            }
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

    private void setIsTracingLocation(boolean isTracingLocation) {
        Logger.info(TAG, "setIsTracingLocation: " + isTracingLocation);
        mIsTracingLocation = isTracingLocation;
    }
}
