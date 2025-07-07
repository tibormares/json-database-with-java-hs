package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static final String[] array = new String[1000];

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23456;

        while (true) {
            try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
                System.out.println("Server started!");
                try (Socket socket = server.accept();
                     DataInputStream input = new DataInputStream(socket.getInputStream());
                     DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                    String[] arguments = input.readUTF().split("\\s",3);

                    if (arguments[0].equals("exit")) {
                        output.writeUTF("OK");
                        break;
                    }

                    String response = processArguments(arguments);
                    output.writeUTF(response);
                    System.out.println("Sent: " + response);

                } catch (IOException e) {
                    System.out.println("Server exception: " + e);
                }
            } catch (IOException e) {
                System.out.println("Server exception: " + e);
            }
        }
    }

    public static String processArguments(String[] arguments) {
        if (arguments.length < 2) {
            return "ERROR";
        }

        String command = arguments[0];
        if (command.isEmpty()) {
            return "ERROR";
        }

        int position;
        try {
            position = Integer.parseInt(arguments[1]) - 1;
        } catch (NumberFormatException e) {
            return "ERROR";
        }

        if (position < 0 || position > 999) {
           return "ERROR";
        }

        switch (command) {
            case "set" -> {
                if (arguments.length != 3) {
                    return "ERROR";
                }
                array[position] = arguments[2];
                return "OK";
            }
            case "get" -> {
                if (array[position] != null) {
                    return array[position];
                } else {
                    return "ERROR";
                }
            }
            case "delete" -> {
                array[position] = null;
                return "OK";
            }
            default ->{
                return "Unknown command";
            }
        }
    }

}
