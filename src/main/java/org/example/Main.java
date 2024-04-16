package org.example;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        try {
            // Get the local host address
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Local Host Address: " + localHost.getHostAddress());

            // Get all IP addresses associated with this machine
            InetAddress[] allAddresses = InetAddress.getAllByName(localHost.getHostName());
            System.out.println("All IP Addresses:");
            for (InetAddress address : allAddresses) {
                System.out.println(address.getHostAddress());
            }
        } catch (UnknownHostException e) {
            System.err.println("Error occurred while retrieving IP address: " + e.getMessage());
        }
    }
}