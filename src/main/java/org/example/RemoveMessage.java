package org.example;

import java.util.List;

public class RemoveMessage extends Message {
    private final int reqNo;
    private final String name;
    private final List<String> files;

    public RemoveMessage(int reqNo, String name, List<String> files) {
        super(Code.REMOVE);
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
}
