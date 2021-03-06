package com.zzb.rxlocationhelper.location;

import android.content.Context;
import android.location.Location;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * 使用：
 * RxLocationHelper helper = RxLocationHelper.newBuilder(context).build();
 * helper.requestLocation().subscribe(/handle on success and on failed/);
 * <p>
 * keepTracing为true, 不需要的时候要调用stopTracing，不然可能导致内存泄露
 * Created by ZZB on 2016/10/17.
 */

public class RxLocationHelper {
    private static final String TAG = "RxLocationHelper";
    private PublishSubject<Location> mLocationPublishSubject = PublishSubject.create();
    private LocationModel mLocationModel;
    private RequestLocationCallback mRequestLocationCallback;

    private RxLocationHelper(Builder builder) {
        mRequestLocationCallback = new RequestLocationCallback() {
            @Override
            public void onLocationChanged(Location location) {
                onGetLocationSuccess(location);
            }

            @Override
            public void onGetLocationFailed() {
                RxLocationHelper.this.onGetLocationFailed();
            }
        };

        mLocationModel = new LocationModel(builder, mRequestLocationCallback);
    }


    public Observable<Location> requestLocation() {
        mLocationModel.requestLocationUpdate();
        return mLocationPublishSubject;
    }

    public void stopTracing() {
        mLocationModel.stopTracing();
    }

    public static Builder newBuilder(Context appContext) {
        return new Builder(appContext);
    }

    private void onGetLocationSuccess(Location location) {
        if (location != null) {
            mLocationPublishSubject.onNext(location);
        } else {
            mLocationPublishSubject.onError(new Exception("get location failed"));
        }
    }

    private void onGetLocationFailed() {
        mLocationPublishSubject.onError(new Exception("get location failed"));
    }

    public static class Builder {
        private Context mContext;
        private boolean keepTracing = false;
        private long updateTimeIntervalInMillis;
        private int updateDistanceInMeters;
        private boolean tryOtherProviderOnFailed = true;
        private boolean forceUpdateOnRequest = false;
        private ILocationManager mLocationManagerForTesting;
        private long requestUpdateTimeoutInMillis = 4000;//默认4秒没拿到地址超时

        public Builder(Context appContext) {
            mContext = appContext.getApplicationContext();
        }

        //是否一直监听距离变化
        public Builder keepTracing(boolean keepTracing) {
            this.keepTracing = keepTracing;
            return this;
        }

        //时间间隔更新
        public Builder updateTimeInterval(long millis) {
            this.updateTimeIntervalInMillis = millis;
            return this;
        }

        //距离变动更新
        public Builder updateDistanceInMeters(int meters) {
            this.updateDistanceInMeters = meters;
            return this;
        }

        //是不是在一个Provider失败的时候，调用另外一个provider
        public Builder tryOtherProviderOnFailed(boolean tryOtherProvider) {
            this.tryOtherProviderOnFailed = tryOtherProvider;
            return this;
        }

        //是不是在请求更新的时候，不用缓存的位置
        public Builder forceUpdateOnRequest(boolean forceUpdate) {
            this.forceUpdateOnRequest = forceUpdate;
            return this;
        }

        //请求超时，如果超时没有回调，强行回调onUpdateLocationFailed
        public void requestUpdateTimeoutInMillis(long requestUpdateTimeoutInMillis) {
            this.requestUpdateTimeoutInMillis = requestUpdateTimeoutInMillis;
        }

        //测试用的，正常代码不要调用
        public Builder locationManagerForTesting(ILocationManager locationManager) {
            mLocationManagerForTesting = locationManager;
            return this;
        }

        Context getContext() {
            return mContext;
        }

        boolean isKeepTracing() {
            return keepTracing;
        }

        long getUpdateTimeIntervalInMillis() {
            return updateTimeIntervalInMillis;
        }

        int getUpdateDistanceInMeters() {
            return updateDistanceInMeters;
        }

        boolean isTryOtherProviderOnFailed() {
            return tryOtherProviderOnFailed;
        }

        boolean isForceUpdateOnRequest() {
            return forceUpdateOnRequest;
        }

        long getRequestUpdateTimeoutInMillis() {
            return requestUpdateTimeoutInMillis;
        }

        public ILocationManager getLocationManagerForTesting() {
            return mLocationManagerForTesting;
        }

        public RxLocationHelper build() {
            RxLocationHelper helper = new RxLocationHelper(this);
            return helper;
        }
    }
}
