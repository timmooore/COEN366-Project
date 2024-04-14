package org.example;

public class FileMessage extends Message {
    private final int reqNo;
    private final String fileName;
    private final int chunkNo;
    private final String text;

    public FileMessage(int reqNo, String fileName, int chunkNo, String text) {
        super(Code.FILE);
        this.reqNo = reqNo;
        this.fileName = fileName;
        this.chunkNo = chunkNo;
        this.text = text;
    }

    public int getReqNo() {
        return reqNo;
    }

    public String getFileName() {
        return fileName;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public String getText() {
        return text;
    }
}
