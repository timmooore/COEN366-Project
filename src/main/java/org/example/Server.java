package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

public class Server {
    private static final int BUFFER_SIZE = 1024;
    private final int port;
    private final DatagramSocket socket;
    private boolean running;
    private final Logger logger = Logger.getLogger(Server.class.getName());

    // TODO: Restore from .json
    private static final HashMap<String, ClientInfo> registeredClients = new HashMap<>();
    //private final Map<String, InetAddress> registeredClients;
    private final Map<String, Set<String>> clientFiles;

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
/*
    private final DatagramSocket socket;
    private boolean running;
    private static final int BUFFER_SIZE = 1024;
    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Set<String> clientNames = new HashSet<>();
    private final HashMap<String, Set<String>> clientFiles = new HashMap<>(); // Store client files
*/

    public Server(int port) throws IOException {
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.clientFiles = new HashMap<>();

        logger.info("Server started on port: " + port);
    }

    public void start() {
        running = true;
        // Start a timer to send UPDATE message every 5 minutes
//        Timer timer = new Timer();
//        timer.schedule(new UpdateTask(), 0, 5 * 60 * 1000);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateTask(), 0, 5 * 60 * 1000);

        while (running) {
            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);

                new Thread(() -> handlePacket(packet)).start();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "IOException: ", e);
                if (!socket.isClosed()) {
                    stop();
                }
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        byte[] dataReceived = packet.getData();
        Message receivedMessage = Message.deserialize(dataReceived);
    
        if (receivedMessage != null) {
            switch (receivedMessage.getCode()) {
                case REGISTER: {
                    RegisterMessage rm = (RegisterMessage) receivedMessage;
                    logger.info("Received: Code: " + receivedMessage.getCode() +
                            ", RQ#: " + rm.getReqNo() +
                            ", Name: " + rm.getName() +
                            ", IP address: " + rm.getIpAddress().toString() +
                            ", UDP port: " + rm.getUdpPort());
                    if (registeredClients.containsKey(rm.getName())) {
                        // Deny registration
                        RegisterDeniedMessage response = new RegisterDeniedMessage(rm.getReqNo(), "Name exists");
                        sendResponse(packet, response);

                        logger.info("Sent: Code: " + response.getCode() +
                                ", RQ#: " + response.getReqNo() +
                                " to client: " + rm.getName());
                    } else {
                        ClientInfo clientInfo = new ClientInfo(rm.getName(), rm.getIpAddress(), rm.getUdpPort());
                        registeredClients.put(rm.getName(), clientInfo); // removed clientnames and replaced, also added get ipaddress
                        RegisteredMessage response = new RegisteredMessage(rm.getReqNo());

                        // Submit a new UpdateTask
                        executor.submit(new UpdateTask());
                        sendResponse(packet, response);
                        logger.info("Sent: Code: " + response.getCode() +
                                ", RQ#: " + response.getReqNo() +
                                " to client: " + rm.getName());
                    }
                    break;
                }
                case DE_REGISTER: {
                    DeRegisterMessage drm = (DeRegisterMessage) receivedMessage;
                    logger.info("Received: Code: " + receivedMessage.getCode() +
                            ", RQ#: " + drm.getReqNo() +
                            ", Name: " + drm.getName());

                    // TODO: Send update to clients
                    registeredClients.remove(drm.getName());
                    StringBuilder msg = new StringBuilder(drm.getName() + " removed. Remaining clients: ");
                    for (String name : registeredClients.keySet()) msg.append(name).append(" ");
                    logger.info(msg.toString());

                    // Submit a new UpdateTask to update clients
                    executor.submit(new UpdateTask());
                    break;
                }
                case PUBLISH:
                    handlePublish((PublishMessage) receivedMessage, packet);
                    // TODO: Update clients here on in handlePublish
                    break;
                // Add cases for other message types as needed

                //Added Remove
                case REMOVE:
                    handleRemove((RemoveMessage) receivedMessage, packet);
                    // TODO: Update clients here on in handlePublish
                    break;
                case CONTACT_CONFIRMED: {
                    handleUpdateConfirmed((UpdateConfirmedMessage) receivedMessage);
                    // Handle UPDATE_CONFIRMED message
                    break;
                }
                case CONTACT_DENIED: {
                    handleUpdateDenied((UpdateDeniedMessage) receivedMessage);
                    // Handle UPDATE_DENIED message
                    break;
                }
                default: {
                    logger.warning("Received an unrecognized message with code: " + receivedMessage.getCode());
                    break;
                }
            }
        }
    }private void handleUpdateConfirmed(UpdateConfirmedMessage confirmedMessage) {
        logger.info("UPDATE confirmed for client " + confirmedMessage.getName() +
                ": REQ#: " + confirmedMessage.getReqNo());
    }

    // Handle UPDATE_DENIED message
    private void handleUpdateDenied(UpdateDeniedMessage deniedMessage) {
        logger.warning("UPDATE denied for client " + deniedMessage.getName() +
                ": REQ#: " + deniedMessage.getReqNo() +
                ", Reason: " + deniedMessage.getReason());
    }

    
    private void handlePublish(PublishMessage pm, DatagramPacket packet) {
        if (registeredClients.containsKey(pm.getName())) {
            // Client is registered, update file list

            // TODO: Check this logic for new
            clientFiles.putIfAbsent(pm.getName(), new HashSet<>());
            clientFiles.get(pm.getName()).addAll(pm.getFiles());
    
            // Send PUBLISHED message
            PublishedMessage response = new PublishedMessage(pm.getReqNo());
            sendResponse(packet, response);
        } else {
            // Client not registered, send PUBLISH-DENIED
            PublishDeniedMessage response = new PublishDeniedMessage(pm.getReqNo(), "Client not registered");
            sendResponse(packet, response);
        }
    }

    private void handleRemove(RemoveMessage rm, DatagramPacket packet) {
        if (registeredClients.containsKey(rm.getName())) {
            // Client is registered, remove files from the list
            Set<String> files = clientFiles.getOrDefault(rm.getName(), new HashSet<>());
            // files.removeAll(rm.getFiles());
            rm.getFiles().forEach(files::remove);  // IntelliJ suggested change
            clientFiles.put(rm.getName(), files);

            // Send REMOVED message
            RemovedMessage response = new RemovedMessage(rm.getReqNo());
            sendResponse(packet, response);
        } else {
            // Client not registered, handle error
            RemoveDeniedMessage response = new RemoveDeniedMessage(rm.getReqNo(), "Client not registered");
            sendResponse(packet, response);
        }
    }

    private void sendResponse(DatagramPacket packet, Message response) {
        byte[] responseData = response.serialize();
        DatagramPacket responsePacket = new DatagramPacket(responseData,
                responseData.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send response: ", e);
        }
    }


    public void stop() {
        running = false;
        socket.close();
        logger.info("Server stopped.");
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            // Construct and send UPDATE message
            UpdateMessage updateMessage = constructUpdateMessage();
            sendUpdateToAllClients(updateMessage);
        }

        private UpdateMessage constructUpdateMessage() {
            // Construct the update message containing registered clients and their files
            HashSet<ClientInfo> clientInfos = new HashSet<>(registeredClients.values());

            StringBuilder sb = new StringBuilder();

            sb.append("Full list of client info:\n");

            for (ClientInfo clientInfo : clientInfos) {

                sb.append("clientName: ").append(clientInfo.getName())
                        .append(", IP address: ").append(clientInfo.getIpAddress().toString())
                        .append(", UDP port: ").append(clientInfo.getUdpPort())
                        .append(", files: ");

                for (String fileName : clientInfo.getFiles()) {
                    sb.append(fileName).append(", ");
                }
                sb.append("\n");
            }

            String clientInfoString = sb.toString();
            logger.info(clientInfoString);

            return new UpdateMessage(clientInfos);
        }

        private void sendUpdateToAllClients(UpdateMessage updateMessage) {
            byte[] updateData = updateMessage.serialize();

            for (Map.Entry<String, ClientInfo> entry : registeredClients.entrySet()) {

                ClientInfo clientInfo = entry.getValue();

                String clientName = clientInfo.getName();
                InetAddress clientAddress = clientInfo.getIpAddress();
                int clientPort = clientInfo.getUdpPort(); // Dynamically retrieve the port for each client

                // Construct DatagramPacket with update data, client address, and port
                DatagramPacket updatePacket = new DatagramPacket(updateData, updateData.length, clientAddress, clientPort);

                try {
                    // Send the update packet
                    socket.send(updatePacket);
                    logger.info("Sent UPDATE message to client: " + clientName);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to send UPDATE to client: " + clientName, e);
                }
            }
        }
    }
//
//    private static class IncomingMessageHandler implements Runnable {
//        private final DatagramSocket socket;
//
//        public IncomingMessageHandler(DatagramSocket socket) {
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//            while (true) {
//                // Receive incoming message
//                byte[] receiveData = new byte[BUFFER_SIZE];
//                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//                try {
//                    socket.receive(receivePacket);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                // Deserialize received message
//                Message receivedMessage = Message.deserialize(receivePacket.getData());
//
//                // Handle different message types
//                if (receivedMessage.getCode() == Code.CONTACT_UPDATE) {
//                    handleContactUpdate((UpdateContactMessage) receivedMessage);
//                } else if (receivedMessage.getCode() == Code.CONTACT_CONFIRMED) {
//                    handleContactConfirmed((UpdateConfirmedMessage) receivedMessage);
//                } else if (receivedMessage.getCode() == Code.CONTACT_DENIED) {
//                    handleContactDenied((UpdateDeniedMessage) receivedMessage);
//                } else {
//                    System.out.println("Received an unrecognized message from the client.");
//                }
//            }
//        }
//
//        // Method to handle contact update request from client
//        private void handleContactUpdate(UpdateContactMessage updateMessage) {
//            String clientName = updateMessage.getName();
//            if (!clientInfoMap.containsKey(clientName)) {
//                // Name does not exist, send denial message
//                String reason = "Name does not exist";
//                UpdateDeniedMessage denialMessage = new UpdateDeniedMessage(updateMessage.getReqNo(), clientName, reason);
//                sendResponse(denialMessage, updateMessage.getIpAddress(), updateMessage.getUdpSocket());
//            } else {
//                // Name exists, process the update
//                // Send confirmation message
//                UpdateConfirmedMessage confirmationMessage = new UpdateConfirmedMessage(updateMessage.getReqNo(), clientName);
//                sendResponse(confirmationMessage, updateMessage.getIpAddress(), updateMessage.getUdpSocket());
//            }
//        }
//
//        // Method to handle contact confirmed response from client
//        private void handleContactConfirmed(UpdateConfirmedMessage confirmedMessage) {
//            System.out.println("UPDATE confirmed for client " + confirmedMessage.getName() +
//                    ": REQ#: " + confirmedMessage.getReqNo());
//        }
//
//        // Method to handle contact denied response from client
//        private void handleContactDenied(UpdateDeniedMessage deniedMessage) {
//            System.out.println("UPDATE denied for client " + deniedMessage.getName() +
//                    ": REQ#: " + deniedMessage.getReqNo() +
//                    ", Reason: " + deniedMessage.getReason());
//        }
//
//        // Method to send response message to the client
//        private void sendResponse(Message responseMessage, InetAddress ipAddress, int udpSocket) {
//            byte[] sendData = responseMessage.serialize();
//            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, udpSocket);
//            try {
//                socket.send(sendPacket);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    // Main method to start the server
//    public static void main(String[] args) {
//        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
//            // Start the incoming message handler in a separate thread
//            new Thread(new IncomingMessageHandler(socket)).start();
//            System.out.println("Server started...");
//
//            // Keep the server running
//            while (true) {
//                // Server will keep listening for incoming messages
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}


    public static void main(String[] args) {
        int port = 3000; // Example port number
        try {
            Server server = new Server(port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.start();
        } catch (IOException e) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Failed to start server", e);
        }
    }
}
