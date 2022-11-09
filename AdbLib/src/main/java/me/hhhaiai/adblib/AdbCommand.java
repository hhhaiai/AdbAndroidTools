package me.hhhaiai.adblib;

import android.os.Looper;
import android.util.Base64;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Pools;
import me.hhhaiai.utils.Streams;
import me.hhhaiai.utils.ref.ContentHolder;

public class AdbCommand {
    private AdbCommand() {
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
     * 执行Adb命令，对外<br/>
     * <b>注意：主线程执行的话超时时间会强制设置为5S以内，防止ANR</b>
     * @param cmd 对应命令
     * @return 命令行输出
     */
    public static String execAdbCmd(final String cmd) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Future<String> result = cachedExecutor.submit(new Callable<String>() {
                @Override
                public String call() {
                    return _execAdbCmd(cmd);
                }
            });
            // 等待执行完毕
            try {
                return result.get();
            } catch (Throwable e) {
                Alog.e(e);
            }
            return null;
        }
        return _execAdbCmd(cmd);
    }

    /**
     * 执行Adb命令
     * @param cmd 对应命令
     * @return 命令行输出
     */
    public static String _execAdbCmd(final String cmd) {
        if (connection == null) {
            Alog.e("no connection when execAdbCmd");
            return null;
        }
        try {
            AdbStream stream = connection.open("shell:" + cmd);
            Alog.cmd("__命令__" + stream.getLocalId() + "@" + "shell:" + cmd);
            streams.add(stream);

            StringBuilder sb = new StringBuilder();
            // 获取stream所有输出
            for (byte[] bytes : stream.getReadQueue()) {
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
        }
        return null;
    }


    /**
     * 生成Adb连接，由所在文件生成，或创建并保存到相应文件
     * @return
     * @param callBack
     */
    public static boolean generateConnection(IAdbCallBack callBack) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Pools.execute(() -> {
                Alog.i(" main thread");
                generateConnectionImpl(callBack);
            });
        } else {
            Alog.i("not main thread");
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


//    private static ScheduledExecutorService scheduledExecutorService;
//    private static volatile long LAST_RUNNING_TIME = 0;
//
//    /**
//     * 开始检查ADB状态
//     */
//    private static void startAdbStatusCheck() {
//        if (scheduledExecutorService == null) {
//            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
//        }
//
//
//        scheduledExecutorService.schedule(new Runnable() {
//            @Override
//            public void run() {
//                long currentTime = System.currentTimeMillis();
//                // 防止重复运行，14s内只能执行一次
//                if (currentTime - LAST_RUNNING_TIME < 14 * 1000) {
//                    return;
//                }
//
//                LAST_RUNNING_TIME = currentTime;
//                int result = checkAdbStatus();
//                if (result < 0) {
//                    Alog.e("[ADB异常]startAdbStatusCheck result:" + result);
//                    return;
//                }
//
//                // 15S 检查一次
//                scheduledExecutorService.schedule(this, 15, TimeUnit.SECONDS);
//            }
//        }, 15, TimeUnit.SECONDS);
//    }
//
//    /**
//     * 检查并尝试恢复ADB连接
//     * @return
//     */
//    private static int checkAdbStatus() {
//        String result = null;
//        try {
//            result = execAdbCmd("echo '1'", 5000);
//        } catch (Throwable e) {
//            Alog.e(e);
//        }
//
//        if (!Texts.equals("1", Texts.trim(result))) {
//            // 等2s再检验一次
//            SystemClock.sleep(2000);
//
//            boolean genResult = false;
//
//            // double check机制，防止单次偶然失败带来重连
//            String doubleCheck = null;
//            try {
//                doubleCheck = execAdbCmd("echo '1'", 5000);
//            } catch (Exception e) {
//                Alog.e(e);
//            }
//            if (!Texts.equals("1", Texts.trim(doubleCheck))) {
//                // 尝试恢复3次
//                for (int i = 0; i < 3; i++) {
//                    // 关停无用连接
//                    if (connection != null && connection.isFine()) {
//                        try {
//                            connection.close();
//                        } catch (Exception e) {
//                            Alog.e(e);
//                        } finally {
//                            connection = null;
//                        }
//                    }
//
//                    // 清理下当前已连接进程
//                    clearProcesses();
//
//                    // 尝试重连
//                    genResult = generateConnection();
//                    if (genResult) {
//                        break;
//                    }
//                }
//
//                // 恢复失败
//                if (!genResult) {
//                    // 通知各个功能ADB挂了
//                    return -1;
//                }
//            }
//        }
//
//        return 0;
//    }
//
//
//    public static void clearProcesses() {
//        try {
//            for (Process p : processes) {
//                Alog.i("stop process: " + p.toString());
//                p.destroy();
//            }
//            processes.clear();
//            for (AdbStream stream : streams) {
//                Alog.i("stop stream: " + stream.toString());
//                Streams.close(stream);
//            }
//            streams.clear();
//        } catch (Throwable e) {
//            Alog.e(e);
//        }
//    }


//    /************************************* 尝试系列 暂时去除******************************************/
//
//    private static String retryExecAdb(String cmd, long wait) {
//        AdbStream stream = null;
//        try {
//            stream = connection.open("shell:" + cmd);
//            Alog.cmd(stream.getLocalId() + "@shell:" + cmd);
//            streams.add(stream);
//
//            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
//            if (wait == 0) {
//                while (!stream.isClosed()) {
//                    Thread.sleep(10);
//                }
//            } else {
//                // 等待wait毫秒后强制退出
//                long start = System.currentTimeMillis();
//                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
//                    Thread.sleep(10);
//                }
//                if (!stream.isClosed()) {
//                    stream.close();
//                }
//            }
//
//            // 获取stream所有输出
//            Queue<byte[]> results = stream.getReadQueue();
//            StringBuilder sb = new StringBuilder();
//            for (byte[] bytes : results) {
//                if (bytes != null) {
//                    sb.append(new String(bytes));
//                }
//            }
//            Alog.cmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
//            streams.remove(stream);
//            return sb.toString();
//        } catch (Throwable e) {
//            Alog.e(e);
//        }
//
//        return "";
//    }
//
//
//    private static String execAdbCmdWithStatus(final String cmd, final int wait) {
//        if (connection == null) {
//            Alog.e(new NullPointerException("[execAdbCmdWithStatus] connection is null"));
//            return "";
//        }
//
//        try {
//            AdbStream stream = connection.open("shell:" + cmd);
//            Alog.cmd(stream.getLocalId() + "@shell:" + cmd);
//            streams.add(stream);
//
//            // 当wait为0，每个10ms观察一次stream状况，直到shutdown
//            if (wait == 0) {
//                while (!stream.isClosed()) {
//                    Thread.sleep(10);
//                }
//            } else {
//                // 等待wait毫秒后强制退出
//                long start = System.currentTimeMillis();
//                while (!stream.isClosed() && System.currentTimeMillis() - start < wait) {
//                    Thread.sleep(10);
//                }
//                if (!stream.isClosed()) {
//                    stream.close();
//                }
//            }
//
//            // 获取stream所有输出
//            Queue<byte[]> results = stream.getReadQueue();
//            StringBuilder sb = new StringBuilder();
//            for (byte[] bytes : results) {
//                if (bytes != null) {
//                    sb.append(new String(bytes));
//                }
//            }
//
//            Alog.cmd(stream.getLocalId() + "@" + "shell:->" + sb.toString());
//            streams.remove(stream);
//            return sb.toString();
//        } catch (Throwable e) {
//            Alog.e(e);
//        }
//        return "";
//    }


}
