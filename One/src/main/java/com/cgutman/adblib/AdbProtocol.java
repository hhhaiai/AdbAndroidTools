package com.cgutman.adblib;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;


/**
 * This class provides useful functions and fields for ADB protocol details.
 * @author Cameron Gutman
 */
public class AdbProtocol {

    /** The length of the ADB message header */
    public static final int ADB_HEADER_LENGTH = 24;

    public static final int CMD_SYNC = 0x434e5953;

    /** CNXN is the connect message. No messages (except AUTH)
     * are valid before this message is received. */
    public static final int CMD_CNXN = 0x4e584e43;

    /** The current version of the ADB protocol */
    public static final int CONNECT_VERSION = 0x01000000;

    /** The maximum data payload supported by the ADB implementation */
    public static final int CONNECT_MAXDATA = 4096;
    /** AUTH is the authentication message. It is part of the
     * RSA public key authentication added in Android 4.2.2. */
    public static final int CMD_AUTH = 0x48545541;
    /** This authentication type represents a SHA1 hash to sign */
    public static final int AUTH_TYPE_TOKEN = 1;
    /** This authentication type represents the signed SHA1 hash */
    public static final int AUTH_TYPE_SIGNATURE = 2;
    /** This authentication type represents a RSA public key */
    public static final int AUTH_TYPE_RSA_PUBLIC = 3;
    /** OPEN is the open stream message. It is sent to open
     * a new stream on the target device. */
    public static final int CMD_OPEN = 0x4e45504f;
    /** OKAY is a success message. It is sent when a write is
     * processed successfully. */
    public static final int CMD_OKAY = 0x59414b4f;
    /** CLSE is the close stream message. It it sent to close an
     * existing stream on the target device. */
    public static final int CMD_CLSE = 0x45534c43;
    /** WRTE is the write stream message. It is sent with a payload
     * that is the data to write to the stream. */
    public static final int CMD_WRTE = 0x45545257;
    /** The payload sent with the connect message */
    public static byte[] CONNECT_PAYLOAD;

    static {
		CONNECT_PAYLOAD = "host::\0".getBytes(StandardCharsets.UTF_8);
	}

    /**
     * This function performs a checksum on the ADB payload data.
     * @param payload Payload to checksum
     * @return The checksum of the payload
     */
    private static int getPayloadChecksum(byte[] payload) {
        int checksum = 0;

        for (byte b : payload) {
            /* We have to manually "unsign" these bytes because Java sucks */
            if (b >= 0)
                checksum += b;
            else
                checksum += b + 256;
        }

        return checksum;
    }

    /**
     * This function validate the ADB message by checking
     * its command, magic, and payload checksum.
     * @param msg ADB message to validate
     * @return True if the message was valid, false otherwise
     */
    public static boolean validateMessage(AdbMessage msg) {
        /* Magic is cmd ^ 0xFFFFFFFF */
        if (msg.command != (msg.magic ^ 0xFFFFFFFF))
            return false;

        if (msg.payloadLength != 0) {
			return getPayloadChecksum(msg.payload) == msg.checksum;
        }

        return true;
    }

    /**
     * This function generates an ADB message given the fields.
     * @param cmd Command identifier
     * @param arg0 First argument
     * @param arg1 Second argument
     * @param payload Data payload
     * @return Byte array containing the message
     */
    public static byte[] generateMessage(int cmd, int arg0, int arg1, byte[] payload) {
        /* struct message {
         * 		unsigned command;       // command identifier constant
         * 		unsigned arg0;          // first argument
         * 		unsigned arg1;          // second argument
         * 		unsigned data_length;   // length of payload (0 is allowed)
         * 		unsigned data_check;    // checksum of data payload
         * 		unsigned magic;         // command ^ 0xffffffff
         * };
         */

        ByteBuffer message;

        if (payload != null) {
            message = ByteBuffer.allocate(ADB_HEADER_LENGTH + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        } else {
            message = ByteBuffer.allocate(ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        }

        message.putInt(cmd);
        message.putInt(arg0);
        message.putInt(arg1);

        if (payload != null) {
            message.putInt(payload.length);
            message.putInt(getPayloadChecksum(payload));
        } else {
            message.putInt(0);
            message.putInt(0);
        }

        message.putInt(cmd ^ 0xFFFFFFFF);

        if (payload != null) {
            message.put(payload);
        }

        return message.array();
    }

    /**
     * Generates a connect message with default parameters.
     * @return Byte array containing the message
     */
    public static byte[] generateConnect() {
        return generateMessage(CMD_CNXN, CONNECT_VERSION, CONNECT_MAXDATA, CONNECT_PAYLOAD);
    }

    /**
     * Generates an auth message with the specified type and payload.
     * @param type Authentication type (see AUTH_TYPE_* constants)
     * @param data The payload for the message
     * @return Byte array containing the message
     */
    public static byte[] generateAuth(int type, byte[] data) {
        return generateMessage(CMD_AUTH, type, 0, data);
    }

    /**
     * Generates an open stream message with the specified local ID and destination.
     * @param localId A unique local ID identifying the stream
     * @param dest The destination of the stream on the target
     * @return Byte array containing the message
     * @throws UnsupportedEncodingException If the destination cannot be encoded to UTF-8
     */
    public static byte[] generateOpen(int localId, String dest) throws UnsupportedEncodingException {
        ByteBuffer bbuf = ByteBuffer.allocate(dest.length() + 1);
        bbuf.put(dest.getBytes(StandardCharsets.UTF_8));
        bbuf.put((byte) 0);
        return generateMessage(CMD_OPEN, localId, 0, bbuf.array());
    }

    /**
     * Generates a write stream message with the specified IDs and payload.
     * @param localId The unique local ID of the stream
     * @param remoteId The unique remote ID of the stream
     * @param data The data to provide as the write payload
     * @return Byte array containing the message
     */
    public static byte[] generateWrite(int localId, int remoteId, byte[] data) {
        return generateMessage(CMD_WRTE, localId, remoteId, data);
    }

    /**
     * Generates a close stream message with the specified IDs.
     * @param localId The unique local ID of the stream
     * @param remoteId The unique remote ID of the stream
     * @return Byte array containing the message
     */
    public static byte[] generateClose(int localId, int remoteId) {
        return generateMessage(CMD_CLSE, localId, remoteId, null);
    }

    /**
     * Generates an okay message with the specified IDs.
     * @param localId The unique local ID of the stream
     * @param remoteId The unique remote ID of the stream
     * @return Byte array containing the message
     */
    public static byte[] generateReady(int localId, int remoteId) {
        return generateMessage(CMD_OKAY, localId, remoteId, null);
    }

    /**
     * This class provides an abstraction for the ADB message format.
     * @author Cameron Gutman
     */
    final static class AdbMessage {
        /** The command field of the message */
        public int command;
        /** The arg0 field of the message */
        public int arg0;
        /** The arg1 field of the message */
        public int arg1;
        /** The payload length field of the message */
        public int payloadLength;
        /** The checksum field of the message */
        public int checksum;
        /** The magic field of the message */
        public int magic;
        /** The payload of the message */
        public byte[] payload;

        /**
         * Read and parse an ADB message from the supplied input stream.
         * This message is NOT validated.
         * @param in InputStream object to read data from
         * @return An AdbMessage object represented the message read
         * @throws IOException If the stream fails while reading
         */
        public static AdbMessage parseAdbMessage(InputStream in) throws IOException {
            AdbMessage msg = new AdbMessage();
            ByteBuffer packet = ByteBuffer.allocate(ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);

            /* Read the header first */
            int dataRead = 0;
            do {
                int bytesRead = in.read(packet.array(), dataRead, 24 - dataRead);

                if (bytesRead < 0)
                    throw new IOException("Stream closed");
                else
                    dataRead += bytesRead;
            }
            while (dataRead < ADB_HEADER_LENGTH);

            /* Pull out header fields */
            msg.command = packet.getInt();
            msg.arg0 = packet.getInt();
            msg.arg1 = packet.getInt();
            msg.payloadLength = packet.getInt();
            msg.checksum = packet.getInt();
            msg.magic = packet.getInt();

            /* If there's a payload supplied, read that too */
            if (msg.payloadLength != 0) {
                msg.payload = new byte[msg.payloadLength];

                dataRead = 0;
                do {
                    int bytesRead = in.read(msg.payload, dataRead, msg.payloadLength - dataRead);

                    if (bytesRead < 0)
                        throw new IOException("Stream closed");
                    else
                        dataRead += bytesRead;
                }
                while (dataRead < msg.payloadLength);
            }

            return msg;
        }
    }
}
