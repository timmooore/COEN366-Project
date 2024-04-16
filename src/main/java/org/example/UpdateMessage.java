package org.example;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateMessage extends Message implements Serializable {
    private final HashSet<ClientInfo> clientInfoSet;

    public UpdateMessage(HashSet<ClientInfo> clientInfoSet) {
        super(Code.UPDATE);
        this.clientInfoSet = clientInfoSet;
    }

    public HashSet<ClientInfo> getClientInfoSet() {
        return clientInfoSet;
    }

}
