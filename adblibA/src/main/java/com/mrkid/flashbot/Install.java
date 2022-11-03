package com.mrkid.flashbot;


import android.util.Log;

import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbStream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xudong on 2/25/14.
 */
public class Install {
    private AdbConnection adbConnection;
    private String remotePath;
    private long installTimeAssumption = 0;

    public Install(AdbConnection adbConnection, String remotePath, long installTimeAssumption) {
        this.adbConnection = adbConnection;
        this.remotePath = remotePath;
        this.installTimeAssumption = installTimeAssumption;
    }

    public void execute() throws IOException, InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        try {
            AdbStream stream = adbConnection.open("shell:pm install -r " + remotePath);


            // we assume installation will take installTimeAssumption milliseconds.
            new Thread() {
                @Override
                public void run() {
                    int percent = 0;

                    while (!done.get()) {
//                        handler.sendMessage(handler.obtainMessage(Message.INSTALLING_PROGRESS, Message.PM_INST_PART, percent));
                        d("percent:" + percent);
                        if (percent < 95) {
                            percent += 1;
                            try {
                                Thread.sleep(installTimeAssumption / 100);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }.start();

            while (!stream.isClosed()) {
                try {
                    d(new String(stream.read()));
                } catch (IOException e) {
                    // there must be a Stream Close Exception
                    break;
                }
            }
        } finally {
            done.set(true);
//            handler.sendMessage(handler.obtainMessage(Message.INSTALLING_PROGRESS, Message.PM_INST_PART, 100));
        }
    }

    public static void d(String e) {
        Log.i("sanbo.adbTestA", e);
    }

}
