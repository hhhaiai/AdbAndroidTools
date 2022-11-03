package com.mrkid.flashbot;


import android.util.Log;

import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by xudong on 2/25/14.
 */
public class Push {

//    private AdbConnection adbConnection;
//    private File local;
//    private String remotePath;
//
//    public Push() {
//        this.adbConnection = adbConnection;
//        this.local = local;
//        this.remotePath = remotePath;
//    }

    public static void push(AdbConnection adbConnection, File local, String remotePath) throws Exception {

        AdbStream stream = adbConnection.open("sync:");

        String sendId = "SEND";

        String mode = ",33206";

        int length = (remotePath + mode).length();

        stream.write(ByteUtils.concat(sendId.getBytes(), ByteUtils.intToByteArray(length)));

        stream.write(remotePath.getBytes());

        stream.write(mode.getBytes());

        byte[] buff = new byte[adbConnection.getMaxData()];
        InputStream is = new FileInputStream(local);

//        long sent = 0;
//        long total = local.length();
//        int lastProgress = 0;
        while (true) {
            int read = is.read(buff);
            if (read < 0) {
                break;
            }
            byte[] bs = ByteUtils.concat("DATA".getBytes(), ByteUtils.intToByteArray(read));
            d("will write bs: " + new String(bs));
            stream.write(bs);
            if (read == buff.length) {
                d("will write buff: " + new String(buff));
                stream.write(buff);
            } else {
                byte[] tmp = new byte[read];
                System.arraycopy(buff, 0, tmp, 0, read);
                d("will write tmp: " + new String(tmp));
                stream.write(tmp);
            }
            // 进度打印
//            sent += read;
//
//            final int progress = (int)(sent * 100 / total);
//            if (lastProgress != progress) {
//                System.out.println(progress);
//                lastProgress = progress;
//            }
        }
//        byte[] d=ByteUtils.concat("DONE".getBytes(), ByteUtils.intToByteArray((int) System.currentTimeMillis()));
//        d("will write d: " + new String(d,"US-ASCII"));
//        stream.write(d);

        byte[] res = stream.read();
        // TODO: test if res contains "OKEY" or "FAIL"
        d(new String(res));

        stream.write(ByteUtils.concat("QUIT".getBytes(), ByteUtils.intToByteArray(0)));
    }

    public static void d(String e) {
        Log.i("sanbo.adbTestA", e);
    }

}
