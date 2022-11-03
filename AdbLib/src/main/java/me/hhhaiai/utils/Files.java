package me.hhhaiai.utils;

import android.content.Context;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Files {

    /**
     * 保存信息到文件中
     * @param log  日志输出文件
     * @param line 单行信息
     * @param ct 上下文
     */
    public static void writeFileData(String log, String line, Context ct) {
        String time = "";
        FileOutputStream fout = null;
        try {
            fout = ct.openFileOutput(log, Context.MODE_APPEND);
            SimpleDateFormat formatter = new SimpleDateFormat("+++   HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            time = formatter.format(curDate);
            byte[] bytes = line.getBytes();
            fout.write(bytes);
            bytes = (time + "\n").getBytes();
            fout.write(bytes);
        } catch (Throwable e) {
            Alog.e(e);
        } finally {
            Streams.close(fout);
        }

    }
}
