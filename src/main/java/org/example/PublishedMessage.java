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

    @Override
    public byte[] serialize() {
       
        String dataString = this.getCode().getCode() + "," + this.reqNo;
        return dataString.getBytes();
    }
    
}
