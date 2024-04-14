package org.example;

public class RemovedMessage extends Message {
    private final int reqNo;

    public RemovedMessage(int reqNo) {
        super(Code.REMOVED);
        this.reqNo = reqNo;
    }

    public int getReqNo() {
        return reqNo;
    }
}
