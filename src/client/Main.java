package client;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Request requestArgs = new Request();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(requestArgs)
                .build();
        jCommander.parse(args);

        String address = "127.0.0.1";
        int port = 23456;
        Gson gson = new Gson();
        String requestJson;

        try (Socket socket = new Socket(InetAddress.getByName(address), port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("Client started!");

            if (requestArgs.getFileName() != null) {
                try {
                    String stringPath = "src/client/data/" + requestArgs.getFileName();
                    Path path = Paths.get(stringPath);
                    requestJson = new String(Files.readAllBytes(path));
                } catch (IOException e) {
                    System.out.println("Cannot read file: " + e.getMessage());
                    return;
                }
            } else {
                JsonObject jsonRequest = new JsonObject();
                jsonRequest.addProperty("type", requestArgs.getType());

                if (requestArgs.getKey() != null) {
                    jsonRequest.addProperty("key", requestArgs.getKey());
                }
                if (requestArgs.getValue() != null) {
                    jsonRequest.addProperty("value", requestArgs.getValue());
                }
                requestJson = gson.toJson(jsonRequest);
            }

            output.writeUTF(requestJson);
            System.out.println("Sent: " + requestJson);
            String receivedMsg = input.readUTF();
            System.out.println("Received: " + receivedMsg);
        } catch (IOException e) {
            System.out.println("Client exception: " + e.getMessage());
        }
    }
}
