package server;

import client.Request;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Map<String, String> database = new HashMap<>();

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23456;

        while (true) {
            try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
                System.out.println("Server started!");
                try (Socket socket = server.accept();
                     DataInputStream input = new DataInputStream(socket.getInputStream());
                     DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

                    Request request = new Gson().fromJson(input.readUTF(), Request.class);

                    if (request.getType().equals("exit")) {
                        output.writeUTF(new Gson().toJson(new Response("OK")));
                        System.out.println("Server has been stopped, because of 'exit' argument (request).");
                        break;
                    }

                    Response response = processRequest(request);
                    String responseJson = new Gson().toJson(response);
                    output.writeUTF(responseJson);
                    System.out.println("Sent: " + responseJson);

                } catch (IOException e) {
                    System.out.println("Server exception: " + e);
                }
            } catch (IOException e) {
                System.out.println("Server exception: " + e);
            }
        }
    }

    public static Response processRequest(Request request) {

        if (request.getType().isEmpty()) {
            return new Response("ERROR", "No such type");
        }

        switch (request.getType()) {
            case "set" -> {
                if (request.getKey().isEmpty() || request.getValue().isEmpty()) {
                    return new Response("ERROR", "Key or Value or both is empty");
                }
                database.put(request.getKey(), request.getValue());
                return new Response("OK");
            }
            case "get" -> {
                if (database.get(request.getKey()) != null) {
                    return new Response("OK", database.get(request.getKey()), null);
                } else {
                    return new Response("ERROR", "No such key");
                }
            }
            case "delete" -> {
                if (database.get(request.getKey()) != null) {
                    database.remove(request.getKey());
                    return new Response("OK");
                }
                return new Response("ERROR", "No such key");
            }
            default -> {
                return new Response("ERROR", "Unknown command");
            }
        }
    }

}
