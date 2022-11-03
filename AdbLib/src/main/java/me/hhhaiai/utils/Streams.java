package me.hhhaiai.utils;

import java.io.Closeable;
import java.net.Socket;

public class Streams {
    public static void close(Object... objs) {
        if (objs == null || objs.length < 1) {
            return;
        }
        for (Object obj : objs) {
            try {
                if (obj == null) {
                    continue;
                }
                if (obj instanceof Socket) {
                    Socket sock = (Socket) obj;
                    if (sock.isConnected()) {
                        sock.close();
                    }
                } else if (obj instanceof Closeable) {
                    ((Closeable) obj).close();
                }
            } catch (Throwable e) {
                Alog.e(e);
            }
        }
    }
}
