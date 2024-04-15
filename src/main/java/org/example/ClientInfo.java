package org.example;
import java.net.InetAddress;
import java.util.Set;


    public class ClientInfo {
        private final String name;
        private final InetAddress ipAddress;
        private final int udpPort;
        private final Set<String> files;

        public ClientInfo(String name, InetAddress ipAddress, int udpPort, Set<String> files) {
            this.name = name;
            this.ipAddress = ipAddress;
            this.udpPort = udpPort;
            this.files = files;
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


