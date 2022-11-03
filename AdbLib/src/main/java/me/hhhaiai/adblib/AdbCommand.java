package me.hhhaiai.adblib;

import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;

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

import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Streams;
import me.hhhaiai.utils.Texts;
import me.hhhaiai.utils.ref.ContentHolder;

public class AdbCommand {
    private static String TAG = "sanbo.Mys";
    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    private static List<Process> processes = new ArrayList<>();
    private static volatile AdbConnection connection;
    private static List<AdbStream> streams = new ArrayList<>();

    private static String mServer = "localhost:5555";

    private AdbCommand() {
    }


    /**
     * 执行Adb命令，对外<br/>
     * <b>注意：主线程执行的话超时时间会强制设置为5S以内，防止ANR</b>
     * @param cmd 对应命令
     * @param wait 等待执行时间，0表示一直等待
     * @return 命令行输出
     */
    public static String execAdbCmd(final String cmd, int wait) {
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
            return null;
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
            Alog.cmd("__命令__" + stream.getLocalId() + "@" + "shell:" + cmd);
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
            Alog.cmd("__结果__" + stream.getLocalId() + "@" + "shell:->" + sb.toString());
            streams.remove(stream);
            return sb.toString();
        } catch (Throwable e) {
            Alog.e(e);

            if (connection != null) {
                connection.setFine(false);
            }
            boolean result = generateConnection(ContentHolder.get().getFilesDir().getAbsolutePath());
            if (result) {
                return retryExecAdb(cmd, wait);
            }

        }
        return "";
    }



    /**
     * 生成Adb连接，由所在文件生成，或创建并保存到相应文件
     * @param cacheKeyDirPath
     * @return
     */
    public static boolean generateConnection(String cacheKeyDirPath) {
        if (Texts.isEmpty(cacheKeyDirPath)) {
            Alog.e("the cacheKeyDirPath is null! please check!!");
            return false;
        }
        if (connection != null && connection.isFine()) {
            return true;
        }
        Alog.i("inside  generateConnection connection:" + connection);
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
        File privKey = new File(ContentHolder.get().getFilesDir(), "privKey");
        File pubKey = new File(ContentHolder.get().getFilesDir(), "pubKey");

        if (!privKey.exists() || !pubKey.exists()) {
            try {
                crypto = AdbCrypto.generateAdbKeyPair(base64);
                privKey.delete();
                pubKey.delete();
                crypto.saveAdbKeyPair(privKey, pubKey);
            } catch (Throwable e) {
                Alog.e(e);
                return false;
            }
        } else {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(base64, privKey, pubKey);
            } catch (Throwable e) {
                Alog.e(e);
                try {
                    crypto = AdbCrypto.generateAdbKeyPair(base64);
                    privKey.delete();
                    pubKey.delete();
                    crypto.saveAdbKeyPair(privKey, pubKey);
                } catch (Throwable ew) {
                    Alog.e(ew);
                    return false;
                }
            }
        }

        // 开始连接adb
        Alog.i("Socket connecting...");
        try {
            String[] split = mServer.split(":");
            sock = new Socket(split[0], Integer.parseInt(split[1]));
            sock.setReuseAddress(true);
        } catch (Throwable e) {
            Alog.e(e);
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
            // socket关闭
            if (sock.isConnected()) {
                try {
                    sock.close();
                } catch (Throwable e1) {
                    Alog.e(e1);
                }
            }
            return false;
        }
        connection = conn;
        Alog.i("ADB connected");

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
                    Alog.e("[ADB异常]startAdbStatusCheck result:" + result);
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
            result = execAdbCmd("echo '1'", 5000);
        } catch (Throwable e) {
            Alog.e(e);
        }

        if (!Texts.equals("1", Texts.trim(result))) {
            // 等2s再检验一次
            SystemClock.sleep(2000);

            boolean genResult = false;

            // double check机制，防止单次偶然失败带来重连
            String doubleCheck = null;
            try {
                doubleCheck = execAdbCmd("echo '1'", 5000);
            } catch (Exception e) {
                Alog.e(e);
            }
            if (!Texts.equals("1", Texts.trim(doubleCheck))) {
                // 尝试恢复3次
                for (int i = 0; i < 3; i++) {
                    // 关停无用连接
                    if (connection != null && connection.isFine()) {
                        try {
                            connection.close();
                        } catch (Exception e) {
                            Alog.e(e);
                        } finally {
                            connection = null;
                        }
                    }

                    // 清理下当前已连接进程
                    clearProcesses();

                    // 尝试重连
                    genResult = generateConnection(ContentHolder.get().getFilesDir().getAbsolutePath());
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
                Alog.i("stop process: " + p.toString());
                p.destroy();
            }
            processes.clear();
            for (AdbStream stream : streams) {
                Alog.i("stop stream: " + stream.toString());
                Streams.close(stream);
            }
            streams.clear();
        } catch (Throwable e) {
            Alog.e(e);
        }
    }

    /************************************* 尝试系列******************************************/

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


    private static String execAdbCmdWithStatus(final String cmd, final int wait) {
        if (connection == null) {
            Alog.e(new NullPointerException("[execAdbCmdWithStatus] connection is null"));
            return "";
        }

        try {
            AdbStream stream = connection.open("shell:" + cmd);
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


}
