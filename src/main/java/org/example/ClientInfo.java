package org.example;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;


    public class ClientInfo implements Serializable {
        private final String name;
        private final InetAddress ipAddress;
        private final int udpPort;
        private final HashSet<String> files = new HashSet<>();

        public ClientInfo(String name, InetAddress ipAddress, int udpPort) {
            this.name = name;
            this.ipAddress = ipAddress;
            this.udpPort = udpPort;
        }

        public String getName() {
            return name;
        }

        public InetAddress getIpAddress() {
            return ipAddress;
        }

        public int getUdpPort() {
            return udpPort;
        }

        public Set<String> getFiles() {
            return files;
        }
    }


