package com.zzb.rxlocationhelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    void test(){
        RxLocationHelper helper = RxLocationHelper.newBuilder().updateInterval(1000).keepTracing(true).build();

    }
}
