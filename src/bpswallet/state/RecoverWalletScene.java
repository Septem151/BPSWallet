package bpswallet.state;

import bpswallet.wallet.BPSWallet;
import bpswallet.ser.AddressType;
import bpswallet.wallet.BIP32;
import bpswallet.wallet.BIP39;
import bpswallet.util.FileUtil;
import java.util.Arrays;

public class RecoverWalletScene extends FlowScene {

    private static final int FILENAME = 0;
    private static final int SEEDPHRASE = 1;
    private static final int ADVANCED = 2;
    private static final int PASSPHRASE = 3;
    private static final int DERIVATION = 4;
    private static final int ADDRESSTYPE = 5;
    private static final int LOOKAHEAD = 6;
    private static final int PASSWORD = 7;
    private static final int CONFIRM = 8;

    private String fileName, derivation, seedPhrase;
    private AddressType addressType;
    private char[] password, passphrase;
    private boolean inputMatches;
    private int lookahead;
    private BPSWallet wallet;

    public RecoverWalletScene() {
        super("Recover Wallet");
        fileName = "";
        derivation = BPSWallet.DEFAULT_DERIVATION;
        addressType = BPSWallet.DEFAULT_ADDR_TYPE;
        lookahead = BPSWallet.DEFAULT_LOOKAHEAD;
        seedPhrase = "";
        password = new char[0];
        passphrase = new char[0];
    }

    @Override
    protected void updateOptions() {
        if (!this.stepChanged()) {
            return;
        }
        options.clear();
        switch (this.getStep()) {
            case FILENAME:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
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
            case SEEDPHRASE:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().equalsIgnoreCase("Q")) {
                            state.setScene(new TitleScene());
                            return;
                        }
                        String seedWords[] = state.getInput().split("\\s+");
                        int seedLength = seedWords.length;
                        if (seedLength == 12 || seedLength == 24) {
                            if (BIP39.testMnemonic(state.getInput())) {
                                String phrase = "";
                                for (int i=0; i<seedWords.length; i++) {
                                    phrase += seedWords[i];
                                    if(i != seedWords.length - 1) {
                                        phrase += " ";
                                    }
                                }
                                seedPhrase = phrase;
                                context.incrementStep();
                            } else {
                                System.out.println("Seed checksum is not valid.");
                                state.pause(1000);
                            }
                        } else {
                            System.out.println("Seed must be 12 or 24 words.");
                            state.pause(1000);
                        }
                    }
                });
                break;
            case ADVANCED:
                options.add(new Option<RecoverWalletScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        context.incrementStep();
                    }
                });
                options.add(new Option<RecoverWalletScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        context.setStep(PASSWORD);
                    }
                });
                break;
            case PASSPHRASE:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
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
            case DERIVATION:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
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
            case ADDRESSTYPE:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        String input = state.getInput();
                        if (input.isEmpty()) {
                            context.incrementStep();
                        } else if (input.equalsIgnoreCase("legacy") || input.equalsIgnoreCase("segwit") || input.equalsIgnoreCase("bech32")) {
                            addressType = AddressType.fromName(input);
                            context.incrementStep();
                        } else {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case LOOKAHEAD:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().isEmpty()) {
                            context.incrementStep();
                            return;
                        }
                        try {
                            lookahead = Integer.parseInt(state.getInput());
                            if (lookahead > 0) {
                                context.incrementStep();
                            } else {
                                System.out.println("Lookahead must be a positive, non-zero value.");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case PASSWORD:
                options.add(new Option<RecoverWalletScene>(this, "", "") {
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
            case CONFIRM:
                options.add(new Option<RecoverWalletScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        wallet = new BPSWallet(fileName, seedPhrase, passphrase, derivation, addressType, lookahead, password);
                        state.setScene(new MainWalletScene(wallet));
                    }
                });
                options.add(new Option<RecoverWalletScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        state.setScene(new TitleScene());
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
            case SEEDPHRASE: // Seed Phrase Prompt
                System.out.print("Seed Phrase (Q to cancel): ");
                break;
            case ADVANCED: // Advanced Options Prompt
                System.out.println("Advanced Options?");
                options.forEach((option) -> {
                    System.out.println("[" + option.key + "] - " + option.value);
                });
                break;
            case PASSPHRASE: // Passphrase Prompt
                askForPassphrase();
                break;
            case DERIVATION: // Derivation Path Prompt
                System.out.print("Derivation Path (defaults to " + BPSWallet.DEFAULT_DERIVATION + "): ");
                break;
            case ADDRESSTYPE: // Address Type Prompt
                System.out.print("Address Type (legacy, segwit, or bech32 - defaults to " + BPSWallet.DEFAULT_ADDR_TYPE + "): ");
                break;
            case LOOKAHEAD: // Lookahead Prompt
                System.out.print("Address Lookahead (defaults to " + BPSWallet.DEFAULT_LOOKAHEAD + "): ");
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
        }
    }
}
