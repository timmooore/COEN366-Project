package org.example;

public class PublishedMessage extends Message {
    private final int reqNo;

    public PublishedMessage(int reqNo) {
        super(Code.PUBLISHED);
        this.reqNo = reqNo;
    }

    public int getReqNo() {
        return reqNo;
    }


}
