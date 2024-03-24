package org.example;

import java.io.*;
import java.net.InetAddress;

public class Message implements Serializable {
    public static final int REGISTER = 1;
    private static final int REGISTERED = 2;
    private final int code;
    private final int reqNo;
    private final String name;
    private final InetAddress ipAddress;
    private final int udpPort;

    public Message(int code, int reqNo, String name, InetAddress ipAddress, int udpPort) {
        this.code = code;
        this.reqNo = reqNo;
        this.name = name;
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
    }

    public int getCode() {
        return code;
    }

    public int getReqNo() {
        return reqNo;
    }

    public String getName() {
        return name;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public byte[] serialize() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            // Write the object to the byte stream
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Get the byte array from the byte stream
        return byteStream.toByteArray();
    }

    // Deserialize bytes back to object
    public static Message deserialize(byte[] bytes) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Message) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Handle deserialization error
            throw new RuntimeException(e);
        }
    }
}
