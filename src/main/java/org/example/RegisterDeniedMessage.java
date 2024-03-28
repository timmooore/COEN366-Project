package org.example;

public class RegisterDeniedMessage extends Message {
    private final int reqNo;
    private final String reason;

    public RegisterDeniedMessage(int reqNo, String reason) {
        super(Code.REGISTER_DENIED);
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
