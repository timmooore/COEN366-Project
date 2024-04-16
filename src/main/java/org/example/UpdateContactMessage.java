package org.example;
import java.net.InetAddress;

public class UpdateContactMessage extends Message {
    private final int reqNo;
    private final String name;
    private final InetAddress ipAddress;
    private final int udpSocket;

    public UpdateContactMessage(int reqNo, String name, InetAddress ipAddress, int udpSocket) {
        super(Code.UPDATE_CONTACT);

        this.reqNo = reqNo;
        this.name = name;
        this.ipAddress = ipAddress;
        this.udpSocket = udpSocket;
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

    public int getUdpSocket() {
        return udpSocket;
    }

}
