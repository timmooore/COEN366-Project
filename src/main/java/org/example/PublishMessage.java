package org.example;

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

    @Override
    public byte[] serialize() {
        // Assuming a simple serialization to a comma-separated string
        String filesString = String.join(";", this.files); // Using semicolon to separate files
        String dataString = this.reqNo + "," + this.name + "," + filesString;
        return dataString.getBytes();
    }

   
}
