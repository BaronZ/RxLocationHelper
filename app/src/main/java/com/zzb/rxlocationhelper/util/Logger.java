package com.zzb.rxlocationhelper.util;

/**
 * Created by ZZB on 2016/10/19.
 */

public class Logger {

    public static void debug(String tag, String msg) {
        print(tag, msg);
    }

    public static void error(String tag, Throwable throwable) {
        print(tag, throwable.toString());
    }

    public static void error(String tag, String msg) {
        print(tag, msg);
    }
    private static void print(String tag, String msg){
        System.out.println("tag:" + tag + "   msg:" + msg);
    }
}
