package com.zzb.rxlocationhelper;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.zzb.rxlocationhelper.location.RxLocationHelper;
import com.zzb.rxlocationhelper.util.Logger;

import java.util.Date;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RxLocationHelper mLocationHelper;
    private TextView mTvContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvContent = (TextView) findViewById(R.id.tv);
        mLocationHelper = RxLocationHelper.newBuilder(this)
                .forceUpdateOnRequest(false).keepTracing(true)
                .updateDistanceInMeters(1).tryOtherProviderOnFailed(true)
                .build();
    }


    @Override
    public void onClick(View v) {
        counter = 0;
        request();
//        request();
//        request();
//        request();
    }

    private void request() {
        mLocationHelper.requestLocation().subscribe(new Action1<Location>() {
            @Override
            public void call(Location location) {

                updateText(location.toString());
                Logger.debug("activity", location.toString());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                updateText(throwable.getMessage());
                Logger.error("activity", throwable.getMessage());
            }
        });
    }

    private StringBuilder mSbText = new StringBuilder();
    private int counter;
    private void updateText(String text){
        counter++;
        Logger.debug("counter", counter + "");
        mSbText.append("location update time:").append(new Date()).append("\n");
        mSbText.append(text).append("\n");
        mTvContent.setText(mSbText.toString());
    }
}
