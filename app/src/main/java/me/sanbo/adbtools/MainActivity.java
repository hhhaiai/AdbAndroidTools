package me.sanbo.adbtools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.ref.Reflect;

public class MainActivity extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
//        HidenCall.unseal();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void onClick(final View v) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (v.getId() == R.id.btnA) {
//                    boolean res = AdbCommand.context(MainActivity.this).generateConnection();
//                    if (!res) {
//                        runOnUiThread(new Runnable() {
//                            @SuppressLint("NewApi")
//                            @Override
//                            public void run() {
//                                Toast.makeText(MainActivity.this, "请在命令行执行命令[adb tcpip 5555]", Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    } else {
//                        runOnUiThread(new Runnable() {
//                            @SuppressLint("NewApi")
//                            @Override
//                            public void run() {
//                                Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//                    i("generateConnection result:" + res);
                } else if (v.getId() == R.id.btnB) {
//                    String res = AdbCommand.execHighPrivilegeCmd("dumpsys window | grep mCurrentFocus");
//                    i("execHighPrivilegeCmd result:" + res);


                    Object activityThread = Reflect.onClass("android.app.ActivityThread").call("currentActivityThread").get();
                    Object application = Reflect.on(activityThread).call("getApplication");
                    Alog.i("activityThread:" + activityThread
                            + "\r\napplication:" + application
                    );
                }
            }
        }).start();
    }

    private void i(String s) {
        Log.println(Log.INFO, "sanbo", s);
    }
}