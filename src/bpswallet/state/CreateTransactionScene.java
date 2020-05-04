package bpswallet.state;

import bpswallet.wallet.BPSWallet;
import bpswallet.ser.Address;
import bpswallet.ser.AddressFactory;
import bpswallet.txn.Outpoint;
import bpswallet.txn.Transaction;
import bpswallet.txn.TransactionInput;
import bpswallet.txn.TransactionOutput;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;

public class CreateTransactionScene extends FlowScene {

    public static final int TXNVERSION = 0;
    public static final int NUM_INPUTS = 1;
    public static final int INPUTS = 2;
    public static final int NUM_RECIPIENTS = 3;
    public static final int RECIPIENTS = 4;
    public static final int LOCKTIME = 5;
    public static final int MINERFEE = 6;
    public static final int SIGNTXN = 7;
    public static final int TXNINFO = 8;
    private final BPSWallet wallet;
    private final boolean raw, spendAll;
    private int numInputs, numOutputs;
    private double feerate;
    private long valueIn, valueOut;
    private final Transaction transaction;

    public CreateTransactionScene(BPSWallet wallet, boolean spendAll, boolean raw) {
        super("Create New Transaction");
        this.autoclear = false;
        this.wallet = wallet;
        this.spendAll = spendAll;
        this.raw = raw;
        if (!spendAll && raw) {
            valueIn = 0;
        } else {
            valueIn = wallet.getBalance();
        }
        valueOut = 0;
        feerate = 2;
        transaction = new Transaction();
        if (spendAll) {
            numOutputs = 1;
            this.setStep(RECIPIENTS);
        } else if (!raw) {
            this.setStep(NUM_RECIPIENTS);
        }
    }

    @Override
    protected void updateOptions() {
        if (!this.stepChanged()) {
            return;
        }
        options.clear();
        switch (this.getStep()) {
            case TXNVERSION:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().isEmpty()) {
                            context.incrementStep();
                            return;
                        }
                        try {
                            int version = Integer.parseInt(state.getInput());
                            if (version == 1 || version == 2) {
                                transaction.setVersion(version);
                                context.incrementStep();
                            } else {
                                System.out.println("Version must be either 1 or 2.");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case NUM_INPUTS:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        try {
                            numInputs = Integer.parseInt(state.getInput());
                            if (numInputs > 0) {
                                context.incrementStep();
                            } else {
                                System.out.println("Number of Inputs must be a positive, non-zero value.");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case INPUTS:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().equalsIgnoreCase("Q")) {
                            state.setScene(new MainWalletScene(wallet));
                            return;
                        }
                        String[] inputs = state.getInput().split(":");
                        if (inputs.length == 2) {
                            try {
                                String hash = inputs[0].trim();
                                int index = Integer.parseInt(inputs[1].trim());
                                Outpoint outpoint = new Outpoint(ByteUtil.flipendian(hash), index);
                                if (wallet.isOwnerOf(outpoint)) {
                                    TransactionInput input = new TransactionInput(outpoint);
                                    transaction.addInput(input);
                                    Transaction refTxn = FileUtil.getTransaction(outpoint.getHash(), false);
                                    valueIn += refTxn.getOutputAt(outpoint.getIndex()).getValue();
                                    if (transaction.getNumInputs() == numInputs) {
                                        context.incrementStep();
                                    }
                                } else {
                                    System.out.println("Outpoint is not related to this wallet.");
                                    state.pause(1000);
                                }
                            } catch (NumberFormatException ex) {
                                System.out.println("Index must be a positive value.");
                                state.pause(1000);
                            }
                        } else {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case NUM_RECIPIENTS:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        try {
                            numOutputs = Integer.parseInt(state.getInput());
                            if (numOutputs > 0) {
                                context.incrementStep();
                            } else {
                                System.out.println("Number of Inputs must be a positive, non-zero value.");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case RECIPIENTS:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().equalsIgnoreCase("Q")) {
                            state.setScene(new MainWalletScene(wallet));
                            return;
                        }
                        if (spendAll) {
                            Address address = AddressFactory.fromEncoded(state.getInput().trim());
                            if (address != null) {
                                TransactionOutput output = new TransactionOutput(0, address);
                                transaction.addOutput(output);
                                context.setStep(MINERFEE);
                            } else {
                                System.out.println("Address is invalid.");
                                state.pause(1000);
                            }
                        } else {
                            String[] recipient = state.getInput().replaceAll("\\s", "").split(",");
                            if (recipient.length == 2) {
                                Address address = AddressFactory.fromEncoded(recipient[0]);
                                if (address != null) {
                                    long amount = BPSWallet.btc2sats(recipient[1]);
                                    if (amount + valueOut > valueIn) {
                                        System.out.println("You do not have enough funds for that.");
                                        state.pause(1000);
                                    } else if (amount >= 546) {
                                        TransactionOutput output = new TransactionOutput(amount, address.getScriptPubKey());
                                        transaction.addOutput(output);
                                        valueOut += amount;
                                        if (transaction.getNumOutputs() == numOutputs) {
                                            if (raw) {
                                                context.incrementStep();
                                            } else {
                                                context.setStep(MINERFEE);
                                            }
                                        }
                                    } else {
                                        System.out.println("Amount is invalid or too small.");
                                        state.pause(1000);
                                    }
                                } else {
                                    System.out.println("Address is invalid.");
                                    state.pause(1000);
                                }
                            } else {
                                state.printInvalidInputNotice();
                            }
                        }
                    }
                });
                break;
            case LOCKTIME:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        if (state.getInput().isEmpty()) {
                            context.incrementStep();
                            return;
                        }
                        try {
                            int locktime = Integer.parseInt(state.getInput());
                            if (locktime >= 0) {
                                transaction.setLocktime(locktime);
                                context.incrementStep();
                            } else {
                                System.out.println("Locktime must be a positive value or 0.");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case MINERFEE:
                options.add(new Option<CreateTransactionScene>(this, "", "") {
                    @Override
                    public void trigger() {
                        try {
                            feerate = Double.parseDouble(state.getInput());
                            if (feerate >= 1) {
                                if (transaction.getNumInputs() == 0) {
                                    if (spendAll) {
                                        wallet.assignAllInputs(transaction, feerate);
                                    } else {
                                        wallet.assignInputs(transaction, feerate);
                                    }
                                } else wallet.assignChangeOutput(transaction, feerate);
                                context.incrementStep();
                            } else {
                                System.out.println("Feerate must be a positive value >= 1.0");
                                state.pause(1000);
                            }
                        } catch (NumberFormatException ex) {
                            state.printInvalidInputNotice();
                        }
                    }
                });
                break;
            case SIGNTXN:
                options.add(new Option<CreateTransactionScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        char[] password = new char[0];
                        if (context.wallet.isPasswordProtected()) {
                            int attempts = 0;
                            while (attempts < 3) {
                                password = askForPassword();
                                if (wallet.testPassword(password)) {
                                    if (!wallet.signTransaction(transaction, password)) {
                                        System.out.println("There was an error signing the transaction.");
                                        state.pause(1000);
                                        state.setScene(new MainWalletScene(wallet));
                                    }
                                    state.setSkipInput(true);
                                    context.incrementStep();
                                    return;
                                } else if (attempts < 3) {
                                    System.out.println("Invalid Password.");
                                }
                            }
                        } else if (!wallet.signTransaction(transaction, password)) {
                            System.out.println("There was an error signing the transaction.");
                            state.pause(1000);
                            state.setScene(new MainWalletScene(wallet));
                        } else {
                            context.incrementStep();
                        }
                    }
                });
                options.add(new Option<CreateTransactionScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        state.setScene(new MainWalletScene(wallet));
                    }
                });
                break;
            case TXNINFO:
                options.add(new Option<CreateTransactionScene>(this, "Y", "Yes") {
                    @Override
                    public void trigger() {
                        FileUtil.addTransaction(transaction);
                        wallet.addTransaction(transaction);
                        System.out.println("Transaction added.");
                        state.pause(1000);
                        state.setScene(new MainWalletScene(wallet));
                    }
                });
                options.add(new Option<CreateTransactionScene>(this, "N", "No") {
                    @Override
                    public void trigger() {
                        state.setScene(new MainWalletScene(wallet));
                    }
                });
                break;
        }
    }

    private char[] askForPassword() {
        System.out.print("Password: ");
        char[] input = state.askForHiddenInput();
        return input;
    }

    @Override
    public void display() {
        updateOptions();
        System.out.println(title);
        switch (this.getStep()) {
            case TXNVERSION:
                System.out.print("Transaction Version (1 or 2, defaults to 1): ");
                break;
            case NUM_INPUTS:
                System.out.print("Number of Inputs: ");
                break;
            case INPUTS:
                System.out.println("Input Transaction Hash and Outpoint Index, separated by a colon (Q to cancel):");
                break;
            case NUM_RECIPIENTS:
                System.out.print("Number of Recipients: ");
                break;
            case RECIPIENTS:
                if (!spendAll) {
                    System.out.println("Recipient Address & Bitcoin Amount to Send, separated by \", \": (Q to cancel)");
                    System.out.println("Amount left to send: " + BPSWallet.sats2btc(valueIn - valueOut) + " BTC");
                } else {
                    System.out.print("Recipient Address (Q to cancel): ");
                    System.out.println("Wallet Balance: " + BPSWallet.sats2btc(valueIn) + " BTC");
                }
                break;
            case LOCKTIME:
                System.out.print("Transaction Locktime (defaults to 0): ");
                break;
            case MINERFEE:
                System.out.print("Miner fee, in sat/vB: ");
                break;
            case SIGNTXN:
                state.clearScreen();
                System.out.println(transaction);
                System.out.println("NOTE: Fee will be inaccurate as transaction has no witness/signatures yet.");
                System.out.println("Sign this transaction?");
                options.forEach((option) -> {
                    System.out.println("[" + option.key + "] - " + option.value);
                });
                break;
            case TXNINFO:
                state.clearScreen();
                System.out.println(transaction);
                System.out.println("\nRAW TRANSACTION:");
                System.out.println("\t" + transaction.getHex());
                System.out.println("\nAdd Transaction?");
                options.forEach((option) -> {
                    System.out.println("[" + option.key + "] - " + option.value);
                });
                break;
        }
    }
}
