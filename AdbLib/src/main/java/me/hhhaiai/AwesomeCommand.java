package me.hhhaiai;

import android.content.ComponentName;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import me.hhhaiai.adblib.AdbBase64;
import me.hhhaiai.adblib.AdbConnection;
import me.hhhaiai.adblib.AdbCrypto;
import me.hhhaiai.adblib.AdbStream;
import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Pools;
import me.hhhaiai.utils.Streams;
import me.hhhaiai.utils.ref.ContentHolder;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: 命令入口方式
 * @Version: 1.0
 * @Create: 2022-11-09 14:47:45
 * @author: sanbo
 */
public class AwesomeCommand {



    public static String getFps() {
        Alog.i("...fps....");
        return null;
    }

    /**
     * 在顶层Activity列表中查找应用的Activity
     * @param app 应用
     * @return 应用在顶层的Activity，不存在返回空字符串  <ComponentName(pkg/activity):pid>
     */
    public static ComponentName getTopActivityAndProcess(String app) {

        ComponentName cc = getTopinfo();
//        if (Build.VERSION.SDK_INT < 29) {
//            // 29也可以
//            String cmd = "dumpsys activity top | grep \"ACTIVITY " + app + "\"";
////            Alog.i("cmd:" + cmd);
//            String[] infos = exec(cmd).split("\n");
//            if (infos.length > 0 && !Texts.isEmpty(infos[0])) {
//                //ACTIVITY com.xiaomi.shop/com.xiaomi.shop2.plugin.PluginRootActivity 85244ee pid=32128
//                String[] contents = Texts.trim(infos[0]).split("\\s+");
//
//                String[] appAndAct = contents[1].split("/");
//                ComponentName cn = null;
//                if (appAndAct.length == 2) {
//                    String pkgName = appAndAct[0];
//                    String className = Texts.startWith(appAndAct[1], ".") ? pkgName + appAndAct[1] : appAndAct[1];
//                    cn = new ComponentName(pkgName, className);
//                }
//
//                if (cn == null) {
//                    Alog.e("the cn is null!");
//                    return null;
//                }
//                return cn;
//            }
//        } else {
//            //Build.VERSION.SDK_INT >= 29
//            String cmd = "dumpsys window windows | grep \"ACTIVITY " + app + "\"";
//            // 每行一个Activity，切换界面时可能存在多个Activity，无法用上一行的task，可能是自定义的
//            String trimmed = Texts.trim(exec(cmd));
//            Alog.i("trimmed:" + trimmed);
//        }

        // plan a  有点毛病
//        sanbo [~]$ adb shell dumpsys window w | grep name=
//                mSurface=Surface(name=NavigationBar0)/@0x4a19fbb
//        mSurface=Surface(name=StatusBar)/@0xd1e2cd8
//        mSurface=Surface(name=com.android.chrome/com.google.android.apps.chrome.Main)/@0xf3c8929

        //mSurface=Surface(name=RoundCorner)/@0x5e3ad29
        //      mSurface=Surface(name=RoundCorner)/@0x39c8b2d
        //       mAnimationIsEntrance=true      mSurface=Surface(name=NavigationBar)/@0x604a014
        //       mAnimationIsEntrance=true      mSurface=Surface(name=StatusBar)/@0x9122a7d
        //      mSurface=Surface(name=com.xiaomi.shop/com.xiaomi.shop2.plugin.PluginCartActivity)/@0x79ebdb8
        //       mAnimationIsEntrance=true      mSurface=Surface(name=com.android.keyguard.wallpaper.service.MiuiKeyguardPictorialWallpaper)/@0xa96433
        //       mAnimationIsEntrance=true      mSurface=Surface(name=com.android.systemui.ImageWallpaper)/@0xf2f2cd6
        //    name=pip_input_consumer pid=2081 user=UserHandle{0}

        // plan b 非Error第一个
        //sanbo [~]$ adb shell dumpsys window | grep mCurrentFocus
        //  mCurrentFocus=Window{a6d65d4 u0 Application Error: me.sanbo.adbtools}
        //  mCurrentFocus=Window{fe11ebe u0 me.sanbo.adbtools/me.sanbo.adbtools.MainActivity}

        // plan c 非ScreenDecorOverlayBottom、ScreenDecorOverlay、NavigationBar0、StatusBar后的第一个
        //sanbo [~]$ adb shell dumpsys window w | grep " mSurface=Surface(name="
        //      mSurface=Surface(name=ScreenDecorOverlayBottom)/@0xe287df6
        //      mSurface=Surface(name=ScreenDecorOverlay)/@0xfe9bc64
        //      mSurface=Surface(name=NavigationBar0)/@0x3067cef
        //      mSurface=Surface(name=StatusBar)/@0xa893e8
        //      mSurface=Surface(name=me.sanbo.adbtools/me.sanbo.adbtools.MainActivity)/@0x38df0ad

        //      mSurface=Surface(name=GestureStubRight)/@0x99867a8
        //      mSurface=Surface(name=GestureStubLeft)/@0x175cf9e
        //      mSurface=Surface(name=RoundCorner)/@0x3693e10
        //      mSurface=Surface(name=RoundCorner)/@0x8d998fe
        //      mSurface=Surface(name=GestureStubHome)/@0x60ae143
        //      mSurface=Surface(name=NavigationBar0)/@0xc04d79
        //      mSurface=Surface(name=control_center)/@0x70b9d45
        //      mSurface=Surface(name=StatusBar)/@0x46a923b
        //      mSurface=Surface(name=PopupWindow:fb4cbfb)/@0x958db57
        //      mSurface=Surface(name=com.ss.android.article.news/com.ss.android.article.news.activity.MainActivity)/@0x9f41644
        //      mSurface=Surface(name=com.miui.miwallpaper.superwallpaper.SnowmountainSuperWallpaper)/@0x90fcb87

        //      mSurface=Surface(name=RoundCorner)/@0x5e3ad29
        //      mSurface=Surface(name=RoundCorner)/@0x39c8b2d
        //       mAnimationIsEntrance=true      mSurface=Surface(name=NavigationBar)/@0x604a014
        //       mAnimationIsEntrance=true      mSurface=Surface(name=StatusBar)/@0x9122a7d
        //      mSurface=Surface(name=com.xiaomi.shop/com.xiaomi.shop2.plugin.PluginCartActivity)/@0x79ebdb8
        //       mAnimationIsEntrance=true      mSurface=Surface(name=com.android.keyguard.wallpaper.service.MiuiKeyguardPictorialWallpaper)/@0xa96433
        //       mAnimationIsEntrance=true      mSurface=Surface(name=com.android.systemui.ImageWallpaper)/@0xf2f2cd6

        //plan d  最后一行
        //sanbo [~]$ adb shell dumpsys activity top | grep ACTIVITY
        //  ACTIVITY com.android.settings/.Settings 7ce7fe6 pid=2556
        //  ACTIVITY me.sanbo.adbtools/.MainActivity 4404fe0 pid=7301
        //  ACTIVITY org.chromium.webview_shell/.WebViewBrowserActivity 1349f03 pid=9272
        //  ACTIVITY com.android.camera2/com.android.camera.CameraLauncher 177c88b pid=9426
        //  ACTIVITY com.android.messaging/.ui.conversationlist.ConversationListActivity 617a024 pid=9538
        //  ACTIVITY com.android.launcher3/.uioverrides.QuickstepLauncher 72075ff pid=2771
        //  ACTIVITY com.android.dialer/.main.impl.MainActivity a4505ad pid=9614 // current package


        return null;
    }

    /**
     * <shell>dumpsys activity top | grep ACTIVITY</shell>
     * <code>
     *           ACTIVITY com.android.settings/.Settings 7ce7fe6 pid=2556
     *           ACTIVITY me.sanbo.adbtools/.MainActivity 4404fe0 pid=7301
     *           ACTIVITY org.chromium.webview_shell/.WebViewBrowserActivity 1349f03 pid=9272
     *           ACTIVITY com.android.camera2/com.android.camera.CameraLauncher 177c88b pid=9426
     *           ACTIVITY com.android.messaging/.ui.conversationlist.ConversationListActivity 617a024 pid=9538
     *           ACTIVITY com.android.launcher3/.uioverrides.QuickstepLauncher 72075ff pid=2771
     *           ACTIVITY com.android.dialer/.main.impl.MainActivity a4505ad pid=9614 // current package
     * </code>
     * @return
     */
    public static ComponentName getTopinfo() {
        // 执行有问题
        String cmd = "dumpsys activity top | grep ACTIVITY";
        String r = exec(cmd,9);
        Alog.i("=============["+r+"]" );

        return null;
    }




    /**
     * 默认设置
     */
    static {
        CommandConfig.debug(true)
                .shellMode(false)
                .tcpip(5555)
                .build(new IAdbCallBack() {
                    @Override
                    public void onError(Throwable exception) {
                        Alog.i("[AwesomeCommand]adb 链接失败！！ 请执行如下命令:"
                                + "\r\nadb tcpip 5555"
                                + "\r\n异常:" + Log.getStackTraceString(exception));
                    }

                    @Override
                    public void onSuccess() {
                        Alog.i("[AwesomeCommand]adb 链接成功！！！");
                    }
                });
    }


    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    private static volatile AdbConnection connection;
    private static List<AdbStream> streams = new ArrayList<AdbStream>();


    public static boolean isReady() {
        if (connection == null) {
            Alog.e("no connection when check connection...");
            return false;
        }
        return true;
    }

    /**
     * 执行adb shell相关命令
     * @param cmd
     * @return
     */
    public static String exec(String cmd) {
        if (!isReady()) {
            return null;
        }
        return exec(cmd, 0);
    }

    /**
     * 执行Adb命令，对外<br/>
     * <b>注意：主线程执行的话超时时间会强制设置为5S以内，防止ANR</b>
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String exec(final String cmd, int wait) {
        if (!isReady()) {
            return null;
        }
        // 主线程的话走Callable
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (wait > 5000 || wait == 0) {
                Alog.w(String.format("主线程配置的等待时间[%dms]过长，修改为5000ms", wait));
                wait = 5000;
            }

            final int finalWait = wait;
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() {
                    return _execAdbCmd(cmd, finalWait);
                }
            };
            Future<String> result = cachedExecutor.submit(callable);

            // 等待执行完毕
            try {
                return result.get();
            } catch (Throwable e) {
                Alog.e(e);
            }
            return null;
        }
        return _execAdbCmd(cmd, wait);
    }

    /**
     * 执行Adb命令
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String _execAdbCmd(final String cmd, final int wait) {
        if (connection == null) {
            Alog.e("no connection when execAdbCmd");
            return "";
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            Alog.cmd(stream.getLocalId() + "@" + "shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待最长wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }

                if (!stream.isClosed()) {
                    stream.close();
                }
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes : results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            Alog.cmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            Alog.e(e);


            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection(null);
            if (result) {
                return retryExecAdb(cmd, wait);
            } else {
                Alog.e("regenerateConnection failed");
                return "";
            }
        } catch (Throwable e) {
            Alog.e(e);
            return "";
        }
    }

    private static String retryExecAdb(String cmd, long wait) {
        AdbStream stream = null;
        try {
            stream = connection.open("shell:" + cmd);
            Alog.cmd(stream.getLocalId() + "@shell:" + cmd);
            streams.add(stream);

            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
            if (wait == 0) {
                while (!stream.isClosed()) {
                    Thread.sleep(10);
                }
            } else {
                // 等待wait毫秒后强制退出
                long start = System.currentTimeMillis();
                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
                    Thread.sleep(10);
                }
                if (!stream.isClosed()) {
                    stream.close();
                }
            }

            // 获取stream所有输出
            Queue<byte[]> results = stream.getReadQueue();
            StringBuilder sb = new StringBuilder();
            for (byte[] bytes : results) {
                if (bytes != null) {
                    sb.append(new String(bytes));
                }
            }
            Alog.cmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
            streams.remove(stream);
            return sb.toString();
        } catch (Throwable e) {

            Alog.e(e);
        }

        return "";
    }

    /**
     * 生成Adb连接，由所在文件生成，或创建并保存到相应文件
     * @return
     * @param callBack
     */
    public static boolean generateConnection(IAdbCallBack callBack) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Pools.execute(() -> {
                generateConnectionImpl(callBack);
            });
        } else {
            return generateConnectionImpl(callBack);
        }
        return false;
    }

    /**
     * 生成配对的公钥、秘钥
     * @param callBack
     * @return
     */
    private static boolean generateConnectionImpl(IAdbCallBack callBack) {
        if (connection != null && connection.isFine()) {
            Alog.i(" connection is fine~~");
            if (callBack != null) {
                callBack.onSuccess();
            }
            return true;
        }
        Alog.i(" generateConnection connection:" + connection);
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable e) {
                Alog.e(e);
            } finally {
                connection = null;
            }
        }

        Socket sock;
        AdbCrypto crypto;
        AdbBase64 base64 = getBase64Impl();

        // 获取连接公私钥
        File privKey = new File(ContentHolder.getCacheDir(), "privKey");
        File pubKey = new File(ContentHolder.getCacheDir(), "pubKey");

        Alog.i("privKey path:" + privKey.getAbsolutePath() + "\r\npubKey path:" + pubKey.getAbsolutePath());
        if (!privKey.exists() || !pubKey.exists()) {
            try {
                Alog.d(" generateConnection privKey & pubKey not exists!");
                crypto = AdbCrypto.generateAdbKeyPair(base64);
                privKey.deleteOnExit();
                pubKey.deleteOnExit();
                crypto.saveAdbKeyPair(privKey, pubKey);
            } catch (Throwable e) {
                Alog.e(e);
                if (callBack != null) {
                    callBack.onError(e);
                }
                return false;
            }
        } else {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
            } catch (Throwable e) {
                Alog.e(e);
                try {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    privKey.deleteOnExit();
                    pubKey.deleteOnExit();
                    crypto.saveAdbKeyPair(privKey, pubKey);
                } catch (Throwable ew) {
                    Alog.e(ew);
                    if (callBack != null) {
                        callBack.onError(ew);
                    }
                    return false;
                }
            }
        }

        // 开始连接adb
        Alog.i("Socket connecting...");
        try {
            sock = new Socket(ContentHolder.getAddress(), ContentHolder.getPort());
            sock.setReuseAddress(true);
        } catch (Throwable e) {
            Alog.e(e);
            if (callBack != null) {
                callBack.onError(e);
            }
            return false;
        }
        Alog.i("Socket connected");

        AdbConnection conn;
        try {
            conn = AdbConnection.create(sock, crypto);
            Alog.i("ADB connecting...");

            // 10s超时
            conn.connect(10 * 1000);
        } catch (Throwable e) {
            Alog.e(e);
            Streams.close(sock);
            if (callBack != null) {
                callBack.onError(e);
            }
            return false;
        }
        connection = conn;
        Alog.i("ADB connected");
//        //  暂缓该部分处理
//        // ADB成功连接后，开启ADB状态监测
//        startAdbStatusCheck();
        if (callBack != null) {
            callBack.onSuccess();
        }
        return true;
    }

    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeToString(arg0, Base64.NO_WRAP);
            }
        };
    }

}
