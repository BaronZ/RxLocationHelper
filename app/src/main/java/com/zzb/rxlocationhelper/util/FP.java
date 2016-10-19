package com.zzb.rxlocationhelper.util;

/**
 * Created by ZZB on 2016/10/19.
 */

public class FP {

    public static boolean empty(CharSequence s) {
        return s == null || s.length() == 0;
    }
    public static boolean eq(Object a, Object b) {
        if (a == null && b == null)
            return true;
        else if (a == null)
            return false;
        else
            return a.equals(b);
    }
}
