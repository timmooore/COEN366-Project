package org.example;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateMessage extends Message implements Serializable {
    private final int reqNo; // Request number
//    private final Map<String, InetAddress> registeredClients;
//    private final Map<String, Set<String>> clientFiles;
    private final HashSet<ClientInfo> clientInfoSet;

    public UpdateMessage(int reqNo, HashSet<ClientInfo> clientInfoSet) {
        super(Code.UPDATE);
        this.reqNo = reqNo;
        this.clientInfoSet = clientInfoSet;
//        this.registeredClients = registeredClients;
//        this.clientFiles = clientFiles;
    }

    public int getReqNo() {
        return reqNo;

    }

    public HashSet<ClientInfo> getClientInfoSet() {
        return clientInfoSet;
    }

    //    public Map<String, InetAddress> getRegisteredClients() {
//        return registeredClients;
//    }
//
//    public Map<String, Set<String>> getClientFiles() {
//        return clientFiles;
//    }
}
