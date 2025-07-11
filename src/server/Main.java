package server;

import com.google.gson.*;
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

    private static final Map<String, JsonElement> database = new HashMap<>();
    private static final Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson gson = new Gson();
    private static final File file = new File("src/server/data/db.json");
    private static ServerSocket server = null;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23456;

        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

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
                    System.out.println("Server stopped accepting connections: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not start server: " + e.getMessage());
        }
    }

    public static void processRequest(Socket socket) {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String requestStr = input.readUTF();
            JsonObject requestJson = gson.fromJson(requestStr, JsonObject.class);

            String type = requestJson.get("type").getAsString();
            JsonElement keyElement = requestJson.has("key") ? requestJson.get("key") : null;
            JsonElement valueElement = requestJson.has("value") ? requestJson.get("value") : null;

            Response response;

            if (type.equals("exit")) {
                writeLock.lock();
                try {
                    server.close();
                    response = new Response("OK");
                    System.out.println("Server has been stopped, because of 'exit' argument (request).");
                } finally {
                    writeLock.unlock();
                }
            } else {
                response = handleRequest(type, keyElement, valueElement);
            }

            String responseJson = gson.toJson(response);
            output.writeUTF(responseJson);
            System.out.println("Sent: " + responseJson);

        } catch (IOException e) {
            System.out.println("Server exception during request processing: " + e.getMessage());
        }
    }

    public static Response handleRequest(String type, JsonElement keyElement, JsonElement valueElement) {
        if (type == null || type.isEmpty()) {
            return new Response("ERROR", "No such type");
        }

        switch (type) {
            case "set" -> {
                if (keyElement == null || valueElement == null) {
                    return new Response("ERROR", "Key or Value or both is missing");
                }
                writeLock.lock();
                try {
                    if (keyElement.isJsonPrimitive()) {
                        database.put(keyElement.getAsString(), valueElement);
                    } else if (keyElement.isJsonArray()) {
                        JsonArray keyPath = keyElement.getAsJsonArray();
                        if (keyPath.size() == 0) {
                            return new Response("ERROR", "Empty key path for set operation");
                        }
                        String rootKey = keyPath.get(0).getAsString();
                        if (!database.containsKey(rootKey) || !database.get(rootKey).isJsonObject()) {
                            database.put(rootKey, new JsonObject());
                        }
                        JsonObject currentObject = database.get(rootKey).getAsJsonObject();
                        traverseAndModifyJson(currentObject, keyPath, valueElement, 1, false);
                    } else {
                        return new Response("ERROR", "Invalid key format");
                    }
                    saveDBToFile();
                    return new Response("OK");
                } finally {
                    writeLock.unlock();
                }
            }
            case "get" -> {
                if (keyElement == null) {
                    return new Response("ERROR","Key is missing");
                }
                readLock.lock();
                try {
                    if (keyElement.isJsonPrimitive()) {
                        JsonElement value = database.get(keyElement.getAsString());
                        if (value != null) {
                            return new Response("OK", value, null);
                        } else {
                            return new Response("ERROR", "No such key");
                        }
                    } else if (keyElement.isJsonArray()) {
                        JsonArray keyPath = keyElement.getAsJsonArray();
                        if (keyPath.size() == 0) {
                            return new Response("ERROR", "Empty key path for get operation");
                        }
                        String rootKey = keyPath.get(0).getAsString();
                        JsonElement currentElement = database.get(rootKey);

                        if (currentElement == null) {
                            return new Response("ERROR", "No such key");
                        }

                        for (int i = 1; i < keyPath.size(); i++) {
                            if (currentElement == null || !currentElement.isJsonObject() || !currentElement.getAsJsonObject().has(keyPath.get(i).getAsString())) {
                                return new Response("ERROR", "No such key");
                            }
                            currentElement = currentElement.getAsJsonObject().get(keyPath.get(i).getAsString());
                        }
                        return new Response("OK", currentElement, null);
                    } else {
                        return new Response("ERROR", "Invalid key format");
                    }
                } finally {
                    readLock.unlock();
                }
            }
            case "delete" -> {
                if (keyElement == null) {
                    return new Response("ERROR", "Key is missing");
                }
                writeLock.lock();
                try {
                    if (keyElement.isJsonPrimitive()) {
                        if (database.containsKey(keyElement.getAsString())) {
                            database.remove(keyElement.getAsString());
                            saveDBToFile();
                            return new Response("OK");
                        }
                        return new Response("ERROR", "No such key");
                    } else if (keyElement.isJsonArray()) {
                        JsonArray keyPath = keyElement.getAsJsonArray();
                        if (keyPath.size() == 0) {
                            return new Response("ERROR", "Empty key path for delete operation");
                        }
                        String rootKey = keyPath.get(0).getAsString();
                        JsonElement currentElement = database.get(rootKey);

                        if (currentElement == null || !currentElement.isJsonObject()) {
                            return new Response("ERROR", "No such key");
                        }

                        if (keyPath.size() == 1) {
                            database.remove(rootKey);
                            saveDBToFile();
                            return new Response("OK");
                        } else {
                            JsonObject parentObject = currentElement.getAsJsonObject();
                            for (int i = 1; i < keyPath.size() - 1; i++) {
                                String currentPathSegment = keyPath.get(i).getAsString();
                                if (!parentObject.has(currentPathSegment) || !parentObject.get(currentPathSegment).isJsonObject()) {
                                    return new Response("ERROR", "No such key");
                                }
                                parentObject = parentObject.getAsJsonObject().get(currentPathSegment).getAsJsonObject();
                            }
                            String lastKey = keyPath.get(keyPath.size() - 1).getAsString();
                            if (parentObject.has(lastKey)) {
                                parentObject.remove(lastKey);
                                saveDBToFile();
                                return new Response("OK");
                            } else {
                                return new Response("ERROR", "No such key");
                            }
                        }
                    } else {
                        return new Response("ERROR", "Invalid key format");
                    }
                } finally {
                    writeLock.unlock();
                }
            }
            default -> {
                return new Response("ERROR", "Unknown command");
            }
        }
    }

    private static void traverseAndModifyJson(JsonObject current, JsonArray path, JsonElement valueToSet, int index, boolean isDelete) {
        if (index == path.size() - 1) {
            String targetKey = path.get(index).getAsString();
            if (isDelete) {
                current.remove(targetKey);
            } else {
                current.add(targetKey, valueToSet);
            }
            return;
        }

        String nextKey = path.get(index).getAsString();
        if (!current.has(nextKey) || !current.get(nextKey).isJsonObject()) {
            current.add(nextKey, new JsonObject());
        }
        traverseAndModifyJson(current.get(nextKey).getAsJsonObject(), path, valueToSet, index + 1, isDelete);
    }


    public static void saveDBToFile() {
        try (FileWriter writer = new FileWriter(file)) {
            gsonWriter.toJson(database, writer);
        } catch (IOException e) {
            System.out.println("Error saving DB to file: " + e.getMessage());
        }
    }

    public static void readFromFileAndSaveToDB() {
        writeLock.lock();
        try {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, JsonElement>>() {}.getType();
                Map<String, JsonElement> mapFromFile = gson.fromJson(reader, type);
                if (mapFromFile != null) {
                    database.putAll(mapFromFile);
                }
            } catch (FileNotFoundException e) {
                System.out.println("Database file not found, starting with empty database.");
            } catch (IOException e) {
                System.out.println("Error reading DB from file: " + e.getMessage());
            } catch (com.google.gson.JsonSyntaxException e) {
                System.out.println("Error parsing JSON from DB file: " + e.getMessage());
            }
        } finally {
            writeLock.unlock();
        }
    }
}
