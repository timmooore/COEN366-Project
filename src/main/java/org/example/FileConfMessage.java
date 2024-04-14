package org.example;

public class FileConfMessage extends Message {
    private final int reqNo;
    private final int tcpPort;

    public FileConfMessage(int reqNo, int tcpPort) {
        super(Code.FILE_CONF);
        this.reqNo = reqNo;
        this.tcpPort = tcpPort;
    }

    public int getReqNo() {
        return reqNo;
    }

    public int getTcpPort() {
        return tcpPort;
    }
}
