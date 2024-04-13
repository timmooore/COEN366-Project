package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Client {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    private static final int BUFFER_SIZE = 1024;
    private static String name;

    private final HashMap<String, Set<String>> clientFiles = new HashMap<>();

    private static class ClientTask implements Runnable {
        private final String clientName;
        private final int reqNo; // Request number for this instance of ClientTask
        private final Code code;
        private List<String> filesToPublish; // Only used for PUBLISH
        private String fileName;  // For FILE_REQ
        private final DatagramSocket socket;
        private int TCPSocket;  // For FILE_CONF


        // TODO: This might get hard to manage as more messages are added, esp. if
        //       the signatures are the same
        // Constructor for REGISTER and DE_REGISTER
        public ClientTask(DatagramSocket socket, String clientName, int reqNo, Code code) {
            this.socket = socket;
            this.clientName = clientName;
            this.reqNo = reqNo;
            this.code = code;
            this.filesToPublish = null; // Not used for REGISTER/DE_REGISTER
        }

        // Overloaded constructor for PUBLISH with a list of files
        public ClientTask(DatagramSocket socket, String clientName, int reqNo, Code code, List<String> filesToPublish) {
            this.socket = socket;
            this.clientName = clientName;
            this.reqNo = reqNo;
            this.code = code;
            this.filesToPublish = filesToPublish;
        }

        public ClientTask(DatagramSocket socket, String clientName, int reqNo, Code code, String fileName) {
            this.socket = socket;
            this.clientName = clientName;
            this.reqNo = reqNo;
            this.code = code;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            InetAddress serverAddress;
            try {
                serverAddress = InetAddress.getByName(SERVER_IP);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            byte[] sendData = null;
            DatagramPacket sendPacket;

            switch (code) {
                case REGISTER: {
                    RegisterMessage registerMessage = new RegisterMessage(reqNo, clientName, serverAddress, socket.getLocalPort());
                    sendData = registerMessage.serialize();
                    break;
                }
                case DE_REGISTER: {
                    DeRegisterMessage deRegisterMessage = new DeRegisterMessage(reqNo, clientName);
                    sendData = deRegisterMessage.serialize();
                    break;
                }
                case PUBLISH: {
                    PublishMessage publishMessage = new PublishMessage(reqNo, clientName, filesToPublish);
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
                try {
                    socket.send(sendPacket);
                    System.out.println("Port: " + socket.getLocalPort());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(code + " message sent to server by client " + Thread.currentThread().getName());
            }
        }
    }

    // Private class for handling peer connections and file transfers
    private static class IncomingMessageHandler implements Runnable {
        private final DatagramSocket socket;

        public IncomingMessageHandler(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            while (true) {
                // Wait for response from server
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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
                // Handle other message types as needed
            }
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Port: " + socket.getLocalPort());
            // Start the incoming request handler in a separate thread
            new Thread(new IncomingMessageHandler(socket)).start();

            Scanner scanner = new Scanner(System.in);

            // Prompt user for the client name
            // TODO: Uncomment and revert
//            System.out.println("Enter your Client name: ");
//
//            name = scanner.nextLine();
            name = "Tim";

            int reqNo = 1;

            while (true) {
                System.out.println("Enter message type (1 = REGISTER, 2 = DE_REGISTER, 3 = PUBLISH, 4 = FILE_REQ, 10 = Multiple registers, 0 = exit): ");
                int messageType = scanner.nextInt();


                // TODO: chatGPT generated. Test each that they work
                switch (messageType) {
                    case 1:
                        new Thread(new ClientTask(socket, name, reqNo++, Code.REGISTER)).start();
                        break;
                    case 2:
                        new Thread(new ClientTask(socket, name, reqNo++, Code.DE_REGISTER)).start();
                        break;
                    case 3:
                        List<String> filesToPublish = Arrays.asList("file1.txt", "file2.txt");
                        new Thread(new ClientTask(socket, name, reqNo++, Code.PUBLISH, filesToPublish)).start();
                        break;
                    case 4:
                        scanner.nextLine(); // Consume the newline character
                        System.out.println("Enter filename for FILE_REQ: ");
                        String fileName = scanner.nextLine().trim();
                        new Thread(new ClientTask(socket, name, reqNo++, Code.FILE_REQ, fileName)).start();
                        break;
                    case 10:
                        // Register clients with names Client1 -> Client5
                        for (int i = 1; i <= 5; i++) {
                            String tmpClientName = name + i;
                            try (DatagramSocket tmpSocket = new DatagramSocket(0)) {
                                Thread t = new Thread(new ClientTask(tmpSocket, tmpClientName, reqNo++, Code.REGISTER));
                                t.start();
                                t.join();
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case 0:
                        // TODO: Make sure resources are closed
                        return;
                    default:
                        System.out.println("Invalid message type.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
