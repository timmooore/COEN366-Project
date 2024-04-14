package org.example;

public class RemoveDeniedMessage extends Message {
    private final int reqNo;
    private final String reason;

    public RemoveDeniedMessage(int reqNo, String reason) {
        super(Code.REMOVE_DENIED);
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
