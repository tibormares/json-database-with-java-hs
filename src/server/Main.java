package server;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 23456;
        try (ServerSocket server = new ServerSocket(port,50, InetAddress.getByName(address))) {
            System.out.println("Server started!");
            try (Socket socket = server.accept();
                 DataInputStream input = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
                String receivedMsg = input.readUTF();
                System.out.println("Received: " + receivedMsg);
                String msg = "A record # 12 was sent!";
                output.writeUTF(msg);
                System.out.println("Sent: " + msg);
            } catch (IOException e) {
                System.out.println("Server exception: " + e);
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e);
        }

//        Scanner scanner = new Scanner(System.in);
//        String[] array = new String[1000];
//
//        while (true) {
//            String command = scanner.nextLine();
//
//            if (command.equals("exit")) break;
//
//            if (!command.isEmpty()) {
//                String[] parts = command.split("\\s+", 3);
//
//                if (parts.length >= 2) {
//                    if (isInteger(parts[1])) {
//                        int position = Integer.parseInt(parts[1]) - 1;
//                        if (position < 0 || position > 999) {
//                            System.out.println("ERROR");
//                            continue;
//                        }
//                        switch (parts[0]) {
//                            case "set" -> {
//                                if (parts.length == 3) {
//                                    array[position] = parts[2];
//                                    System.out.println("OK");
//                                }
//                            }
//                            case "get" -> System.out.println(array[position] == null ? "ERROR" : array[position]);
//                            case "delete" -> {
//                                array[position] = null;
//                                System.out.println("OK");
//                            }
//                        }
//                    }
//                }
//
//            }
//        }
//    }
//
//    public static boolean isInteger(String str) {
//        try {
//            Integer.parseInt(str);
//            return true;
//        } catch (NumberFormatException e) {
//            return false;
//        }
    }
}
