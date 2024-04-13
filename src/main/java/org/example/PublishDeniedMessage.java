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

//    @Override
//    public byte[] serialize() {
//        String dataString = this.getCode().getCode() + "," + this.reqNo + "," + this.reason;
//        return dataString.getBytes();
//    }
//
//     //Create a deserilization class?
//    public static PublishDeniedMessage deserialize(byte[] data) {
//        String[] parts = new String(data).split(",", 3);
//        int reqNo = Integer.parseInt(parts[1]);
//        String reason = parts[2];
//        return new PublishDeniedMessage(reqNo, reason);
//    }
}
