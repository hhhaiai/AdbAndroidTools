package me.sanbo.adbtools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import me.hhhaiai.HighPrivilegeCommand;
import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Pools;

public class MainActivity extends Activity {
    private volatile boolean isSuccess = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }


    public void onClick(final View v) {


        if (v.getId() == R.id.btnA) {
            HighPrivilegeCommand.debug(true).context(MainActivity.this).build(new IAdbCallBack() {
                @Override
                public void onError(Throwable exception) {
                    Alog.i("收到异常！！！" + exception.getMessage());
                    isSuccess = false;
                    Pools.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "adb 链接失败！！ 原因:\r\n" + Log.getStackTraceString(exception), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onSuccess() {
                    isSuccess = true;
                    Alog.i("成功！！！");
                    Pools.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "adb 链接成功！！！", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            });
        } else if (v.getId() == R.id.btnB) {
            if (!isSuccess) {
                Toast.makeText(MainActivity.this, "[" + isSuccess + "] adb 认证失败，请先检查后再玩吧~~", Toast.LENGTH_LONG).show();
                return;
            }
            String res = HighPrivilegeCommand.execHighPrivilegeCmd("dumpsys window | grep mCurrentFocus");
            Alog.i("[" + isSuccess + "]execHighPrivilegeCmd result:" + res);
            Toast.makeText(MainActivity.this, "[" + isSuccess + "]execHighPrivilegeCmd result:" + res, Toast.LENGTH_LONG).show();

        }
    }

}