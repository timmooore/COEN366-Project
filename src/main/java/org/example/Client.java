package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;


public class Client {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 3000;
    private static final int BUFFER_SIZE = 1024;

    private static class ClientTask implements Runnable {
        private int reqNo; // Request number for this instance of ClientTask
        private final Code code;
        private List<String> filesToPublish; // Only used for PUBLISH

        // Overloaded constructor for PUBLISH with files list
         public ClientTask(int reqNo, Code code, List<String> filesToPublish) {
            this.reqNo = reqNo;
            this.code = code;
            this.filesToPublish = filesToPublish;
        }


        // TODO: Modify so that we can pass different messages to the CLientTask
        public ClientTask(int reqNo, Code code) {
            this.reqNo = reqNo;
            this.code = code;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                if (code == Code.REGISTER) {
                    ///////////////////////////////////////////////
                    // TODO: Testing. Remove
                    String name = "Client" + reqNo;
                    if (reqNo == 3) name = "Client" + 2;
                    ///////////////////////////////////////////////
                    // Create a register message for each client
                    RegisterMessage register = new RegisterMessage(reqNo++, name, serverAddress, SERVER_PORT);
                    byte[] sendData = register.serialize();

                    // Create packet to send to server
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);

                    // Send packet to server
                    socket.send(sendPacket);
                    System.out.println("Message sent to server by client " + Thread.currentThread().getName());

                    // Receive response from server
                    byte[] receiveData = new byte[BUFFER_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    // Wait for response packet from server
                    socket.receive(receivePacket);

                    // Deserialize the received data into a RegisteredMessage
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
                    }
                } else if (code == Code.DE_REGISTER) {
                    DeRegisterMessage deRegister = new DeRegisterMessage(reqNo, "Client1");
                    byte[] sendData = deRegister.serialize();

                    // Create packet to send to server
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);

                    // Send packet to server
                    socket.send(sendPacket);
                    System.out.println("Message sent to server by client " + Thread.currentThread().getName());
                }

//            // Process response
//            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
//            System.out.println("Received from server: " + receivedMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        int reqNo = 1;  // REQ# numbers the requests of each Client
        // Number of clients to run
        int numClients = 5;
        Code code = Code.REGISTER;
        for (int i = 0; i < numClients; i++) {
            new Thread(new ClientTask(reqNo, code)).start();
            ++reqNo;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        new Thread(new ClientTask(reqNo, Code.DE_REGISTER)).start();
        ++reqNo;

        // TODO: Implement some sort of input CLI to choose messages
    }
}
