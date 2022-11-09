package me.hhhaiai;

import android.util.Log;

import me.hhhaiai.adblib.AdbCommand;
import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.Alog;

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
     * 执行adb shell相关命令
     * @param cmd
     * @return
     */
    public static String exec(String cmd) {
        if (!AdbCommand.isReady()) {
            Alog.e(" AdbCommand not ready!");
            return null;
        }
        return AdbCommand.execAdbCmd(cmd);
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
}
