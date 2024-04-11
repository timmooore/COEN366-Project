package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Client {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    private static final int BUFFER_SIZE = 1024;

    private final HashMap<String, Set<String>> clientFiles = new HashMap<>();

    private static class ClientTask implements Runnable {
        private final int reqNo; // Request number for this instance of ClientTask
        private final Code code;
        private List<String> filesToPublish; // Only used for PUBLISH
        private String fileName;  // For FILE_REQ
        private int TCPSocket;// For FILE_CONF

        // Constructor for REGISTER and DE_REGISTER
        public ClientTask(int reqNo, Code code) {
            this.reqNo = reqNo;
            this.code = code;
            this.filesToPublish = null; // Not used for REGISTER/DE_REGISTER
        }

        // Overloaded constructor for PUBLISH with a list of files
        public ClientTask(int reqNo, Code code, List<String> filesToPublish) {
            this.reqNo = reqNo;
            this.code = code;
            this.filesToPublish = filesToPublish;
        }

        public ClientTask(int reqNo, Code code, String fileName) {
            this.reqNo = reqNo;
            this.code = code;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);

                byte[] sendData = null;
                DatagramPacket sendPacket = null;

                // TODO: Client + reqNo is not a good name. Should be unique to each client
                switch (code) {
                    case REGISTER: {
                        String name = "Client" + reqNo;
                        RegisterMessage registerMessage = new RegisterMessage(reqNo, name, serverAddress, SERVER_PORT);
                        sendData = registerMessage.serialize();
                        break;
                    }
                    case DE_REGISTER: {
                        String name = "Client" + reqNo;
                        DeRegisterMessage deRegisterMessage = new DeRegisterMessage(reqNo, name);
                        sendData = deRegisterMessage.serialize();
                        break;
                    }
                    case PUBLISH: {
                        String name = "Client" + reqNo; // Example name, adjust as needed
                        PublishMessage publishMessage = new PublishMessage(reqNo, name, filesToPublish);
                        sendData = publishMessage.serialize();
                        break;
                    }
                    case FILE_REQ:
                        FileReqMessage fileReqMessage = new FileReqMessage(reqNo, fileName);
                        sendData = fileReqMessage.serialize();
                        break;
                    // Add other cases as necessary
                }

                if (sendData != null) {
                    sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                    socket.send(sendPacket);
                    System.out.println(code + " message sent to server by client " + Thread.currentThread().getName());
                }

                // Wait for response from server
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                // Deserialize the received data into a Message
                Message receivedMessage = Message.deserialize(receivePacket.getData());
                if (receivedMessage instanceof PublishedMessage) {
                    System.out.println("PUBLISHED received by client " + Thread.currentThread().getName());
                } else if (receivedMessage instanceof PublishDeniedMessage) {
                    PublishDeniedMessage deniedMessage = (PublishDeniedMessage) receivedMessage;
                    System.out.println("PUBLISH-DENIED received by client " + Thread.currentThread().getName() + 
                        ": Reason: " + deniedMessage.getReason());
                } else {
                    // Handle other responses or unknown message types
                    System.out.println("Received an unrecognized message from the server.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        // Example usage
        int reqNo = 1; // Start with request number 1

        // Register clients
        for (int i = 1; i <= 5; i++) {
            new Thread(new ClientTask(reqNo++, Code.REGISTER)).start();
        }

        // Example publishing
        List<String> filesToPublish = Arrays.asList("file1.txt", "file2.txt");
        new Thread(new ClientTask(reqNo++, Code.PUBLISH, filesToPublish)).start();

        // De-register a client as an example
        new Thread(new ClientTask(reqNo, Code.DE_REGISTER)).start();
    }
}
