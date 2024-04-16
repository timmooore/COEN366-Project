package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    private static final int BUFFER_SIZE = 1024;
    private static String name;

    // TODO: Hash each fileName to a List or Set of clientNames hosting that file
    private static final HashMap<String, HashSet<String>> clientFiles = new HashMap<>();
    private static HashMap<String, ClientInfo> clientInfoHashMap = new HashMap<>();

    private static final HashSet<String> hostedFiles = new HashSet<>();

    // Create a thread pool of 10 to handle up to 10 transfers at once
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    private static class ClientTask implements Runnable {
        //private final String clientName;
        //private final int reqNo; // Request number for this instance of ClientTask
        //private final Code code;
        //private List<String> filesToPublish; // Only used for PUBLISH
        //private String fileName;  // For FILE_REQ
        private final Message message;  // Store the message object
        private final DatagramSocket socket;
        private int tcpSocket;  // For FILE_CONF


        // TODO: This might get hard to manage as more messages are added, esp. if
        //       the signatures are the same -- UPDATE Added single constructor

        public ClientTask(DatagramSocket socket, Message message) {
            this.socket = socket;
            this.message = message;
        }



        /*
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

        // Constructor for FILE-REQ
        public ClientTask(DatagramSocket socket, String clientName, int reqNo, Code code, String fileName) {
            this.socket = socket;
            this.clientName = clientName;
            this.reqNo = reqNo;
            this.code = code;
            this.fileName = fileName;
        }
         */

        @Override
        public void run() {
            try {
                byte[] sendData = message.serialize();
                InetAddress destinationAddress = InetAddress.getByName(SERVER_IP);
                int destinationPort = SERVER_PORT; // Default port, adjust based on message type if needed

                if (message instanceof FileReqMessage) {
                    destinationPort = 4000; // Special port for FILE_REQ if necessary
                }

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress, destinationPort);
                socket.send(sendPacket);
                System.out.println(message.getCode() + " message sent to server by client " + Thread.currentThread().getName());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            /*
            InetAddress destinationAddress;
            int destinationPort = SERVER_PORT;
            try {
                destinationAddress = InetAddress.getByName(SERVER_IP);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            byte[] sendData = null;
            DatagramPacket sendPacket;

            switch (code) {
                case REGISTER: {
                    RegisterMessage registerMessage = new RegisterMessage(reqNo, clientName, destinationAddress, socket.getLocalPort());
                    sendData = registerMessage.serialize();
                    // Handle update confirmation and print the message
                    handleUpdateConfirmation("register");

                    break;
                }
                case DE_REGISTER: {
                    DeRegisterMessage deRegisterMessage = new DeRegisterMessage(reqNo, clientName);
                    sendData = deRegisterMessage.serialize();

                    // Handle update confirmation and print the message
                    handleUpdateConfirmation("deregister");
                    break;
                }
                case PUBLISH: {
                    PublishMessage publishMessage = new PublishMessage(reqNo, clientName, filesToPublish);
                    sendData = publishMessage.serialize();
                    // Handle update confirmation and print the message
                    handleUpdateConfirmation("publish");
                    break;
                }
                case FILE_REQ:
                    // TODO: Get IP address and UDP port of Client by searching for fileName
                    try {
                        destinationAddress = InetAddress.getByName(SERVER_IP);
                        destinationPort = 4000;
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    FileReqMessage fileReqMessage = new FileReqMessage(reqNo, fileName);
                    sendData = fileReqMessage.serialize();
                    break;
                // Add other cases as necessary
            }

            if (sendData != null) {
                sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress, destinationPort);
                try {
                    socket.send(sendPacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(code + " message sent to server by client " + Thread.currentThread().getName());
            }*/
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

                switch (receivedMessage.getCode()) {
                    case REGISTERED:
                        RegisteredMessage rm = (RegisteredMessage) receivedMessage;
                        System.out.println("Received from server by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + rm.getCode()
                                + ", REQ#: " + rm.getReqNo());
                        break;

                    case REGISTER_DENIED:
                        RegisterDeniedMessage rdm = (RegisterDeniedMessage) receivedMessage;
                        System.out.println("Received from server by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + rdm.getCode()
                                + ", REQ#: " + rdm.getReqNo()
                                + ", Reason: " + rdm.getReason());
                        break;

                    case PUBLISHED:
                        PublishedMessage pm = (PublishedMessage) receivedMessage;
                        System.out.println("PUBLISHED received by client " + Thread.currentThread().getName());
                        break;

                    case PUBLISH_DENIED:
                        PublishDeniedMessage pdm = (PublishDeniedMessage) receivedMessage;
                        System.out.println("PUBLISH-DENIED received by client " + Thread.currentThread().getName() +
                                ": Reason: " + pdm.getReason());
                        break;

                    case UPDATE:
                        UpdateMessage um = (UpdateMessage) receivedMessage;
                        System.out.println("Your information has just been updated.");

                        HashSet<ClientInfo> clientInfoSet = um.getClientInfoSet();
                        HashMap<String, ClientInfo> clientInfoHashMap = new HashMap<>();

                        for (ClientInfo clientInfo : clientInfoSet) {
                            clientInfoHashMap.put(clientInfo.getName(), clientInfo);
                        }

                        // Update client's internal state with the latest information
                        // about registered clients and their available files
                        //problem area
                        // For example:
                        updateInternalState(clientInfoHashMap);
                        updateClientFiles();
                        break;

                    case FILE_REQ:
                        FileReqMessage frm = (FileReqMessage) receivedMessage;
                        System.out.println("Received from server by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + frm.getCode()
                                + ", REQ#: " + frm.getReqNo()
                                + ", File Name: " + frm.getFileName());

                        executor.submit(() -> handleFileTransfer(frm, receivePacket));
                        break;

                    case FILE_CONF:
                        FileConfMessage fcm = (FileConfMessage) receivedMessage;
                        System.out.println("Received from server by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + fcm.getCode()
                                + ", REQ#: " + fcm.getReqNo()
                                + ", TCP Port: " + fcm.getTcpPort());

                        executor.submit(() -> receiveFile(receivePacket.getAddress(), fcm.getTcpPort()));
                        break;

                    case REMOVED:
                        RemovedMessage rem = (RemovedMessage) receivedMessage;
                        System.out.println("REMOVE received by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + rem.getCode()
                                + ", REQ#: " + rem.getReqNo());
                        break;

                    case REMOVE_DENIED:
                        RemoveDeniedMessage rddm = (RemoveDeniedMessage) receivedMessage;
                        System.out.println("REMOVE-DENIED received by client "
                                + Thread.currentThread().getName()
                                + ": Code: " + rddm.getCode()
                                + ", REQ#: " + rddm.getReqNo()
                                + ", Reason: " + rddm.getReason());
                        break;

                    default:
                        // Handle other responses or unknown message types
                        System.out.println("Received an unrecognized message from the server.");
                        break;
                }
            }
        }


        private static String readFileToString(String fileName) throws IOException {
            String filePath = "src" + File.separator
                    + "main" + File.separator
                    + "java" + File.separator
                    + "org" + File.separator
                    + "example" + File.separator
                    + fileName;
            StringBuilder contentBuilder = getStringBuilder(filePath);
            return contentBuilder.toString();
        }

        private static StringBuilder getStringBuilder(String filePath) throws IOException {
            StringBuilder contentBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (!firstLine) {
                        contentBuilder.append("\n");
                    }
                    contentBuilder.append(line);
                    firstLine = false;
                }
            }
            return contentBuilder;
        }

        private static List<String> splitIntoChunks(String content, int maxChunkSize) {
            List<String> chunks = new ArrayList<>();
            int length = content.length();
            for (int i = 0; i < length; i += maxChunkSize) {
                // Calculate end index for the current chunk
                int endIndex = Math.min(i + maxChunkSize, length);
                // Extract the chunk
                String chunk = content.substring(i, endIndex);
                chunks.add(chunk);
            }
            return chunks;
        }

        private void receiveFile(InetAddress hostAddress, int tcpPort) {
            // TODO: (Optional) Potentially verify that the RQ# and fileName received match requested
            try (Socket clientSocket = new Socket(hostAddress, tcpPort)) {
                InputStream inputStream = clientSocket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                FileReceiver fileReceiver;
                Message receivedMessage;

                boolean fileConstructed = false;

                // Receive first message to get file name
                receivedMessage = (Message) objectInputStream.readObject();
                if (receivedMessage instanceof FileMessage fileMessage) {
                    fileReceiver = new FileReceiver(fileMessage.getFileName());
                    fileConstructed = fileReceiver.addChunk(fileMessage.getChunkNo(), fileMessage.getText());
                } else if (receivedMessage instanceof FileEndMessage fileEndMessage) {
                    fileReceiver = new FileReceiver(fileEndMessage.getFileName());
                    fileReceiver.setNumExpectedChunks(fileEndMessage.getChunkNo() + 1);
                    fileConstructed = fileReceiver.addChunk(fileEndMessage.getChunkNo(), fileEndMessage.getText());
                } else {
                    fileReceiver = null;
                }

                assert fileReceiver != null;

                // Receive the rest of the packets
                while (!fileConstructed) {
                    receivedMessage = (Message) objectInputStream.readObject();
                    if (receivedMessage instanceof FileMessage fileMessage) {
//                        System.out.println("Received fileMessage Object with text: " + fileMessage.getText());
                        fileConstructed = fileReceiver.addChunk(fileMessage.getChunkNo(), fileMessage.getText());
                    } else if (receivedMessage instanceof FileEndMessage fileEndMessage) {
//                        System.out.println("Received fileEndMessage Object with text: " + fileEndMessage.getText());
                        fileReceiver.setNumExpectedChunks(fileEndMessage.getChunkNo() + 1);
                        fileConstructed = fileReceiver.addChunk(fileEndMessage.getChunkNo(), fileEndMessage.getText());
                    } else {
                        System.out.println("Received an unknown packet, aborting");
                        return;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleFileTransfer(FileReqMessage fileReqMessage, DatagramPacket receivedPacket) {
            try (ServerSocket tcpServerSocket = new ServerSocket(0)) {
                int tcpPort = tcpServerSocket.getLocalPort();

                InetAddress destinationAddress = receivedPacket.getAddress();
                int destinationPort = receivedPacket.getPort();

                FileConfMessage fileConfMessage = new FileConfMessage(fileReqMessage.getReqNo(), tcpPort);
                byte[] sendData = fileConfMessage.serialize();

                DatagramPacket sendPacket;

                if (sendData != null) {
                    sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress, destinationPort);
                    try {
                        socket.send(sendPacket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(Code.FILE_CONF + " message sent to server by client " + Thread.currentThread().getName());

                    Socket peerSocket = tcpServerSocket.accept();
                    OutputStream outputStream = peerSocket.getOutputStream();

                    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                        // Split the requested file into chunks
                        String fileText = readFileToString(fileReqMessage.getFileName());
                        List<String> chunks = splitIntoChunks(fileText, 200);

                        int index = 0;

                        for (String chunk : chunks) {
                            if (index == chunks.size() - 1) {
                                // Last chunk
                                FileEndMessage fileEndMessage = new FileEndMessage(fileReqMessage.getReqNo(),
                                        fileReqMessage.getFileName(), index, chunk);
                                objectOutputStream.writeObject(fileEndMessage);
                            } else {
                                FileMessage fileMessage = new FileMessage(fileReqMessage.getReqNo(),
                                        fileReqMessage.getFileName(), index, chunk);
                                objectOutputStream.writeObject(fileMessage);
                            }
                            ++index;
                        }
                        System.out.println("NumChunksSent: " + index);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static void handleUpdateConfirmation(String action) {
        // Update the client's internal state with the latest information
        // about registered clients and their available files
        // For example:
        // updateInternalState(clientInfoSet);

        // Print the confirmation message
        System.out.println("Your " + action + " action has been completed, and your information has been updated.");
    }

    // Update the client's internal state with the latest information
    private static synchronized void updateInternalState(HashMap<String, ClientInfo> clientInfoMap) {
        // Clear existing client files
        // clientFiles.clear();

        // TODO: (Note) If there are memory issues this might be the cause
        clientInfoHashMap = clientInfoMap;

        // Iterate through the client info map and update the client files
//        for (Map.Entry<String, ClientInfo> entry : clientInfoMap.entrySet()) {
//            String clientName = entry.getKey();
//            ClientInfo clientInfo = entry.getValue();
//            Set<String> files = clientInfo.getFiles();
//
//            // Update client files
//            clientFiles.put(clientName, new HashSet<>(files));
//        }
    }


    private synchronized static void updateClientFiles() {
        clientFiles.clear();

        for (Map.Entry<String, ClientInfo> entry : clientInfoHashMap.entrySet()) {
            String clientName = entry.getKey();
            ClientInfo clientInfo = entry.getValue();

            for (String fileName : clientInfo.getFiles()) {
                if (!clientFiles.containsKey(fileName)) {
                    clientFiles.put(fileName, new HashSet<>());
                }

                HashSet<String> hostingClients = clientFiles.get(fileName);

                hostingClients.add(clientName);
            }
        }

        // Print updated client files (for demonstration)
        System.out.println("Updated client files:");
        for (Map.Entry<String, HashSet<String>> entry : clientFiles.entrySet()) {
            String fileName = entry.getKey();
            HashSet<String> clients = entry.getValue();
            System.out.println("File: " + fileName + ", Clients: " + clients);
        }
    }

    public static void main(String[] args) {
        // TODO: Change once file transfer testing complete
        try (DatagramSocket socket = (args.length > 0 ? new DatagramSocket(4000) : new DatagramSocket())) {
            System.out.println("Port: " + socket.getLocalPort());
            new Thread(new IncomingMessageHandler(socket)).start();
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter your Client name: ");
            name = scanner.nextLine();

            int reqNo = 1;

            // TODO: chatGPT generated. Test each that they work
            while (true) {
                System.out.println("Enter message type (1 = REGISTER, 2 = DE_REGISTER, 3 = PUBLISH, 4 = FILE_REQ, 5 = REMOVE, 10 = Multiple registers, 0 = EXIT): ");
                int messageType = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character

                Message message = null;
                switch (messageType) {
                    case 1:
                        message = new RegisterMessage(reqNo++, name, InetAddress.getByName(SERVER_IP), socket.getLocalPort());
                        break;
                    case 2:
                        message = new DeRegisterMessage(reqNo++, name);
                        break;
                    case 3:
                        System.out.println("Enter filenames to publish (comma-separated): ");
                        List<String> filesToPublish = Arrays.asList(scanner.nextLine().split(","));
                        message = new PublishMessage(reqNo++, name, filesToPublish);
                        break;
                    case 4:
                        System.out.println("Enter filename for FILE_REQ: ");
                        String fileName = scanner.nextLine();
                        message = new FileReqMessage(reqNo++, fileName);
                        break;
                    case 5:
                        System.out.println("Enter filenames to remove (comma-separated): ");
                        List<String> filesToRemove = Arrays.asList(scanner.nextLine().split(","));
                        message = new RemoveMessage(reqNo++, name, filesToRemove);
                        break;
                    case 10:
                        // Handle multiple registrations
                        for (int i = 1; i <= 5; i++) {
                            String tmpClientName = name + i;
                            Message tmpMessage = new RegisterMessage(reqNo++, tmpClientName, InetAddress.getByName(SERVER_IP), socket.getLocalPort());
                            new Thread(new ClientTask(socket, tmpMessage)).start();
                        }
                        continue; // Continue to the next iteration of the loop
                    case 0:

                        // TODO: Make sure resources are closed  (William: Worked on adding this just need to validate)
                        socket.close();
                        scanner.close();
                        System.exit(0); // Exit the program
                    default:
                        System.out.println("Invalid message type.");
                }

                if (message != null) {
                    new Thread(new ClientTask(socket, message)).start();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

// TODO: (Optional) Might want to do reqNo validation if we have time