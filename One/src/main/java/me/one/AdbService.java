package me.one;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class AdbService {
    private ThreadPoolProxy mProxy;
    private AdbConnector mAdbConnector;
    private Context mContext;

    public AdbService(Context context) {
        mProxy = ThreadPoolProxyFactory.getThreadPoolProxy();
        mAdbConnector = new AdbConnector();
        mContext = context;
    }

    public void performAdbRequest(final String cmd, final Callback callback) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String response = mAdbConnector.openShell(mContext, cmd);
                    if (!TextUtils.isEmpty(response)) {
                        if (callback != null) {
                            callback.onSuccess(response);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFail("");
                        }
                    }
                } catch (Throwable e) {
                    if (callback != null) {
                        callback.onFail(Log.getStackTraceString(e));
                    }
                }
            }
        };
        mProxy.execute(runnable);
    }

}
