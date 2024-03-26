package org.example;

public class RegisteredMessage extends Message {
    private final int reqNo;

    public RegisteredMessage(int reqNo) {
        super(Code.REGISTERED);
        this.reqNo = reqNo;
    }

    public int getReqNo() {
        return reqNo;
    }
}
