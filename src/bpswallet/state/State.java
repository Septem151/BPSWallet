package bpswallet.state;

import java.util.Scanner;
import java.io.IOException;
import java.io.Console;

public class State {

    private static State instance;
    private Scanner scan;
    private Console console;
    private Scene currentScene;
    private boolean running;
    private boolean skipInput;
    private String input;

    private State() {

    }

    public static State getInstance() {
        if (instance == null) {
            instance = new State();
        }
        return instance;
    }

    private void serverLoop() {
        while (running) {
            if (currentScene.autoclear) {
                clearScreen();
            }
            if (!skipInput) {
                displayScene();
                askForInput();
                processInput();
            } else {
                skipInput = false;
            }
        }
    }

    public void setSkipInput(boolean skipInput) {
        this.skipInput = skipInput;
    }

    public void askForInput() {
        if (!skipInput) {
            input = scan.nextLine();
        }
    }

    public char[] askForHiddenInput() {
        skipInput = true;
        if (console != null) {
            return console.readPassword();
        } else {
            System.out.print("(WARNING - Input will be visible) ");
            return scan.nextLine().toCharArray();
        }
    }

    public String getInput() {
        return input;
    }

    private void processInput() {
        if (!currentScene.processInput(input)) {
            printInvalidInputNotice();
        }
    }

    public void printInvalidInputNotice() {
        System.out.println("Invalid input.");
        if (currentScene.autoclears()) {
            pause(1000);
        }
    }

    private void displayScene() {
        currentScene.display();
    }

    public void setScene(Scene newScene) {
        currentScene = newScene;
    }

    public void start() {
        if (running) {
            System.out.println("State is already running.");
        } else {
            running = true;
            scan = new Scanner(System.in);
            console = System.console();
            currentScene = new TitleScene();
            serverLoop();
        }
    }

    public void stop() {
        if (!running) {
            System.out.println("State is already not running.");
        } else {
            scan.close();
            console = null;
            running = false;
        }
    }

    public void clearScreen() {
        String system = System.getProperty("os.name");
        if (system.startsWith("Linux")) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        } else if (system.startsWith("Windows")) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (IOException | InterruptedException ex) {
                System.out.println("Exception while clearing screen.");
            }
        }
        System.out.println("################################################################################");
        System.out.println("                        Better Privacy & Security Wallet");
        System.out.println("################################################################################\n");
    }

    public void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            System.out.println("Exception while sleeping thread.");
        }
    }

}
