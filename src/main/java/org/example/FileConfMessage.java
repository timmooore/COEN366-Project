package org.example;

public class FileConfMessage extends Message {
    private final int reqNo;
    private final int TCPPort;

    public FileConfMessage(Code code, int reqNo, int TCPPort) {
        super(Code.FILE_CONF);
        this.reqNo = reqNo;
        this.TCPPort = TCPPort;
    }

    public int getReqNo() {
        return reqNo;
    }

    public int getTCPPort() {
        return TCPPort;
    }
}
