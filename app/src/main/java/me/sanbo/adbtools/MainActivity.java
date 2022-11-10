package me.sanbo.adbtools;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import me.hhhaiai.AwesomeCommand;
import me.hhhaiai.CommandConfig;
import me.hhhaiai.adblib.IAdbCallBack;
import me.hhhaiai.utils.Alog;
import me.hhhaiai.utils.Pools;
import me.hhhaiai.utils.ShellCommand;
import me.hhhaiai.utils.Texts;

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
            CommandConfig.debug(true)
                    .context(MainActivity.this)
                    .build(new IAdbCallBack() {
                        @Override
                        public void onError(Throwable exception) {
                            Alog.i("收到异常！！！" + exception.getMessage());
                            isSuccess = false;
                            show("adb 链接失败！！ 请执行如下命令:"
                                    + "\r\nadb tcpip 5555"
                                    + "\r\n异常:" + Log.getStackTraceString(exception));
                        }

                        @Override
                        public void onSuccess() {
                            isSuccess = true;
                            show("adb 链接成功！！！");
                        }
                    });
        } else if (v.getId() == R.id.btnB) {
            if (!isSuccess) {
                Toast.makeText(MainActivity.this
                                , "[" + isSuccess + "] adb 认证失败，请先检查后再玩吧~~"
                                , Toast.LENGTH_LONG)
                        .show();
                return;
            }
            String shellCmd = "dumpsys window | grep mCurrentFocus";
            //adb shell xxxx
            String res = AwesomeCommand.exec(shellCmd, 0);


            show("命令:" + shellCmd
                    + "\r\n执行结果:" + res);
        } else if (v.getId() == R.id.btnC) {
            boolean isRoot = ShellCommand.ready();
            show("=====root模式检测========" +
                    "\r\nroot模式:" + isRoot
                    + "\r\ntype_su:" + ShellCommand.exec("type su")
                    + "\r\nwhich_su:" + ShellCommand.exec("which su")
                    + "\r\ngetprop:" + ShellCommand.exec("getprop ro.secure")
            );
        } else if (v.getId() == R.id.btnD) {
//            AwesomeCommand.getFps();
            AwesomeCommand.getTopActivityAndProcess(this.getPackageName());
        }
    }

    /**
     * show log
     * @param info
     */
    private void show(String info) {
        if (Texts.isEmpty(info)) {
            return;
        }
        Pools.runOnUiThread(() -> {
            Alog.i(info);
            Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
        });
    }

}