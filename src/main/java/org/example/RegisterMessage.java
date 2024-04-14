package org.example;

import java.net.InetAddress;

public class RegisterMessage extends Message {
    private final int reqNo;
    private final String name;
    private final InetAddress ipAddress;
    private final int udpPort;

    public RegisterMessage(int reqNo, String name, InetAddress ipAddress, int udpPort) {
        super(Code.REGISTER);
        this.reqNo = reqNo;
        this.name = name;
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
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
}
