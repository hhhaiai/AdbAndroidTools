package me.hhhaiai.utils;

import android.text.TextUtils;
import android.util.Log;

import me.hhhaiai.utils.ref.ContentHolder;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2022-11-03 11:52:42
 * @author: sanbo
 */
public final class Alog {

    private static String TAG = "sanbo";

    private Alog() {
    }

    public static void cmd(String cmd) {
        i("[ADB CMD] " + cmd);
    }



    public static int v(String msg) {
        return println(Log.VERBOSE, TAG, msg);
    }

    public static int v(String tag, String msg) {
        return println(Log.VERBOSE, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return println(Log.VERBOSE, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int d(String msg) {
        return println(Log.DEBUG, TAG, msg);
    }

    public static int d(String tag, String msg) {
        return println(Log.DEBUG, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return println(Log.DEBUG, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int i(String msg) {
        return println(Log.INFO, TAG, msg);
    }

    public static int i(String tag, String msg) {
        return println(Log.INFO, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return println(Log.INFO, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String msg) {
        return println(Log.WARN, TAG, msg);
    }

    public static int w(Throwable msg) {
        return println(Log.WARN, TAG, Log.getStackTraceString(msg));
    }

    public static int w(String tag, String msg) {
        return println(Log.WARN, tag, msg);
    }


    public static int w(String tag, String msg, Throwable tr) {
        return println(Log.WARN, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        return println(Log.WARN, tag, Log.getStackTraceString(tr));
    }

    public static int e(String msg) {
        return println(Log.ERROR, TAG, msg);
    }

    public static int e(Throwable msg) {
        return println(Log.ERROR, TAG, Log.getStackTraceString(msg));
    }

    public static int e(String tag, String msg) {
        return println(Log.ERROR, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return println(Log.ERROR, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    public static int wtf(String msg) {
        return println(Log.ASSERT, TAG, msg);
    }

    public static int wtf(String tag, Throwable tr) {
        return println(Log.ASSERT, tag, Log.getStackTraceString(tr));
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        return println(Log.ASSERT, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    /**
     * Low-level logging call.
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
        if (!ContentHolder.isDebug()) {
            return 0;
        }
        //process tag
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        } else {
            if (!tag.startsWith(TAG)) {
                tag = TAG + "." + tag;
            }
        }
        return Log.println(priority, tag, msg);
    }
}