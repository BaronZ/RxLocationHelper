package com.zzb.rxlocationhelper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zzb.rxlocationhelper.location.RxLocationHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    void test() {
        RxLocationHelper helper = RxLocationHelper.newBuilder(this).updateTimeInterval(1000).keepTracing(true).build();

    }
}
