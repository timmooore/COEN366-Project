package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private final DatagramSocket socket;
    private boolean running;
    private static final int BUFFER_SIZE = 1024;
    private final Logger logger = Logger.getLogger(Server.class.getName());

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
        // String received = new String(packet.getData(), 0, packet.getLength());
        // logger.info("Received: " + received);

        byte[] dataReceived = packet.getData();
        Message receivedMessage = Message.deserialize(dataReceived);
        assert receivedMessage != null;  // TODO: Might want this to be an if, needs testing
        if (receivedMessage.getCode() == Code.REGISTER) {
            RegisterMessage rm = (RegisterMessage) receivedMessage;
            logger.info("Received: Code: " + receivedMessage.getCode() +
                    ", RQ#: " + rm.getReqNo() +
                    ", Name: " + rm.getName() +
                    ", IP address: " + rm.getIpAddress().toString() +
                    ", UDP port: " + rm.getUdpPort());

            // TODO: Implement protocol logic here (e.g., REGISTER, PUBLISH, etc.)
            Message response;
            response = new RegisteredMessage(rm.getReqNo());
            byte[] responseData = response.serialize();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
            try {
                socket.send(responsePacket);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to send response: ", e);
            }
        }
        // Example response
//        String responseStr = "ACK";
//        byte[] responseData = responseStr.getBytes();
//        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());



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
