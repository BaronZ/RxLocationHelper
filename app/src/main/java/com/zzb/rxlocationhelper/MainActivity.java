package com.zzb.rxlocationhelper;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.zzb.rxlocationhelper.location.RxLocationHelper;
import com.zzb.rxlocationhelper.util.Logger;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RxLocationHelper mLocationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationHelper = RxLocationHelper.newBuilder(this)
                .forceUpdateOnRequest(false).keepTracing(true)
                .updateDistanceInMeters(1).tryOtherProviderOnFailed(true)
                .build();
    }


    @Override
    public void onClick(View v) {
        mLocationHelper.requestLocation().subscribe(new Action1<Location>() {
            @Override
            public void call(Location location) {
                Logger.debug("activity", location.toString());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Logger.error("activity", throwable.getMessage());
            }
        });
    }
}
