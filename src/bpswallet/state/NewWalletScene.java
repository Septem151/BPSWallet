package bpswallet.state;

import bpswallet.wallet.BPSWallet;
import bpswallet.ser.AddressType;
import bpswallet.wallet.BIP32;
import bpswallet.crypto.InvalidPasswordException;
import bpswallet.util.FileUtil;
import java.util.Arrays;

public class NewWalletScene extends FlowScene {

    private static final int FILENAME = 0;
    private static final int ADVANCED = 1;
    private static final int SEEDLENGTH = 2;
    private static final int DERIVATION = 3;
    private static final int ADDRESSTYPE = 4;
    private static final int PASSPHRASE = 5;
    private static final int PASSWORD = 6;
    private static final int CONFIRM = 7;
    private static final int SHOWSEED = 8;

    private String fileName, derivation;
    private AddressType addressType;
    private int seedLength;
    private char[] password, passphrase;
    private boolean inputMatches;
    private BPSWallet wallet;

    public NewWalletScene() {
        super("New Wallet");
        fileName = "";
        derivation = BPSWallet.DEFAULT_DERIVATION;
        addressType = BPSWallet.DEFAULT_ADDR_TYPE;
        seedLength = BPSWallet.DEFAULT_SEEDLENGTH;
        password = new char[0];
        passphrase = new char[0];
        wallet = null;
    }

    @Override
    protected void updateOptions() {
        if (!this.stepChanged()) {
            return;
        }
        options.clear();
        switch (this.getStep()) {
            case FILENAME: // File Name Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        String input = state.getInput().trim();
                        if (input.isEmpty()) {
                            state.printInvalidInputNotice();
                        } else if (!FileUtil.walletFileExists(input)) {
                            fileName = input;
                            context.incrementStep();
                        } else {
                            System.out.println("A wallet with this name already exists.");
                            state.pause(1000);
                        }
                    }
                });
                break;
            case ADVANCED: // Advanced Options Prompt
                options.add(new Option<NewWalletScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        context.incrementStep();
                    }
                });
                options.add(new Option<NewWalletScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        context.setStep(PASSWORD);
                    }
                });
                break;
            case SEEDLENGTH: // Seed Length Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().isEmpty()) {
                            context.incrementStep();
                            return;
                        }
                        try {
                            int length = Integer.parseInt(state.getInput());
                            if (length == 12 || length == 24) {
                                seedLength = length;
                                context.incrementStep();
                            } else {
                                state.printInvalidInputNotice();
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case DERIVATION: // Derivation Path Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().isEmpty()) {
                            context.incrementStep();
                        } else if (BIP32.testDerivation(state.getInput())) {
                            derivation = state.getInput();
                            context.incrementStep();
                        } else {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case ADDRESSTYPE: // Address Type Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        String input = state.getInput();
                        if (input.isEmpty()) {
                            context.incrementStep();
                        } else if (input.equalsIgnoreCase("legacy") || input.equalsIgnoreCase("segwit") || input.equalsIgnoreCase("bech32")) {
                            addressType = AddressType.fromName(value);
                            context.incrementStep();
                        } else {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case PASSPHRASE: // Passphrase Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (inputMatches) {
                            context.incrementStep();
                        } else {
                            System.out.println("Passphrase does not match.");
                            state.pause(1000);
                        }
                    }
                });
                break;
            case PASSWORD: // Password Prompt
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (inputMatches) {
                            context.incrementStep();
                        } else {
                            System.out.println("Password does not match.");
                            state.pause(1000);
                        }
                    }
                });
                break;
            case CONFIRM: // Confirm Wallet Creation Prompt
                options.add(new Option<NewWalletScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        wallet = new BPSWallet(fileName, seedLength, passphrase, derivation, addressType, BPSWallet.DEFAULT_LOOKAHEAD, password);
                        FileUtil.writeWalletToFile(wallet);
                        context.incrementStep();
                    }
                });
                options.add(new Option<NewWalletScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        state.setScene(new TitleScene());
                    }
                });
                break;
            case SHOWSEED: // Show Recovery Phrase
                options.add(new Option<NewWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        state.setScene(new MainWalletScene(wallet));
                    }
                });
                break;
        }
    }

    private void askForPassphrase() {
        System.out.print("Set Passphrase: ");
        char[] input = state.askForHiddenInput();
        if (input.length != 0) {
            System.out.print("Confirm Passphrase: ");
            inputMatches = Arrays.equals(state.askForHiddenInput(), input);
            if (inputMatches) {
                passphrase = input;
            }
        } else {
            inputMatches = true;
        }
    }

    private void askForPassword() {
        System.out.print("Set Password: ");
        char[] input = state.askForHiddenInput();
        if (input.length != 0) {
            System.out.print("Confirm Password: ");
            inputMatches = Arrays.equals(state.askForHiddenInput(), input);
            if (inputMatches) {
                password = input;
            }
        } else {
            inputMatches = true;
        }
    }

    @Override
    public void display() {
        updateOptions();
        System.out.println(title);
        switch (this.getStep()) {
            case FILENAME: // File Name Prompt
                System.out.print("File Name (Cannot be empty): ");
                break;
            case ADVANCED: // Advanced Options Prompt
                System.out.println("Advanced Options?");
                options.forEach((option) -> {
                    System.out.println("[" + option.key + "] - " + option.value);
                });
                break;
            case SEEDLENGTH: // Seed Length Prompt
                System.out.print("Seed Length (12 or 24 - defaults to " + BPSWallet.DEFAULT_SEEDLENGTH + "): ");
                break;
            case DERIVATION: // Derivation Path Prompt
                System.out.print("Derivation Path (defaults to " + BPSWallet.DEFAULT_DERIVATION + "): ");
                break;
            case ADDRESSTYPE: // Address Type Prompt
                System.out.print("Address Type (legacy, segwit, or bech32 - defaults to " + BPSWallet.DEFAULT_ADDR_TYPE + "): ");
                break;
            case PASSPHRASE: // Passphrase Prompt
                askForPassphrase();
                break;
            case PASSWORD: // Password Prompt
                askForPassword();
                break;
            case CONFIRM: // Confirm Wallet Creation Prompt
                System.out.println("Create this wallet?");
                options.forEach((option) -> {
                    System.out.println("[" + option.key + "] - " + option.value);
                });
                break;
            case SHOWSEED: // Show Recovery Phrase
                System.out.println("!! TAKE NOTE OF YOUR RECOVERY PHRASE !!\n");
                try {
                    System.out.println(wallet.decryptMnemonic(password));
                } catch (InvalidPasswordException ex) {
                    throw new RuntimeException(ex);
                }
                System.out.println("\n[Any Key] - Open Wallet");
                break;
        }
    }
}
