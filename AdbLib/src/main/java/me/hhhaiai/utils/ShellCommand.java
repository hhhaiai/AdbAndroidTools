package me.hhhaiai.utils;

import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ShellCommand {
    private static Boolean isRoot = null;

    /**
     * 高版本，直接失败
     * @return
     */
    public static boolean ready() {
        if (isRoot != null) {
            if (isRoot) {
                return isRoot;
            }
        }

        String[] suPaths = {
                "/sbin/",
                "/system/bin/",
                "/system/xbin/",
                "/system/sbin/",
                "/vendor/bin/",
                "/su/bin/",
                "/system/sd/xbin/",
                "/system/bin/failsafe/",
                "/system/bin/failsafe/",
                "/data/local/xbin/",
                "/data/local/bin/",
                "/system/sd/xbin/",
                "/data/local/",
                "/system/bin/failsafe/"
        };
        String[] apkPaths = {
                "/system/app/Superuser.apk",
                "/system/priv-app/Superuser.apk"
        };
        try {
            //1. find the su files
            for (String path : suPaths) {
                Alog.i("path:" + path
                        + "\r\nsu:" + new File(path + "su").exists()
                        + "\r\nmysu:" + new File(path + "mysu").exists()
                );
                if (new File(path + "su").exists() || new File(path + "mysu").exists()) {
                    isRoot = true;
                    return isRoot;
                }
            }
            for (String path : apkPaths) {
                if (new File(path).exists()) {
                    isRoot = true;
                    return isRoot;
                }
            }
            String[] gg = {"which", "type"};

            // 2. get su file path
            for (String g : gg) {
                String execResult = ShellCommand.exec(g + " su");
                //!"su not found".equals(execResult)
                if (!Texts.isEmpty(execResult) && !execResult.contains("not found")) {
                    isRoot = true;
                    return isRoot;
                }
                execResult = ShellCommand.exec(g + " mysu");
                //!"su not found".equals(execResult)
                if (!Texts.isEmpty(execResult) && !execResult.contains("not found")) {
                    isRoot = true;
                    return isRoot;
                }
            }
            // 3. adb shell getprop ro.secure 返回值1则未root，返回值为0则已root
            String r1 = ShellCommand.exec("getprop ro.secure");
            if (!Texts.isEmpty(r1) && Texts.equals(r1, "0")) {
                return true;
            }
            // 4. adb shell su


        } catch (Throwable igone) {
            Alog.e(igone);
        }
        isRoot = false;
        return false;
    }

    public static String exec(String... cmds) {
        return run("sh", cmds);
    }

    public static String su(String... cmds) {
        return run("su", cmds);
    }

    private static String run(String base, String... cmds) {
        if (cmds == null || cmds.length < 1) {
            return null;
        }
        //check and makesure is root/non-root mode
        if (!Texts.isEmpty(base) && Texts.equals(base, "su")) {
            base = "su";
            if (isRoot == null) {
                ready();
            }
            if (!isRoot) {
                Alog.e("not root mode, please check!");
                return null;
            }
        } else {
            base = "sh";
        }

        String result = "";
        Process proc = null;
        BufferedInputStream in = null;
        BufferedReader br = null;
        InputStreamReader is = null;
        InputStream ii = null;
        StringBuilder sb = new StringBuilder();
        DataOutputStream os = null;
        OutputStream pos = null;
        try {
            proc = Runtime.getRuntime().exec(base);
            pos = proc.getOutputStream();
            os = new DataOutputStream(pos);

            for (String cmd : cmds) {
                // donnot use os.writeBytes(commmand), avoid chinese charset error
                os.write(cmd.getBytes());
                os.writeBytes("\n");
                os.flush();
            }

            // exitValue
            os.writeBytes("exit\n");
            os.flush();
            ii = proc.getInputStream();
            in = new BufferedInputStream(ii);
            is = new InputStreamReader(in);
            br = new BufferedReader(is);
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            if (sb.length() > 0) {
                return sb.substring(0, sb.length() - 1);
            }
            result = String.valueOf(sb);
            if (!TextUtils.isEmpty(result)) {
                result = result.trim();
            }
//        } catch (FileNotFoundException e) {
//            //igone
        } catch (Throwable e) {
            Alog.e(e);
        } finally {
            Streams.close(pos, ii, br, is, in, os);
        }

        return result;
    }

}
