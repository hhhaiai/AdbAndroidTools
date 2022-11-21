package me.hhhaiai;

import android.content.Context;
import android.text.TextUtils;

import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.ShellCommand;
import me.hhhaiai.utils.ref.ContentHolder;

/**
 * 命令相关配置选项, 可选择配置，也可以不选择配置
 */
public class CommandConfig {
    private static final CommandConfig mHighPrivilegeCommand = new CommandConfig();
    private static boolean isShellMode = false;
    private CommandConfig() {
    }

    /**
     * 设置context.可选项
     * @param ctx
     * @return
     */
    public static CommandConfig context(Context ctx) {
        ContentHolder.get(ctx);
        return mHighPrivilegeCommand;
    }


    /**
     * 设置缓存目录.可选项
     * @param cacheKeyDirName
     * @return
     */
    public static CommandConfig cacheKeyPath(String cacheKeyDirName) {
        if (!TextUtils.isEmpty(cacheKeyDirName)) {
            ContentHolder.setCacheDir(cacheKeyDirName);
        }
        return mHighPrivilegeCommand;
    }

    /**
     * 是否优先使用root模式，默认不使用。可选项
     * @param _isShellMode
     * @return
     */
    public static CommandConfig shellMode(boolean _isShellMode) {
        isShellMode = _isShellMode;
        return mHighPrivilegeCommand;
    }

    /**
     * 是否日志调试.可选项
     * @param _isDebug
     * @return
     */
    public static CommandConfig debug(boolean _isDebug) {
        ContentHolder.setDebug(_isDebug);
        return mHighPrivilegeCommand;
    }

    /**
     * 设置adb tcpip {tcpip}.可选项
     * @param port 用户设置的自定义tcpip端口
     * @return
     */
    public static CommandConfig tcpip(int port) {
        ContentHolder.setPort(port);
        return mHighPrivilegeCommand;
    }

    /**
     *  默认会执行adb初始化,必须选项
     * @param callBack
     */
    public static void build(IAdbCallBack callBack) {
        AwesomeCommand.generateConnection(callBack);
    }

    /**
     * 运行高权限命令,先确认是否可以root,然后再执行adb
     * @param cmd
     * @return
     */
    public static String execCmd(String cmd) {
        if (isShellMode && ShellCommand.ready()) {
            return ShellCommand.su(cmd);
        }
        return AwesomeCommand.exec(cmd);
    }


}
