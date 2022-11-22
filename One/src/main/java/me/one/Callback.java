package me.one;

public interface Callback {
    void onSuccess(String adbResponse);
    void onFail(String failString);
}
