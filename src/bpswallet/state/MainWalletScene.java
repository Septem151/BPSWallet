package bpswallet.state;

import bpswallet.wallet.BPSWallet;
import bpswallet.ser.Address;
import bpswallet.ser.AddressFactory;
import bpswallet.txn.Outpoint;
import bpswallet.txn.Transaction;
import bpswallet.txn.TransactionOutput;
import bpswallet.wallet.BIP32;
import bpswallet.wallet.ExtendedPrvKey;
import bpswallet.crypto.ECPrvKey;
import bpswallet.crypto.InvalidPasswordException;
import bpswallet.txn.TransactionInput;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;

public class MainWalletScene extends Scene {

    private BPSWallet wallet;
    private boolean printBeginningMessage;

    public MainWalletScene(BPSWallet wallet) {
        super("Wallet Info");
        this.wallet = wallet;
        addOptions();
        autoclear = false;
        printBeginningMessage = true;
        state.setSkipInput(false);
    }

    private void addOptions() {
        options.add(new Option<MainWalletScene>(this, "balance", "Prints total balance of all coins.") {
            @Override
            public void trigger() {
                String balance = BPSWallet.sats2btc(wallet.getBalance());
                System.out.println("Balance: " + balance + " BTC");
            }
        });
        options.add(new Option<MainWalletScene>(this, "buildtxn", "Create a new transaction from scratch.") {
            @Override
            public void trigger() {
                if (wallet.getBalance() > 0) {
                    state.setScene(new CreateTransactionScene(wallet, false, true));
                } else {
                    System.out.println("One cannot spend what one does not have.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "clear", "Clears the screen.") {
            @Override
            public void trigger() {
                state.clearScreen();
            }
        });
        options.add(new Option<MainWalletScene>(this, "exit", "Saves & exits Wallet.") {
            @Override
            public void trigger() {
                FileUtil.writeWalletToFile(wallet);
                wallet = null;
                state.setScene(new TitleScene());
            }
        });
        options.add(new Option<MainWalletScene>(this, "givecoins", "Gives a specified amount of bitcoin.") {
            @Override
            public void trigger() {
                System.out.print("Amount: ");
                state.askForInput();
                long sats = BPSWallet.btc2sats(state.getInput());
                if (sats > 0) {
                    Address addr = wallet.getExtAddress();
                    Transaction transaction = new Transaction();
                    transaction.addInput(TransactionInput.getCoinbaseInput());
                    transaction.addOutput(new TransactionOutput(sats, addr));
                    wallet.addTransaction(transaction);
                    FileUtil.addTransaction(transaction);
                    FileUtil.writeWalletToFile(wallet);
                    System.out.println("Transaction ID: " + transaction.getHash(true));
                    String balance = BPSWallet.sats2btc(wallet.getBalance());
                    System.out.println("Balance: " + balance + " BTC");
                } else {
                    System.out.println("No coins for you!");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "help", "Prints all valid commands & descriptions.") {
            @Override
            public void trigger() {
                this.context.options.forEach((option) -> {
                    if (!option.isAnyInput()) {
                        System.out.println("[" + option.key + "]\n\t\t" + option.value);
                    }
                });
            }
        });
        options.add(new Option<MainWalletScene>(this, "importtxn", "Adds a new raw transaction to the data directory.") {
            @Override
            public void trigger() {
                importTxn();
            }
        });
        options.add(new Option<MainWalletScene>(this, "importtxns", "Add new raw transactions from a file.") {
            @Override
            public void trigger() {
                importTxns();
            }
        });
        options.add(new Option<MainWalletScene>(this, "listaddresses", "Prints all addresses of this wallet.") {
            @Override
            public void trigger() {
                System.out.println("EXTERNAL ADDRESSES");
                for (int i = 0; i < wallet.getExtAddresses().size(); i++) {
                    System.out.println(wallet.getDerivation() + "/0/" + i + ": " + wallet.getExtAddresses().get(i).getEncoded());
                }
                System.out.println("INTERNAL ADDRESSES");
                for (int i = 0; i < wallet.getIntAddresses().size(); i++) {
                    System.out.println(wallet.getDerivation() + "/1/" + i + ": " + wallet.getIntAddresses().get(i).getEncoded());
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "listcoins", "Prints all coins/outpoints of this wallet.") {
            @Override
            public void trigger() {
                for (Outpoint outpoint : wallet.getCoins()) {
                    Transaction txn = FileUtil.getTransaction(outpoint.getHash(), false);
                    TransactionOutput output = txn.getOutputAt(outpoint.getIndex());
                    System.out.println(ByteUtil.flipendian(outpoint.getHash()) + ":" + outpoint.getIndex() + "\t"
                            + BPSWallet.sats2btc(output.getValue()) + " BTC");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "listtxns", "Prints all transaction hashes in the data directory.") {
            @Override
            public void trigger() {
                String[] txnHashes = FileUtil.listAllTransactions(true);
                for (String hash : txnHashes) {
                    System.out.println(hash);
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "newaddress", "Gets a new, unused receiving address.") {
            @Override
            public void trigger() {
                System.out.println(wallet.getExtAddress().getEncoded());
            }
        });
        options.add(new Option<MainWalletScene>(this, "newchangeaddress", "Gets a new, unused change address.") {
            @Override
            public void trigger() {
                System.out.println(wallet.getIntAddress().getEncoded());
            }
        });
        options.add(
                new Option<MainWalletScene>(this, "rescan", "Rescans transactions for related outputs and updates coins.") {
            @Override
            public void trigger() {
                System.out.println("Rescan in progress, please wait.");
                wallet.rescan();
                String balance = BPSWallet.sats2btc(wallet.getBalance());
                System.out.println("Rescan complete. Balance: " + balance + " BTC");
            }
        });
        options.add(new Option<MainWalletScene>(this, "sendall", "Send all bitcoin to a single address.") {
            @Override
            public void trigger() {
                if (wallet.getBalance() > 0) {
                    state.setScene(new CreateTransactionScene(wallet, true, false));
                } else {
                    System.out.println("One cannot spend what one does not have.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "sendcoins", "Create a new transaction.") {
            @Override
            public void trigger() {
                if (wallet.getBalance() > 0) {
                    state.setScene(new CreateTransactionScene(wallet, false, false));
                } else {
                    System.out.println("One cannot spend what one does not have.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "showprvkey", "Prints the private key of an address.") {
            @Override
            public void trigger() {
                Address address = askForAddress();
                if (address == null) {
                    System.out.println("Address is not valid.");
                    return;
                }
                char[] password = askForPassword();
                int addressIndex = wallet.getExtAddresses().indexOf(address);
                int extOrInt = 0;
                if (addressIndex == -1) {
                    addressIndex = wallet.getIntAddresses().indexOf(address);
                    extOrInt = 1;
                }
                if (addressIndex == -1) {
                    System.out.println("Address is not owned by this wallet.");
                    return;
                }
                try {
                    ExtendedPrvKey xprv = wallet.decryptXprv(password);
                    xprv = BIP32.CKDpriv(xprv, extOrInt, false);
                    xprv = BIP32.CKDpriv(xprv, addressIndex, false);
                    System.out.println("Private Key: " + ((ECPrvKey) xprv.getKey()).getWIF());
                } catch (InvalidPasswordException ex) {
                    System.out.println("Invalid password.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "showseed", "Prints this wallet's seed phrase.") {
            @Override
            public void trigger() {
                char[] password = askForPassword();
                try {
                    String seed = wallet.decryptMnemonic(password);
                    System.out.println(seed);
                } catch (InvalidPasswordException ex) {
                    System.out.println("Invalid password.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "showxprv", "Prints this wallet's account extended private key.") {
            @Override
            public void trigger() {
                char[] password = askForPassword();
                try {
                    ExtendedPrvKey xprv = wallet.decryptXprv(password);
                    System.out.println(xprv.getEncoded());
                } catch (InvalidPasswordException ex) {
                    System.out.println("Invalid password.");
                }
            }
        });
        options.add(new Option<MainWalletScene>(this, "txninfo", "Prints information about a transaction.") {
            @Override
            public void trigger() {
                printTxnInfo();
            }
        });
        options.add(new Option<MainWalletScene>(this, "walletinfo",
                "Prints this wallet's derivation path and account extended public key.") {
            @Override
            public void trigger() {
                System.out.println("Derivation Path: " + wallet.getDerivation());
                System.out.println("Extended Public Key: " + wallet.getXpub().getEncoded());
            }
        });
        options.add(new Option<MainWalletScene>(this, "", "") {
            @Override
            public void trigger() {
                System.out.println("Try typing \"help\" for a list of valid commands.");
            }
        });
    }

    @Override
    public void display() {
        if (printBeginningMessage) {
            state.clearScreen();
            System.out
                    .println("If this is a new wallet, it is advised to import at least one transaction relevant to the wallet.");
            System.out.println("Try typing \"help\" for a list of valid commands.");
            printBeginningMessage = false;
        }
        System.out.print("> ");
    }

    private void importTxn() {
        System.out.print("Raw Transaction Hex: ");
        try {
            state.askForInput();
            Transaction txn = Transaction.fromHex(state.getInput());
            FileUtil.addTransaction(txn);
            wallet.addTransaction(txn);
            System.out.println("Transaction added: " + txn.getHash(true));
        } catch (Exception ex) {
            System.out.println("Transaction import failed.");
        }
    }
    
    private void importTxns() {
        System.out.print("File Name: ");
        try {
            state.askForInput();
            FileUtil.addTransactionsFromFile(state.getInput(), wallet);
        } catch (Exception ex) {
            System.out.println("Failed to import transactions.");
        }
    }

    private void printTxnInfo() {
        System.out.print("Transaction Hash: ");
        state.askForInput();
        String input = state.getInput();
        if (input.length() != 64) {
            System.out.println("Invalid Transaction Hash.");
        } else {
            Transaction txn = FileUtil.getTransaction(input, true);
            if (txn != null) {
                System.out.println("\nRaw Transaction: " + txn.getHex() + "\n");
                System.out.println(txn);
            } else {
                System.out.println("Could not find transaction.");
            }
        }
    }

    private char[] askForPassword() {
        for (int i = 0; i < 3; i++) {
            if (wallet.isPasswordProtected()) {
                System.out.print("Password: ");
                char[] password = state.askForHiddenInput();
                state.setSkipInput(false);
                if (wallet.testPassword(password)) {
                    return password;
                } else {
                    System.out.println("Invalid password.");
                }
            } else {
                return new char[0];
            }
        }
        return new char[0];
    }

    private Address askForAddress() {
        System.out.print("Address: ");
        state.askForInput();
        return AddressFactory.fromEncoded(state.getInput().trim());
    }
}
