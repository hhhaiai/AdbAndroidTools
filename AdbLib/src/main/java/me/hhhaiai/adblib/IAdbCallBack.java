package me.hhhaiai.adblib;

public interface IAdbCallBack {
    // 发生异常
    public abstract void onError(Throwable exception);

    // 链接成功
    public abstract void onSuccess();

}
