package me.hhhaiai.adblib;

/**
 * ABD链接状态回调，提供给用户
 */
public interface IAdbCallBack {
    // 发生异常
    void onError(Throwable exception);

    // 链接成功
    void onSuccess();

}