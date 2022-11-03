package me.hhhaiai.rootlib;

import android.content.Context;

import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Files;
import me.hhhaiai.utils.Streams;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class RootCommand {
    private static Boolean isRoot = null;

    public static boolean ready() {

        return false;
    }

    /**
     * 执行root命令
     * @param cmd 待执行命令
     * @param log 日志输出文件
     * @param ret 是否保留命令行输出
     * @param ct 上下文
     * @return 输出
     */
    @SuppressWarnings("deprecation")
    public static StringBuilder execRootCmd(String cmd, String log, Boolean ret, Context ct) {
        StringBuilder result = new StringBuilder();
        DataOutputStream dos = null;
        DataInputStream dis = null;
        DataInputStream des = null;
        String line = null;
        Process p;

        try {
            p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());
            des = new DataInputStream(p.getErrorStream());

            Alog.i(cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

            while ((line = dis.readLine()) != null) {
                if (log != null) {
                    Files.writeFileData(log, line, ct);
                }
                if (ret) {
                    result.append(line).append("\n");
                }
            }
            p.waitFor();
            isRoot = true;
        } catch (Throwable e) {
            Alog.e(e);
            isRoot = false;
        } finally {
            Streams.close(dos, dis);
        }
        return result;
    }


}
