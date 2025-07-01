package server;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String[] array = new String[1000];

        while (true) {
            String command = scanner.nextLine();

            if (command.equals("exit")) break;

            if (!command.isEmpty()) {
                String[] parts = command.split("\\s+", 3);

                if (parts.length >= 2) {
                    if (isInteger(parts[1])) {
                        int position = Integer.parseInt(parts[1]) - 1;
                        if (position < 0 || position > 999) {
                            System.out.println("ERROR");
                            continue;
                        }
                        switch (parts[0]) {
                            case "set" -> {
                                if (parts.length == 3) {
                                    array[position] = parts[2];
                                    System.out.println("OK");
                                }
                            }
                            case "get" -> System.out.println(array[position] == null ? "ERROR" : array[position]);
                            case "delete" -> {
                                array[position] = null;
                                System.out.println("OK");
                            }
                        }
                    }
                }

            }
        }
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
