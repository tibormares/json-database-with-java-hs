package server;

import client.Request;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    private static final Map<String, String> database = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final File file = new File("src/server/data/db.json");
    private static ServerSocket server = null;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23456;

        if (file.exists()) {
            readFromFileAndSaveToDB();
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            server = new ServerSocket(port, 50, InetAddress.getByName(address));
            System.out.println("Server started!");
            while (true) {
                try {
                    Socket socket = server.accept();
                    executor.submit(() -> processRequest(socket));
                } catch (IOException e) {
                    executor.shutdownNow();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not start server: " + e);
        }

    }

    public static void processRequest(Socket socket) {

        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            Request request = gson.fromJson(input.readUTF(), Request.class);

            if (request.getType().equals("exit")) {
                server.close();
                output.writeUTF(gson.toJson(new Response("OK")));
                System.out.println("Server has been stopped, because of 'exit' argument (request).");
                return;
            }

            Response response = processRequestToResponse(request);
            String responseJson = gson.toJson(response);
            output.writeUTF(responseJson);
            System.out.println("Sent: " + responseJson);
        } catch (IOException e) {
            System.out.println("Server exception: " + e);
        }

    }

    public static Response processRequestToResponse(Request request) {

        if (request.getType().isEmpty()) {
            return new Response("ERROR", "No such type");
        }

        switch (request.getType()) {
            case "set" -> {
                writeLock.lock();
                try {
                    if (request.getKey().isEmpty() || request.getValue().isEmpty()) {
                        return new Response("ERROR", "Key or Value or both is empty");
                    }
                    database.put(request.getKey(), request.getValue());
                    saveDBToFile();
                    return new Response("OK");
                } finally {
                    writeLock.unlock();
                }
            }
            case "get" -> {
                readLock.lock();
                try {
                    if (database.get(request.getKey()) != null) {
                        return new Response("OK", database.get(request.getKey()), null);
                    } else {
                        return new Response("ERROR", "No such key");
                    }
                } finally {
                    readLock.unlock();
                }
            }
            case "delete" -> {
                writeLock.lock();
                try {
                    if (database.get(request.getKey()) != null) {
                        database.remove(request.getKey());
                        saveDBToFile();
                        return new Response("OK");
                    }
                    return new Response("ERROR", "No such key");
                } finally {
                    writeLock.unlock();
                }
            }
            default -> {
                return new Response("ERROR", "Unknown command");
            }
        }
    }

    public static void saveDBToFile() {
        writeLock.lock();
        try {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(database, writer);
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public static void readFromFileAndSaveToDB() {
        writeLock.lock();
        try {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> mapFromFile = gson.fromJson(reader, type);
                if (mapFromFile != null) {
                    database.putAll(mapFromFile);
                }
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
        } finally {
            writeLock.unlock();
        }
    }

}
