package com.pablisco;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;

public class MainApp {

    private final List<User> users;
    private final Scanner scanner;
    private final Writer out;

    public MainApp() {
        this(new LinkedList<>(),
                new Scanner(System.in),
                new PrintWriter(System.out));
    }

    private MainApp(List<User> users,
                    Scanner scanner,
                    Writer out) {
        this.users = users;
        this.scanner = scanner;
        this.out = out;
    }

    public static void main(String... args) {

        new MainApp().execute();

    }

    private void execute() {
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                processInput(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processInput(String line) throws IOException {
        processCommand(line.split(" "));
    }

    private void processCommand(String... args) throws IOException {
        switch (args[0]) {
            case "list":
                writeLine("List of users:");
                users.forEach(this::writeLine);
                break;
            case "add":
                if (args.length > 1) {
                    String name = args[1];
                    User user = new User(name);
                    users.add(user);
                    writeLine(name + " added");
                } else {
                    writeLine("Must provide a name for the user");
                }
                break;
            case "exit":
                System.exit(0);
                break;
            default:
                writeLine("Command not recognised");
            case "help":

                break;
        }
        out.flush();
    }

    private void writeLine(String line) {
        try {
            out.write(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLine(Object line) {
        writeLine(line.toString());
    }

}
