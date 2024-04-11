package org.example;

public class FileReqMessage extends Message {
    private final String fileName;
    private final int reqNo;

    public FileReqMessage(int reqNo, final String fileName) {
        super(Code.FILE_REQ);
        this.reqNo = reqNo;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getReqNo() {
        return reqNo;
    }
}
