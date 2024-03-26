package org.example;

import java.io.*;

public class Message implements Serializable {
    private final Code code;

    public Message(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }



    public byte[] serialize() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            // Write the object to the byte stream
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Get the byte array from the byte stream
        return byteStream.toByteArray();
    }

    // Deserialize bytes back to object
    public static Message deserialize(byte[] bytes) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Message) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Handle deserialization error
            throw new RuntimeException(e);
        }
    }
}

