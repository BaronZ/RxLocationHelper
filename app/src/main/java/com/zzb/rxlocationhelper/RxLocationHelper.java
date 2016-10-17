package com.zzb.rxlocationhelper;

/**
 * Created by ZZB on 2016/10/17.
 */

public class RxLocationHelper {
    private Builder mBuilder;

    public RxLocationHelper(Builder builder) {
        mBuilder = builder;
    }


    public

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private boolean keepTracing;
        private long updateIntervalInMillis;

        public Builder keepTracing(boolean keepTracing) {
            this.keepTracing = keepTracing;
            return this;
        }

        public Builder updateInterval(long updateIntervalInMillis) {
            this.updateIntervalInMillis = updateIntervalInMillis;
            return this;
        }

        public RxLocationHelper build() {
            RxLocationHelper helper = new RxLocationHelper(this);
            return helper;
        }
    }
}
