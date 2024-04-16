package org.example;

public class FileErrorMessage extends Message {
    private final int reqNo;
    private final String reason;

    public FileErrorMessage(int reqNo, String reason) {
        super(Code.FILE_ERROR);
        this.reqNo = reqNo;
        this.reason = reason;
    }

    public int getReqNo() { return reqNo; }

    public String getReason() { return reason; }
}
