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
    private static final HashMap<String, ClientInfo> clientInfoSet = new HashMap<>();

    private static final HashSet<String> hostedFiles = new HashSet<>();

    // Create a thread pool of 10 to handle up to 10 transfers at once
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    private static class ClientTask implements Runnable {
        private final String clientName;
        private final int reqNo; // Request number for this instance of ClientTask
        private final Code code;
        private List<String> filesToPublish; // Only used for PUBLISH
        private String fileName;  // For FILE_REQ
        private final DatagramSocket socket;
        private int tcpSocket;  // For FILE_CONF


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

        // Constructor for FILE-REQ
        public ClientTask(DatagramSocket socket, String clientName, int reqNo, Code code, String fileName) {
            this.socket = socket;
            this.clientName = clientName;
            this.reqNo = reqNo;
            this.code = code;
            this.fileName = fileName;
        }

        @Override
        public void run() {
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
                } else if (receivedMessage instanceof UpdateMessage) {
                    UpdateMessage updateMessage = (UpdateMessage) receivedMessage;
                    HashSet<ClientInfo> clientInfoSet = updateMessage.getClientInfoSet();

                    // Update client's internal state with the latest information
                    // about registered clients and their available files
                    //problem area
                    // For example:
                    // updateInternalState(clientInfoSet);
                } else if (receivedMessage instanceof FileReqMessage fileReqMessage) {
                    System.out.println("Received from server by client "
                            + Thread.currentThread().getName()
                            + ": Code: " + fileReqMessage.getCode()
                            + ", REQ#: " + fileReqMessage.getReqNo()
                            + ", File Name: " + fileReqMessage.getFileName());

                    executor.submit(() -> handleFileTransfer(fileReqMessage, receivePacket));
                } else if (receivedMessage instanceof FileConfMessage fileConfMessage) {
                    System.out.println("Received from server by client "
                            + Thread.currentThread().getName()
                            + ": Code: " + fileConfMessage.getCode()
                            + ", REQ#: " + fileConfMessage.getReqNo()
                            + ", TCP Port: " + fileConfMessage.getTcpPort());

                    executor.submit(() -> receiveFile(receivePacket.getAddress(), fileConfMessage.getTcpPort()));
                } else {
                    // Handle other responses or unknown message types
                    System.out.println("Received an unrecognized message from the server.");
                }
                // Handle other message types as needed
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

    // Update the client's internal state with the latest information
    private static void updateInternalState(Map<String, ClientInfo> clientInfoMap) {
        // Clear existing client files
        clientFiles.clear();

        // Iterate through the client info map and update the client files
        for (Map.Entry<String, ClientInfo> entry : clientInfoMap.entrySet()) {
            String clientName = entry.getKey();
            ClientInfo clientInfo = entry.getValue();
            Set<String> files = clientInfo.getFiles();

            // Update client files
            clientFiles.put(clientName, new HashSet<>(files));
        }

        // Print updated client files (for demonstration)
        System.out.println("Updated client files:");

        //TODO: Fixed an error here, validate but allows me to run.
        for (Map.Entry<String, HashSet<String>> entry : clientFiles.entrySet()) {
            String clientName = entry.getKey();
            Set<String> files = entry.getValue();
            System.out.println("Client: " + clientName + ", Files: " + files);
        }
    }

    public static void main(String[] args) {
        // TODO: Change once file transfer testing complete
        try (DatagramSocket socket = (args.length > 0 ? new DatagramSocket(4000) : new DatagramSocket())) {
            System.out.println("Port: " + socket.getLocalPort());
            // Start the incoming request handler in a separate thread
            new Thread(new IncomingMessageHandler(socket)).start();

            // Prompt user for the client name
            System.out.println("Enter your Client name: ");
            Scanner scanner = new Scanner(System.in);

            name = scanner.nextLine();

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

                    //William Added for remove
                    case 5: // Assuming 5 is the option number for removing files
                        scanner.nextLine(); // Consume the newline character
                        System.out.println("Enter filenames to remove (comma-separated): ");
                        String filesInput = scanner.nextLine();
                        List<String> filesToRemove = Arrays.asList(filesInput.split(","));
                        new Thread(new ClientTask(socket, name, reqNo++, Code.REMOVE, filesToRemove)).start();
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

// TODO: (Optional) Might want to do reqNo validation if we have time