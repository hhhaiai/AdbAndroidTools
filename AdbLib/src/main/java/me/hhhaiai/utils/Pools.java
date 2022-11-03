package me.hhhaiai.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class Pools {
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Handler sUiThreadHandler = new Handler(Looper.getMainLooper());

    public static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public static void runOnWorkThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (isMainThread()) {
            execute(runnable);
        } else {
            runnable.run();
        }

    }

    public static void runOnUiThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (isMainThread()) {
            runnable.run();

        } else {
            FutureTask<Void> task = new FutureTask<Void>(runnable, null);
            sUiThreadHandler.post(task);
            try {
                task.get();
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred while waiting for runnable", e);
            }
        }

    }
}
