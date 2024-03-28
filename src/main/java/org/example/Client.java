package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Client {
     public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    // public static final long EXCHANGE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds
    private static final int BUFFER_SIZE = 1024;
    private int reqNo = 1;  // REQ# numbers the requests of each Client

    public static void main(String[] args) {
        int reqNo = 1;
        // Number of clients to run
        int numClients = 5;

        for (int i = 0; i < 1; i++) {
            new Thread(new ClientTask(reqNo)).start();
            ++reqNo;
        }

        // TODO: Implement some sort of input CLI to choose messages

    }

    private static class ClientTask implements Runnable {
        private int reqNo; // Request number for this instance of ClientTask

        public ClientTask(int reqNo) {
            this.reqNo = reqNo;
        }
        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

                // Create a register message for each client
                RegisterMessage register = new RegisterMessage(reqNo++, "Client", serverAddress, SERVER_PORT);
                byte[] sendData = register.serialize();

                // Create packet to send to server
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);

                // Send packet to server
                socket.send(sendPacket);
                System.out.println("Message sent to server by client " + Thread.currentThread().getName());

                // Receive response from server
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // Wait for response packet from server
                socket.receive(receivePacket);

                // Deserialize the received data into a RegisteredMessage
                RegisteredMessage receivedMessage = (RegisteredMessage) Message.deserialize(receivePacket.getData());
                System.out.println("Received from server by client " + Thread.currentThread().getName() + ": Code: " + receivedMessage.getCode() + ", REQ#: " + receivedMessage.getReqNo());
//            // Process response
//            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
//            System.out.println("Received from server: " + receivedMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
