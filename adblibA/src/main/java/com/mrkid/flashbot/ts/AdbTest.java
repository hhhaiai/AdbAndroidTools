package com.mrkid.flashbot.ts;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;
import com.cgutman.adblib.TcpChannel;
import com.mrkid.flashbot.Base64;
import com.mrkid.flashbot.Push;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;


public class AdbTest {

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

    public static void main(String[] args) throws Exception {
        testShellStream();
//        testPush();
    }

    private static void testPush() throws Exception {
        // Setup the crypto object required for the AdbConnection
        AdbCrypto crypto = setupCrypto("pub.key", "priv.key");

        // Connect the socket to the remote host
        System.out.println("Socket connecting...");
        Socket sock = new Socket("192.168.50.111", 5555);
        System.out.println("Socket connected");

        // Construct the AdbConnection object
        AdbConnection conn = AdbConnection.create(new TcpChannel(sock), crypto);

        // Start the application layer connection process
        System.out.println("ADB connecting...");
        conn.connect();
        System.out.println("ADB connected, will push");

        Push.push(conn, new File("build.sh"), "/data/local/tmp");
        System.out.println("ADB connected,  push over");
    }

    private static void testShellStream() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InterruptedException {
        Scanner in = new Scanner(System.in);

        // Setup the crypto object required for the AdbConnection
        AdbCrypto crypto = setupCrypto("pub.key", "priv.key");

        // Connect the socket to the remote host
        System.out.println("Socket connecting...");
        Socket sock = new Socket("192.168.4.223", 5555);
        System.out.println("Socket connected");

        // Construct the AdbConnection object
        AdbConnection adb = AdbConnection.create(new TcpChannel(sock), crypto);

        // Start the application layer connection process
        System.out.println("ADB connecting...");
        adb.connect();
        System.out.println("ADB connected");

        // Open the shell stream of ADB
        final AdbStream stream = adb.open("shell:  ip address");

        // Start the receiving thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stream.isClosed())
                    try {
                        // Print each thing we read from the shell stream
                        System.out.print(new String(stream.read(), "US-ASCII"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
            }
        }).start();

        // We become the sending thread
        for (; ; ) {
            try {
                stream.write(in.nextLine() + '\n');
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
