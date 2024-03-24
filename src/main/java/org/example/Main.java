package org.example;

public class Main {
    public static void main(String[] args) {
        // Check server handles multiple clients
        for (int i = 0; i < 3; i++){
            Thread thread = new Thread(new Client());
            thread.start();
        }
    }
}