package org.example;

public class PublishDeniedMessage extends Message {
    private final int reqNo;
    private final String reason;

    public PublishDeniedMessage(int reqNo, String reason) {
        super(Code.PUBLISH_DENIED);
        this.reqNo = reqNo;
        this.reason = reason;
    }

    public int getReqNo() {
        return reqNo;
    }

    public String getReason() {
        return reason;
    }

}
