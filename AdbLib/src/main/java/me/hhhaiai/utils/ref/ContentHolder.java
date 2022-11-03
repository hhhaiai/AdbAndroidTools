package me.hhhaiai.utils.ref;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import me.hhhaiai.utils.Texts;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: 上下文持有及部分数据持有
 * @Version: 1.0
 * @Create: 2022-11-03 16:45:36
 * @author: sanbo
 */
public class ContentHolder {

    private static Context mContext = null;
    private static String mCacheDir = null;
    private static boolean isDebug = true;


    /**
     * 获取上下文
     * @param objs
     * @return
     */
    public static Context get(Object... objs) {
        if (mContext != null) {
            return mContext;
        }
        // parser args to memory
        if (objs != null && objs.length > 0) {
            for (Object obj : objs) {
                if (obj instanceof Activity) {
                    mContext = ((Activity) obj).getApplicationContext();
                } else if (obj instanceof Context) {
                    mContext = (Context) obj;
                }
            }
        }
        // ref get context
        if (mContext == null) {
            //public static ActivityThread currentActivityThread()
            Object activityThread = Reflect.onClass("android.app.ActivityThread").call("currentActivityThread").get();
            //public Application getApplication()
            Object application = Reflect.on(activityThread).call("getApplication");
            if (application == null) {
                application = Reflect.onClass("android.app.AppGlobals").call("getInitialApplication").get();
            }
            if (application != null) {
                mContext = ((Application) application).getApplicationContext();
                return mContext;
            }
            //public ContextImpl getSystemContext() ---call--->ContextImpl.createSystemContext(this);
            mContext = Reflect.on(activityThread).call("getSystemContext").get();
            if (mContext == null) {
                if (Build.VERSION.SDK_INT > 25) {
                    //public ContextImpl getSystemUiContext()
                    mContext = Reflect.on(activityThread).call("getSystemUiContext").get();
                }
            }
            // static ContextImpl createSystemContext(ActivityThread mainThread)
            if (mContext == null && activityThread != null) {
                mContext = Reflect.onClass("android.app.ContextImpl").call("createSystemContext", activityThread).get();
            }
        }

        return mContext;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean _isDebug) {
        isDebug = _isDebug;
    }

    /**
     * 默认缓存目录
     * @return
     */
    public static String getCacheDir() {
        if (Texts.isEmpty(mCacheDir)) {
            return mCacheDir;
        }
        get();
        if (mContext != null) {
            return mContext.getFilesDir().getAbsolutePath();
        }
        return null;
    }

    /**
     * 设置缓存目录
     * @param cacheKeyDirName
     */
    public static void setCacheDir(String cacheKeyDirName) {
        if (!Texts.isEmpty(cacheKeyDirName)) {
            mCacheDir = cacheKeyDirName;
        }
    }

}
