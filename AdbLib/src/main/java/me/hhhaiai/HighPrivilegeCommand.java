package me.hhhaiai;

import android.content.Context;
import android.text.TextUtils;

import me.hhhaiai.adblib.AdbCommand;
import me.hhhaiai.rootlib.RootCommand;
import me.hhhaiai.utils.ref.ContentHolder;

public class HighPrivilegeCommand {

    private static HighPrivilegeCommand mHighPrivilegeCommand = new HighPrivilegeCommand();
    public static boolean isDebug = true;

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
     * 运行高权限命令
     * @param cmd
     * @return
     */
    public static String execHighPrivilegeCmd(String cmd) {
        if (RootCommand.ready()) {
            return RootCommand.execRootCmd(cmd, null, true, null).toString();
        }
        return AdbCommand.execAdbCmd(cmd, 0);
    }
}
