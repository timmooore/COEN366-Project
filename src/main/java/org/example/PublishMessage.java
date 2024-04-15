package org.example;

import java.util.Arrays;
import java.util.List;

public class PublishMessage extends Message {
    private final int reqNo;
    private final String name;
    private final List<String> files;

    public PublishMessage(int reqNo, String name, List<String> files) {
        super(Code.PUBLISH);
        this.reqNo = reqNo;
        this.name = name;
        this.files = files;
    }

    public int getReqNo() {
        return reqNo;
    }

    public String getName() {
        return name;
    }

    public List<String> getFiles() {
        return files;
    }

//    @Override
//    public byte[] serialize() {
//        // Assuming a simple serialization to a comma-separated string
//        String filesString = String.join(";", this.files); // Using semicolon to separate files
//        String dataString = this.reqNo + "," + this.name + "," + filesString;
//        return dataString.getBytes();
//    }
//
//    //Create a deserilization class?
//    public static PublishMessage deserialize(byte[] data) {
//        String[] parts = new String(data).split(",", 3);
//        int reqNo = Integer.parseInt(parts[0]);
//        String name = parts[1];
//        List<String> files = Arrays.asList(parts[2].split(";"));
//        return new PublishMessage(reqNo, name, files);
//    }
}
