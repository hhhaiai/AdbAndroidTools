package me.sanbo.adbtools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cgutman.adblib.ADBCmd;

public class MainActivity extends Activity {

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
                    boolean res = ADBCmd.context(MainActivity.this).generateConnection();
                    if (!res) {
                        runOnUiThread(new Runnable() {
                            @SuppressLint("NewApi")
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "请在命令行执行命令[adb tcpip 5555]", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @SuppressLint("NewApi")
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    i("generateConnection result:" + res);
                } else if (v.getId() == R.id.btnB) {
                    String res = ADBCmd.exec("dumpsys window | grep mCurrentFocus");
                    i("B result:" + res);

                } else if (v.getId() == R.id.btnC) {
                    String res = ADBCmd.exec("dumpsys window w | grep name=");
                    i("C result:" + res);
                } else if (v.getId() == R.id.btnD) {
                    String res = ADBCmd.exec("dumpsys activity");
                    i("D result:" + res);
                } else if (v.getId() == R.id.btnE) {

                    i("context:" + ADBCmd.getContext());
                }
            }
        }).start();
    }

    private void i(String s) {
        Log.println(Log.INFO, "sanbo", s);
    }
}