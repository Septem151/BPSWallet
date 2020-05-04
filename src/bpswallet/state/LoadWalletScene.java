package bpswallet.state;

import bpswallet.crypto.InvalidPasswordException;
import bpswallet.util.FileUtil;

public class LoadWalletScene extends Scene {

    private char[] password;

    public LoadWalletScene() {
        super("Load Wallet");
        password = new char[0];
        addOptions();
    }

    private void addOptions() {
        String[] fileNames = FileUtil.listWalletFileNames();
        for (int i = 1; i <= fileNames.length; i++) {
            options.add(new Option<LoadWalletScene>(this, String.valueOf(i), fileNames[i-1]) {
                @Override
                public void trigger() {
                    openWallet(this.value);
                }
            });
        }
        options.add(new Option<LoadWalletScene>(this, "Q", "Back") {
            @Override
            public void trigger() {
                state.setScene(new TitleScene());
            }
        });
    }

    private void openWallet(String fileName) {
        for (int i = 0; i < 3; i++) {
            state.clearScreen();
            try {
                if (FileUtil.walletFileIsEncrypted(fileName)) {
                    System.out.print("Password: ");
                    password = state.askForHiddenInput();
                }
                state.setScene(new MainWalletScene(FileUtil.loadWalletFromFile(fileName, password)));
                return;
            } catch (InvalidPasswordException ex) {
                System.out.println("Invalid password.");
                state.pause(1000);
            }
        }
    }

    @Override
    public void display() {
        System.out.println(title);
        for (Option option : options) {
            System.out.println("[" + option.key + "] - " + option.value);
        }
    }
}
