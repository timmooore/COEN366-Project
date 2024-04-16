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
    private static final String SERVER_IP = "192.168.2.12";
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
        this.socket = new DatagramSocket(port, InetAddress.getByName(SERVER_IP));
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
            }
        }
    }

    private void handlePublish(PublishMessage pm, DatagramPacket packet) {
        if (registeredClients.containsKey(pm.getName())) {
            ClientInfo clientInfo = registeredClients.get(pm.getName());

            clientInfo.getFiles().addAll(pm.getFiles());

//            // Client is registered, update file list
//            clientFiles.putIfAbsent(pm.getName(), new HashSet<>());
//            clientFiles.get(pm.getName()).addAll(pm.getFiles());

            // Send PUBLISHED message
            PublishedMessage response = new PublishedMessage(pm.getReqNo());
            sendResponse(packet, response);

            // Trigger update
            executor.submit(new UpdateTask());
        } else {
            // Client not registered, send PUBLISH-DENIED
            PublishDeniedMessage response = new PublishDeniedMessage(pm.getReqNo(), "Client not registered");
            sendResponse(packet, response);
        }
    }

    private void handleRemove(RemoveMessage rm, DatagramPacket packet) {
        if (registeredClients.containsKey(rm.getName())) {
            rm.getFiles().forEach(registeredClients.get(rm.getName()).getFiles()::remove);

//            // Client is registered, remove files from the list
//            Set<String> files = clientFiles.getOrDefault(rm.getName(), new HashSet<>());
//
//            // files.removeAll(rm.getFiles());
//            rm.getFiles().forEach(files::remove);  // IntelliJ suggested change
//            clientFiles.put(rm.getName(), files);

            // Send REMOVED message
            RemovedMessage response = new RemovedMessage(rm.getReqNo());
            sendResponse(packet, response);

            // Trigger update
            executor.submit(new UpdateTask());
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
