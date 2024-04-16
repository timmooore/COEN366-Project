package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ClientInfoSerializer {
    private static final String FILE_PATH = "src" + File.separator
            + "main" + File.separator
            + "java" + File.separator
            + "org" + File.separator
            + "example" + File.separator;

    private static final String JSON_FILE_NAME = "registeredClients.json";

    public static void serializeClientInfoMap(HashMap<String, ClientInfo> clientInfoMap) {
        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Serialize HashMap to JSON and write to file
            objectMapper.writeValue(new File(FILE_PATH + JSON_FILE_NAME), clientInfoMap);
            System.out.println("ClientInfo map serialized successfully to " + JSON_FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error serializing ClientInfo map: " + e.getMessage());
        }
    }

    public static HashMap<String, ClientInfo> loadClientInfoMap() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            File jsonFile = new File(FILE_PATH + JSON_FILE_NAME);
            if (jsonFile.exists()) {
                // Read JSON file and deserialize into a Map<String, ClientInfo>
                HashMap<String, ClientInfo> clientInfoMap = objectMapper.readValue(
                        new File(FILE_PATH + JSON_FILE_NAME),
                        new TypeReference<HashMap<String, ClientInfo>>() {
                        }
                );
                return clientInfoMap;
            } else {
                return new HashMap<>();
            }
        } catch (IOException e) {
            // Handle IO exceptions (e.g., file not found, read error)
            e.printStackTrace(); // Handle more gracefully in production
            return new HashMap<>(); // Return empty map if loading fails
        }

    }
}

