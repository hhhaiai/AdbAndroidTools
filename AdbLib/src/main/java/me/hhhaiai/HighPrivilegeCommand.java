package me.hhhaiai;

import android.content.Context;
import android.text.TextUtils;

import me.hhhaiai.adblib.AdbCommand;
import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.ShellCommand;
import me.hhhaiai.utils.ref.ContentHolder;

public class HighPrivilegeCommand {

    private static HighPrivilegeCommand mHighPrivilegeCommand = new HighPrivilegeCommand();

    private HighPrivilegeCommand() {
    }


    /**
     * 设置context.可选项
     * @param ctx
     * @return
     */
    public static HighPrivilegeCommand context(Context ctx) {
        ContentHolder.get(ctx);
        return mHighPrivilegeCommand;
    }


    /**
     * 设置缓存目录.可选项
     * @param cacheKeyDirName
     * @return
     */
    public static HighPrivilegeCommand cacheKeyPath(String cacheKeyDirName) {
        if (!TextUtils.isEmpty(cacheKeyDirName)) {
            ContentHolder.setCacheDir(cacheKeyDirName);
        }
        return mHighPrivilegeCommand;
    }

    /**
     * 是否日志调试.可选项
     * @param _isDebug
     * @return
     */
    public static HighPrivilegeCommand debug(boolean _isDebug) {
        ContentHolder.setDebug(_isDebug);
        return mHighPrivilegeCommand;
    }

    /**
     * 设置adb tcpip {tcpip}.
     * @param port 用户设置的自定义tcpip端口
     * @return
     */
    public static HighPrivilegeCommand tcpip(int port) {
        ContentHolder.setPort(port);
        return mHighPrivilegeCommand;
    }

    /**
     *  默认会执行adb初始化
     * @param callBack
     */
    public static void build(IAdbCallBack callBack) {
        AdbCommand.generateConnection(callBack);
    }

    /**
     * 运行高权限命令,先确认是否可以root,然后再执行adb
     * @param cmd
     * @return
     */
    public static String execHighPrivilegeCmd(String cmd) {
        if (ShellCommand.ready()) {
            return ShellCommand.su(cmd);
        }
        return AdbCommand.execAdbCmd(cmd, 0);
    }
}
