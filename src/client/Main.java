package client;

import com.beust.jcommander.JCommander;
import com.google.gson.Gson;

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
        Request request = new Request();
        JCommander.newBuilder()
                .addObject(request)
                .build()
                .parse(args);

        String address = "127.0.0.1";
        int port = 23456;

        String requestJson;

        try (Socket socket = new Socket(InetAddress.getByName(address), port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("Client started!");

            if (request.getFileName() != null) {
                try {
                    String stringPath = "src/client/data/" + request.getFileName();
                    Path path = Paths.get(stringPath);
                    requestJson = new String(Files.readAllBytes(path));
                } catch (Exception e) {
                    System.out.println("Cannot read file: " + e);
                    return;
                }
            } else {
                requestJson = new Gson().toJson(request);
            }

            output.writeUTF(requestJson);
            System.out.println("Sent: " + requestJson);
            String receivedMsg = input.readUTF();
            System.out.println("Received: " + receivedMsg);
        } catch (IOException e) {
            System.out.println("Client exception: " + e);
        }

    }
}
