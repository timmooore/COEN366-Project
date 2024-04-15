package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private final DatagramSocket socket;
    private boolean running;
    private static final int BUFFER_SIZE = 1024;
    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Set<String> clientNames = new HashSet<>();
    private final HashMap<String, Set<String>> clientFiles = new HashMap<>(); // Store client files

    public Server(int port) {
        try {
            socket = new DatagramSocket(port);
            logger.info("Server started on port: " + port);
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "SocketException: ", e);
            throw new RuntimeException(e);
        }
    }

    public void listen() {
        running = true;

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
                    if (clientNames.contains(rm.getName())) {
                        // Deny registration
                        PublishDeniedMessage response = new PublishDeniedMessage(rm.getReqNo(), "Name exists");
                        sendResponse(packet, response);
                    } else {
                        clientNames.add(rm.getName());
                        PublishedMessage response = new PublishedMessage(rm.getReqNo());
                        sendResponse(packet, response);
                    }
                    break;
                }
                case DE_REGISTER: {
                    DeRegisterMessage drm = (DeRegisterMessage) receivedMessage;
                    logger.info("Received: Code: " + receivedMessage.getCode() +
                            ", RQ#: " + drm.getReqNo() +
                            ", Name: " + drm.getName());
                    clientNames.remove(drm.getName());
                    StringBuilder msg = new StringBuilder(drm.getName() + " removed. Remaining clients: ");
                    for (String name : clientNames) msg.append(name).append(" ");
                    logger.info(msg.toString());
                    break;
                }
                case PUBLISH:
                    handlePublish((PublishMessage) receivedMessage, packet);
                    break;
                // Add cases for other message types as needed

                //Added Remove
                case REMOVE:
                    handleRemove((RemoveMessage) receivedMessage, packet);
                    break;



            }
        }
    }
    
    private void handlePublish(PublishMessage pm, DatagramPacket packet) {
        if (clientNames.contains(pm.getName())) {
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
        if (clientNames.contains(rm.getName())) {
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

    public static void main(String[] args) {
        int port = 3000; // Example port number
        Server server = new Server(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.listen();
    }
}
