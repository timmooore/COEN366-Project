package org.example;

public class DeRegisterMessage extends Message {
    private final int reqNo;
    private final String name;

    public DeRegisterMessage(int reqNo, String name) {
        super(Code.DE_REGISTER);
        this.reqNo = reqNo;
        this.name = name;
    }
    public int getReqNo() {
        return reqNo;
    }
    public String getName() {
        return name;
    }
}
