package com.cgutman.adblib;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ADBCmd {
    private static String TAG = "sanbo.ADBCmd";
    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    private static List<Process> processes = new ArrayList<>();
    private static volatile AdbConnection connection;
    private static List<AdbStream> streams = new ArrayList<>();

    private static String mServer = "localhost:5555";
    private static Context mContext = null;

    private static ADBCmd cmds = new ADBCmd();

    private ADBCmd() {
    }

    public static ADBCmd context(Context context) {
        mContext = context;
        return cmds;
    }


    public static String exec(final String cmd) {
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
        // 主线程的话走Callable
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (wait > 5000 || wait == 0) {
                w(String.format("主线程配置的等待时间[%dms]过长，修改为5000ms", wait));
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
                e(e);
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
            e("no connection when execAdbCmd");
            generateConnection();
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            if (!cmd.contains("echo")) {
                logcatCmd("__命令__" + stream.getLocalId() + "@" + "shell:" + cmd);
            }
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
            if (!cmd.contains("echo")) {
                logcatCmd("__结果__" + stream.getLocalId() + "@" + "shell:->" + sb.toString());
            }
            streams.remove(stream);
            return sb.toString();
        } catch (IllegalStateException e) {
            e(e);

            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection();
            if (result) {
                return retryExecAdb(cmd, wait);
            }
        } catch (Throwable e) {
            e(e);
        }
        return "";
    }


    /**
     * 生成Adb连接，由所在文件生成，或创建并保存到相应文件
     */
    public static boolean generateConnection() {
        if (mContext == null) {
            mContext = getContext();
        }
        if (connection != null && connection.isFine()) {
            return true;
        }
        i("inside  generateConnection connection:" + connection);
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable e) {
                e(e);
            } finally {
                connection = null;
            }
        }

        Socket sock;
        AdbCrypto crypto;
        AdbBase64 base64 = getBase64Impl();

        // 获取连接公私钥
        File privKey = new File(mContext.getFilesDir(), "privKey");
        File pubKey = new File(mContext.getFilesDir(), "pubKey");

        if (!privKey.exists() || !pubKey.exists()) {
            try {
                crypto = AdbCrypto.generateAdbKeyPair(base64);
                privKey.delete();
                pubKey.delete();
                crypto.saveAdbKeyPair(privKey, pubKey);
            } catch (Throwable e) {
                e(e);
                return false;
            }
        } else {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
            } catch (Throwable e) {
                e(e);
                try {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    privKey.delete();
                    pubKey.delete();
                    crypto.saveAdbKeyPair(privKey, pubKey);
                } catch (Throwable ew) {
                    e(ew);
                    return false;
                }
            }
        }

        // 开始连接adb
        i("Socket connecting...");
        try {
            String[] split = mServer.split(":");
            sock = new Socket(split[0], Integer.parseInt(split[1]));
            sock.setReuseAddress(true);
        } catch (Throwable e) {
            e(e);
            return false;
        }
        i("Socket connected");

        AdbConnection conn;
        try {
            conn = AdbConnection.create(sock, crypto);
            i("ADB connecting...");

            // 10s超时
            conn.connect(10 * 1000);
        } catch (Throwable e) {
            e(e);
            // socket关闭
            if (sock.isConnected()) {
                try {
                    sock.close();
                } catch (Throwable e1) {
                    e(e1);
                }
            }
            return false;
        }
        connection = conn;
        i("ADB connected");

//        if (DEVICE_ID == null) {
//            DEVICE_ID = StringUtil.trim(execHighPrivilegeCmd("getprop ro.serialno"));
//        }

        // ADB成功连接后，开启ADB状态监测
        startAdbStatusCheck();

//        // 触发ADB连接状态监听
//        LauncherApplication.getInstance().triggerAtTime(Trigger.TRIGGER_TIME_ADB_CONNECT);

        return true;
    }

    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeToString(arg0, 2);
            }
        };
    }


    private static ScheduledExecutorService scheduledExecutorService;
    private static volatile long LAST_RUNNING_TIME = 0;

    /**
     * 开始检查ADB状态
     */
    private static void startAdbStatusCheck() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }


        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // 防止重复运行，14s内只能执行一次
                if (currentTime - LAST_RUNNING_TIME < 14 * 1000) {
                    return;
                }

                LAST_RUNNING_TIME = currentTime;
                int result = checkAdbStatus();
                if (result < 0) {
                    e("[ADB异常]startAdbStatusCheck result:" + result);
                    return;
                }

                // 15S 检查一次
                scheduledExecutorService.schedule(this, 15, TimeUnit.SECONDS);
            }
        }, 15, TimeUnit.SECONDS);
    }

    /**
     * 检查并尝试恢复ADB连接
     * @return
     */
    private static int checkAdbStatus() {
        String result = null;
        try {
            result = exec("echo '1'", 5000);
        } catch (Exception e) {
            e(e);
        }

        if (!trimEquals("1", result)) {
            // 等2s再检验一次
            SystemClock.sleep(2000);

            boolean genResult = false;

            // double check机制，防止单次偶然失败带来重连
            String doubleCheck = null;
            try {
                doubleCheck = exec("echo '1'", 5000);
            } catch (Exception e) {
                e(e);
            }
            if (!trimEquals("1", doubleCheck)) {
                // 尝试恢复3次
                for (int i = 0; i < 3; i++) {
                    // 关停无用连接
                    if (connection != null && connection.isFine()) {
                        try {
                            connection.close();
                        } catch (Exception e) {
                            e(e);
                        } finally {
                            connection = null;
                        }
                    }

                    // 清理下当前已连接进程
                    clearProcesses();

                    // 尝试重连
                    genResult = generateConnection();
                    if (genResult) {
                        break;
                    }
                }

                // 恢复失败
                if (!genResult) {
                    // 通知各个功能ADB挂了
                    return -1;
                }
            }
        }

        return 0;
    }


    public static void clearProcesses() {
        try {
            for (Process p : processes) {
                i("stop process: " + p.toString());
                p.destroy();
            }
            processes.clear();
            for (AdbStream stream : streams) {
                i("stop stream: " + stream.toString());
                close(stream);
            }
            streams.clear();
        } catch (Throwable e) {
            e(e);
        }
    }

    /************************************* 尝试系列******************************************/

    private static String retryExecAdb(String cmd, long wait) {
        AdbStream stream = null;
        try {
            stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
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
            logcatCmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
            streams.remove(stream);
            return sb.toString();
        } catch (Throwable e) {
            e(e);
        }

        return "";
    }


    private static String execAdbCmdWithStatus(final String cmd, final int wait) {
        if (connection == null) {
            e(new NullPointerException("[execAdbCmdWithStatus] connection is null"));
            return "";
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            logcatCmd(stream.getLocalId() + "@shell:" + cmd);
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

            logcatCmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
            streams.remove(stream);
            return sb.toString();
        } catch (Throwable e) {
            e(e);
        }
        return "";
    }


    private static void close(Object... objs) {
        if (objs == null || objs.length < 1) {
            return;
        }
        for (Object obj : objs) {
            try {
                if (obj instanceof Closeable) {
                    ((Closeable) obj).close();
                }
            } catch (Throwable e) {
                e(e);
            }
        }
    }



    private static void i(String cmd) {
        Log.i(TAG, cmd);
    }

    private static void w(String cmd) {
        Log.w(TAG, cmd);
    }

    private static void e(String cmd) {
        Log.e(TAG, cmd);
    }

    private static void e(Throwable e) {
        Log.e(TAG, Log.getStackTraceString(e));
    }

    protected static void logcatCmd(String cmd) {
        i("ADB CMD: " + cmd);
    }


    public static Context getContext() {
        Object activityThread = MinRefUtils.call("android.app.ActivityThread", "currentActivityThread");
        Object application = MinRefUtils.call(activityThread.getClass(), "getApplication", activityThread);

        if (application == null) {
            application = MinRefUtils.call("android.app.AppGlobals", "getInitialApplication");
        }
        if (application != null) {
            return ((Application) application).getApplicationContext();
        }
        Object context = MinRefUtils.call(activityThread.getClass(), "getSystemContext", activityThread);
        if (context == null) {
            context = MinRefUtils.call(activityThread.getClass(), "getSystemUiContext", activityThread);
        }
        if (context != null) {
            return (Context) context;
        }
        return null;
    }

    public static boolean trimEquals(CharSequence a, CharSequence b) {
        a = trim(a);
        b = trim(b);
        // 两者都为空，相等
        if (a == null && b == null) {
            return true;
        }

        // 一个为空，不等
        if (a == null || b == null) {
            return false;
        }

        // 都不为空，直接比较
        return a.toString().equals(b.toString());
    }

    /**
     * 去除前后不可见符号
     * @param origin
     * @return
     */
    public static String trim(CharSequence origin) {
        return origin == null ? null : origin.toString().trim();
    }

}
