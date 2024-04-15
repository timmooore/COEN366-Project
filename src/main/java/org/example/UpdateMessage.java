package org.example;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

public class UpdateMessage extends Message implements Serializable {
    private final int reqNo; // Request number
    private final Map<String, InetAddress> registeredClients;
    private final Map<String, Set<String>> clientFiles;

    public UpdateMessage(int reqNo, Map<String, InetAddress> registeredClients, Map<String, Set<String>> clientFiles) {
        super(Code.UPDATE);
        this.reqNo = reqNo;
        this.registeredClients = registeredClients;
        this.clientFiles = clientFiles;
    }

    public int getReqNo() {
        return reqNo;

    }
    public Map<String, InetAddress> getRegisteredClients() {
        return registeredClients;
    }

    public Map<String, Set<String>> getClientFiles() {
        return clientFiles;
    }


    @Override
    public byte[] serialize() {
        // Serialize the update message to a byte array
        String dataString = this.getCode().getCode() + "," + this.reqNo;
        return dataString.getBytes();
    }

    // Deserialize the update message from a byte array
    public static UpdateMessage deserialize(byte[] data) {
        // Split the data string and extract the request number
        String[] parts = new String(data).split(",");
        int reqNo = Integer.parseInt(parts[1]);
        return new UpdateMessage(reqNo,null, null);
    }
}
