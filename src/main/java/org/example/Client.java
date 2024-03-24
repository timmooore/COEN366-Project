package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Client implements Runnable {
     public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    public static final long EXCHANGE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static final int BUFFER_SIZE = 1024;

    public void run() {
        // Open client DatagramSocket on any available port
        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Connected to server.");

            // ChatGPT example
            String message = "Hello, server!";
            byte[] sendData = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

            // Create packet to send to server
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);

            // Send packet to server
            socket.send(sendPacket);
            System.out.println("Message sent to server.");

            // Receive response from server
            byte[] receiveData = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // Wait for response packet from server
            socket.receive(receivePacket);

            // Process response
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received from server: " + receivedMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
