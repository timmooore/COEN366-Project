package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Client {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    private static final int BUFFER_SIZE = 1024;
    private static String name;

    private final HashMap<String, Set<String>> clientFiles = new HashMap<>();

    private static class ClientTask implements Runnable {
        private final int reqNo; // Request number for this instance of ClientTask
        private final Code code;
        private List<String> filesToPublish; // Only used for PUBLISH
        private String fileName;  // For FILE_REQ
        private int TCPSocket;  // For FILE_CONF


        // TODO: This might get hard to manage as more messages are added, esp. if
        //       the signatures are the same
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
                DatagramPacket sendPacket;

                switch (code) {
                    case REGISTER: {
                        RegisterMessage registerMessage = new RegisterMessage(reqNo, Client.name, serverAddress, SERVER_PORT);
                        sendData = registerMessage.serialize();
                        break;
                    }
                    case DE_REGISTER: {
                        DeRegisterMessage deRegisterMessage = new DeRegisterMessage(reqNo, Client.name);
                        sendData = deRegisterMessage.serialize();
                        break;
                    }
                    case PUBLISH: {
                        PublishMessage publishMessage = new PublishMessage(reqNo, Client.name, filesToPublish);
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

                if (receivedMessage.getCode() == Code.REGISTERED) {
                    RegisteredMessage rm = (RegisteredMessage) receivedMessage;
                    System.out.println("Received from server by client "
                            + Thread.currentThread().getName()
                            + ": Code: " + rm.getCode()
                            + ", REQ#: " + rm.getReqNo());
                } else if (receivedMessage.getCode() == Code.REGISTER_DENIED) {
                    RegisterDeniedMessage rdm = (RegisterDeniedMessage) receivedMessage;
                    System.out.println("Received from server by client "
                            + Thread.currentThread().getName()
                            + ": Code: " + rdm.getCode()
                            + ", REQ#: " + rdm.getReqNo()
                            + ", Reason: " + rdm.getReason());
                } else if (receivedMessage instanceof PublishedMessage) {
                    System.out.println("PUBLISHED received by client " + Thread.currentThread().getName());
                } else if (receivedMessage instanceof PublishDeniedMessage deniedMessage) {
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

    // Private class for handling peer connections and file transfers
    private static class HandleIncomingRequest implements Runnable {
        private final DatagramPacket packet;

        public HandleIncomingRequest(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            // Deserialize the received data into a Message
            Message receivedMessage = Message.deserialize(packet.getData());

            // Handle the received message based on its type
            // Implement your logic here based on the type of receivedMessage
            // For example:
            if (receivedMessage.getCode() == Code.FILE_REQ) {
                FileReqMessage fileReqMessage = (FileReqMessage) receivedMessage;
                // Process file request and send the file back or respond accordingly
                System.out.println("Received FILE_REQ from client: " + fileReqMessage.getFileName());
            }
            // Handle other message types as needed
        }
    }

    public static void main(String[] args) {
        // Start the incoming request handler in a separate thread
        // new Thread(new IncomingRequestHandler()).start();

        // Example usage
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your Client name: ");

        name = scanner.nextLine();

        int reqNo = 1;

        while (true) {
            System.out.println("Enter message type (1 = REGISTER, 2 = DE_REGISTER, 3 = PUBLISH, 4 = FILE_REQ, 10 = Multiple registers, 0 = exit): ");
            int messageType = scanner.nextInt();

            // TODO: chatGPT generated. Test each that they work
            switch (messageType) {
                case 1:
                    new Thread(new ClientTask(reqNo++, Code.REGISTER)).start();
                    break;
                case 2:
                    new Thread(new ClientTask(reqNo++, Code.DE_REGISTER)).start();
                    break;
                case 3:
                    List<String> filesToPublish = Arrays.asList("file1.txt", "file2.txt");
                    new Thread(new ClientTask(reqNo++, Code.PUBLISH, filesToPublish)).start();
                    break;
                case 4:
                    scanner.nextLine(); // Consume the newline character
                    System.out.println("Enter filename for FILE_REQ: ");
                    String fileName = scanner.nextLine().trim();
                    new Thread(new ClientTask(reqNo++, Code.FILE_REQ, fileName)).start();
                    break;
                case 10:
                    // Register clients with names Client1 -> Client5
                    String tmpClientName = name;
                    for (int i = 1; i <= 5; i++) {
                        name = tmpClientName + i;
                        new Thread(new ClientTask(reqNo++, Code.REGISTER)).start();
                    }
                    name = tmpClientName;
                    break;
                case 0:
                    // TODO: Make sure resources are closed
                    return;
                default:
                    System.out.println("Invalid message type.");
            }
        }
    }

//    public static void main(String[] args) {
//        // Example usage
//        int reqNo = 1; // Start with request number 1
//
//        // Register clients
//        for (int i = 1; i <= 5; i++) {
//            new Thread(new ClientTask(reqNo++, Code.REGISTER)).start();
//        }
//
//        // Example publishing
//        List<String> filesToPublish = Arrays.asList("file1.txt", "file2.txt");
//        new Thread(new ClientTask(reqNo++, Code.PUBLISH, filesToPublish)).start();
//
//        // De-register a client as an example
//        new Thread(new ClientTask(reqNo, Code.DE_REGISTER)).start();
//    }
}
