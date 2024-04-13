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

//    @Override
//    public byte[] serialize() {
//        String dataString = this.getCode().getCode() + "," + this.reqNo;
//        return dataString.getBytes();
//    }
//
//    //Create a deserilization class?
//    public static PublishedMessage deserialize(byte[] data) {
//        String[] parts = new String(data).split(",");
//        int reqNo = Integer.parseInt(parts[1]);
//        return new PublishedMessage(reqNo);
//    }
}
