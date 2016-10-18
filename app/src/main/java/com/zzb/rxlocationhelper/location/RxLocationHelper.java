package com.zzb.rxlocationhelper.location;

import android.content.Context;
import android.location.Location;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by ZZB on 2016/10/17.
 */

public class RxLocationHelper implements RequestLocationCallback {
    private static final String TAG = "RxLocationHelper";
    private PublishSubject<Location> mLocationPublishSubject = PublishSubject.create();
    private LocationModel mLocationModel;

    private RxLocationHelper(Builder builder) {
        mLocationModel = new LocationModel(builder, this);
    }

    public Observable<Location> requestLocation() {
        mLocationModel.requestLocationUpdate();
        return mLocationPublishSubject;
    }

    public static Builder newBuilder(Context appContext) {
        return new Builder(appContext);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLocationPublishSubject.onNext(location);
        } else {
            mLocationPublishSubject.onError(new Exception("get location failed"));
        }
    }

    @Override
    public void onGetLocationFailed() {
        mLocationPublishSubject.onError(new Exception("get location failed"));
    }


    public static class Builder {
        private Context mContext;
        private boolean keepTracing;
        private long updateTimeIntervalInMillis;
        private int updateDistanceInMeters;

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

        public RxLocationHelper build() {
            RxLocationHelper helper = new RxLocationHelper(this);
            return helper;
        }
    }
}
