package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileReceiver {
    private final Map<Integer, String> receivedChunks = new HashMap<>();
    private final String fileName;
    private int numChunksReceived;
    private Integer numExpectedChunks;

    public FileReceiver(String fileName) {
        // TODO: (Optional) Revert appendCopy
        this.fileName = appendCopy(fileName);
        this.numChunksReceived = 0;
    }

    // Method to add a received chunk with a sequence number
    public synchronized boolean addChunk(int chunkNumber, String chunk) {
        // Map chunk to its chunk no
        receivedChunks.put(chunkNumber, chunk);

        int lNumChunksReceived = incrementNumChunksReceived();

        // Might not have received the FILE-END yet
        if (numExpectedChunks != null) {
            // Construct the file when all chunks received
            if (lNumChunksReceived == numExpectedChunks) {
                reconstructAndWriteToFile(fileName);
                return true;
            }
        }
        return false;
    }

    private synchronized int incrementNumChunksReceived() { return ++numChunksReceived; }
    // Method to set the total number of expected chunks
    public synchronized void setNumExpectedChunks(int numExpectedChunks) {
        this.numExpectedChunks = numExpectedChunks;
    }

    // Method to reconstruct the text file from chunks and write to file
    private synchronized void reconstructAndWriteToFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numExpectedChunks; i++) {
            sb.append(receivedChunks.get(i));
        }
        String reconstructedFileContent = sb.toString();

        String filePath = "src" + File.separator
                + "main" + File.separator
                + "java" + File.separator
                + "org" + File.separator
                + "example" + File.separator
                + fileName;

        // Write the reconstructed file content to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(reconstructedFileContent);
            System.out.println("Reconstructed file saved successfully to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing reconstructed file: " + e.getMessage());
        }
    }

    public static String appendCopy(String fileName) {
        // Find the last occurrence of '.' to locate the file extension
        int extensionIndex = fileName.lastIndexOf('.');

        if (extensionIndex == -1) {
            // If no extension found, return the fileName as it is
            return fileName;
        }

        // Extract the file name without extension
        String baseName = fileName.substring(0, extensionIndex);
        // Extract the file extension
        String fileExtension = fileName.substring(extensionIndex);

        // Construct the modified file name with "_copy" + original file extension
        return baseName + "_copy" + fileExtension;
    }
}

