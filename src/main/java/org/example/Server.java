package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private static final HashMap<String, ClientInfo> registeredClients = new HashMap<String, ClientInfo>();
    //private final Map<String, InetAddress> registeredClients;
    private final Map<String, Set<String>> clientFiles;

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

    public void start(int reqNo) {
        running = true;
        // Start a timer to send UPDATE message every 5 minutes
//        Timer timer = new Timer();
//        timer.schedule(new UpdateTask(), 0, 5 * 60 * 1000);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateTask(reqNo), 0, 5 * 60 * 1000);

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
                    } else {
                        // TODO: Send Update to clients
                        ClientInfo clientInfo = new ClientInfo(rm.getName(), rm.getIpAddress(), rm.getUdpPort());
                        registeredClients.put(rm.getName(), clientInfo); // removed clientnames and replaced, also added get ipaddress
                        RegisteredMessage response = new RegisteredMessage(rm.getReqNo());
                        sendResponse(packet, response);
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
            // Client is registered, update file list
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
            files.removeAll(rm.getFiles());
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
        private final int reqNo;

        public UpdateTask(int reqNo) {
            this.reqNo = reqNo;
        }

        @Override
        public void run() {
            // Construct and send UPDATE message
            UpdateMessage updateMessage = constructUpdateMessage();
            sendUpdateToAllClients(updateMessage);
        }

        private UpdateMessage constructUpdateMessage() {
            // Construct the update message containing registered clients and their files

            HashSet<ClientInfo> clientInfos = new HashSet<>(registeredClients.values());
            return new UpdateMessage(reqNo, clientInfos);
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

        private int getClientPort(String clientName) {
            // Implement logic to retrieve the port associated with the given client name
            // This could involve querying some data structure where client names and ports are stored
            // For example, you might have a Map<String, Integer> that maps client names to their associated ports
            // Return the port associated with the client name
            return 0;  // If the port is not found or cannot be determined, you may return a default port or handle the situation accordingly
        }
    }

    public static void main(String[] args) {
        int reqNo = 1;
        int port = 3000; // Example port number
        try {
            Server server = new Server(port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.start(reqNo);
        } catch (IOException e) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, "Failed to start server", e);
        }
    }
}
