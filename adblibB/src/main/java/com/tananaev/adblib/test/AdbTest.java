package com.tananaev.adblib.test;

import android.util.Log;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

public class AdbTest {
    public static void main(String[] args) throws Exception {
        testConnect1();
//        test();
//        testPush();
    }


    private static AdbCrypto crypto = null;
    private static Socket socket = null;
    private static AdbConnection connection = null;

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    private static AdbCrypto setupCrypto(String pubKeyFile, String privKeyFile)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        AdbBase64 base = new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.getEncoder().encodeToString(arg0);
            }
        };
        File pub = new File(pubKeyFile);
        File priv = new File(privKeyFile);
        AdbCrypto c = null;

        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            c = AdbCrypto.loadAdbKeyPair(base, priv, pub);
        }

        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(base);
            // Save it
            c.saveAdbKeyPair(priv, pub);
            System.out.println("Generated new keypair");
        } else {
            System.out.println("Loaded existing keypair");
        }
        return c;
    }

    private static void prepareStep() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InterruptedException {
        if (crypto == null) {
            // Setup the crypto object required for the AdbConnection
            crypto = setupCrypto("pub.key", "priv.key");
        }

        if (connection == null) {
            socket = new Socket("192.168.50.111", 5555);

            try {
                connection = AdbConnection.create(socket, crypto);
                connection.connect(Long.MAX_VALUE, TimeUnit.MILLISECONDS, true);
            } catch (Exception e) {
                System.out.println("On the target device, check 'Always allow from this computer' and press Allow");
                connection = AdbConnection.create(socket, crypto);
                connection.connect();
            }
        }
    }

    private static void testPush() throws Exception {
        prepareStep();
        AdbStream stream = connection.open("sync:");
        byte[] response = stream.read();
        String responseText = new String(response, StandardCharsets.UTF_8);
        System.out.println(responseText);
    }


    private static void test() throws Exception {
        prepareStep();
        // test one  print
        AdbStream stream = connection.open("shell:echo Hello world");
        Thread.sleep(1000); // Giving the peer time to send us the "close stream" message
        byte[] response = stream.read();
        String responseText = new String(response, StandardCharsets.UTF_8);
        if ("Hello world".equals(responseText.trim())) {
            i("测试打印成功。 测试方法[shell:echo Hello world]");
        } else {
            e("测试打印失败。 测试方法[shell:echo Hello world], 结果:" + responseText);
        }
        // test stream flush sth
        stream = connection.open("shell:"); // Starting empty shell so it won't self-close
        stream.write("echo \"Hello world\"");
        Thread.sleep(1000); // Giving the peer time to run the command and send the output back

        response = stream.read();
        responseText = new String(response, StandardCharsets.UTF_8);
        if ("Hello world".equals(responseText.trim())) {
            i("测试流式操作成功。 测试方法[shell:--->echo Hello world]");
        } else {
            e("测试流式操作失败。 测试方法[shell:--->echo Hello world], 结果:" + responseText);
        }

    }


    /**
     * 测试连接
     */
    private static void testConnect1() throws IOException, NoSuchAlgorithmException, InterruptedException {
        Socket socket = new Socket("192.168.4.223", 5555);

        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
            @Override
            public String encodeToString(byte[] data) {
                return Base64.getEncoder().encodeToString(data);
            }
        });

        AdbConnection connection = AdbConnection.create(socket, crypto);
        connection.connect();

        AdbStream stream = connection.open("shell:getprop");
        byte[] bs = stream.read();
        System.out.println(new String(bs));
    }

    public static void i(String e) {
        Log.i("sanbo.adbTestB", e);
    }

    public static void e(String e) {
        Log.e("sanbo.adbTestB", e);
    }
}
