package bpswallet.wallet;

import bpswallet.ser.Address;
import bpswallet.ser.AddressFactory;
import bpswallet.ser.AddressType;
import bpswallet.txn.Outpoint;
import bpswallet.txn.Transaction;
import bpswallet.txn.TransactionInput;
import bpswallet.txn.TransactionOutput;
import bpswallet.crypto.AESData;
import bpswallet.crypto.ECIESData;
import bpswallet.crypto.ECKeyPair;
import bpswallet.crypto.ECPrvKey;
import bpswallet.crypto.ECPubKey;
import bpswallet.crypto.InvalidPasswordException;
import bpswallet.crypto.SaltedKey;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class BPSWallet {

    public static final String DEFAULT_DERIVATION = "m/84'/0'/0'";
    public static final AddressType DEFAULT_ADDR_TYPE = AddressType.BECH32;
    public static final int DEFAULT_SEEDLENGTH = 12;
    public static final int DEFAULT_LOOKAHEAD = 10;
    // WALLET OBJECTS
    private final AESData mnemonicEncrypted, xprvEncrypted;
    private final String derivation;
    private final ExtendedPubKey xpub;
    private final ArrayList<Outpoint> coins;
    private final ArrayList<Address> extAddresses, intAddresses;
    // REQUIRED FILES OBJECTS
    private final boolean passwordProtected;
    private final String fileName, ownerSalt;
    private final ECPubKey ownerPubKey;

    public BPSWallet(String fileName, int seedLength, char[] passphrase, String derivation, AddressType addrType, int lookahead, char[] password) {
        this(fileName, BIP39.generateMnemonic(seedLength), passphrase, derivation, addrType, lookahead, password);
    }

    public BPSWallet(String fileName, String mnemonic, char[] passphrase, String derivation, AddressType addrType, int lookahead, char[] password) {
        this.fileName = fileName;
        ExtendedPrvKey xprv = BIP32.deriveXprv(addrType, BIP39.generateSeed(mnemonic, passphrase), derivation);
        this.derivation = derivation;
        xpub = BIP32.neuter(xprv);
        SaltedKey key = new SaltedKey(password);
        ownerSalt = key.getSalt();
        mnemonicEncrypted = key.encrypt(mnemonic);
        xprvEncrypted = key.encrypt(xprv.getHex());
        ownerPubKey = new ECKeyPair(ByteUtil.hexify(key.getKey().getEncoded())).getPub();
        coins = new ArrayList<>();
        ExtendedPubKey extXpub = BIP32.CKDpub(xpub, 0);
        ExtendedPubKey intXpub = BIP32.CKDpub(xpub, 1);
        extAddresses = genAddresses(extXpub, lookahead);
        intAddresses = genAddresses(intXpub, lookahead);
        passwordProtected = password.length > 0;
    }

    public BPSWallet(String fileName, ECIESData encrypted, char[] password) throws InvalidPasswordException {
        this.fileName = fileName;
        this.ownerSalt = encrypted.getSalt();
        SaltedKey key = new SaltedKey(password, ownerSalt);
        String hex = key.decrypt(encrypted);
        ownerPubKey = new ECKeyPair(ByteUtil.hexify(key.getKey().getEncoded())).getPub();
        passwordProtected = password.length > 0;
        coins = new ArrayList<>();
        extAddresses = new ArrayList<>();
        intAddresses = new ArrayList<>();
        int offset = 0;
        int derivationLength = ByteUtil.hex2int(hex.substring(offset, offset += 8), false) * 2;
        derivation = new String(ByteUtil.hex2bytes(hex.substring(offset, offset += derivationLength)), StandardCharsets.UTF_8);
        xpub = new ExtendedPubKey(hex.substring(offset, offset += 156));
        int mnemonicEncryptedLength = ByteUtil.hex2int(hex.substring(offset, offset += 8), false) * 2;
        mnemonicEncrypted = new AESData(hex.substring(offset, offset += mnemonicEncryptedLength));
        int xprvEncryptedLength = ByteUtil.hex2int(hex.substring(offset, offset += 8), false) * 2;
        xprvEncrypted = new AESData(hex.substring(offset, offset += xprvEncryptedLength));
        int numCoins = ByteUtil.hex2int(hex.substring(offset, offset += 8), false);
        for (int i = 0; i < numCoins; i++) {
            String hash = hex.substring(offset, offset += 64);
            int index = ByteUtil.hex2int(hex.substring(offset, offset += 8), true);
            coins.add(new Outpoint(hash, index));
        }
        int numExtAddresses = ByteUtil.hex2int(hex.substring(offset, offset += 8), false);
        for (int i = 0; i < numExtAddresses; i++) {
            Address address = AddressFactory.fromHex(hex.substring(offset));
            offset += address.getHex().length();
            extAddresses.add(address);
        }
        int numIntAddresses = ByteUtil.hex2int(hex.substring(offset, offset += 8), false);
        for (int i = 0; i < numIntAddresses; i++) {
            Address address = AddressFactory.fromHex(hex.substring(offset));
            offset += address.getHex().length();
            intAddresses.add(address);
        }
    }

    public void addTransaction(Transaction txn) {
        for (int i = 0; i < txn.getNumInputs(); i++) {
            TransactionInput input = txn.getInputs().get(i);
            Outpoint outpoint = input.getOutpoint();
            if (coins.contains(outpoint)) {
                coins.remove(outpoint);
            }
        }
        for (int i = 0; i < txn.getNumOutputs(); i++) {
            boolean found = false;
            TransactionOutput output = txn.getOutputAt(i);
            Outpoint outpoint = new Outpoint(txn.getHash(false), i);
            if (output.getAddress() == null) {
                continue;
            }
            if (coins.contains(outpoint)) {
                continue;
            }
            for (Address addr : extAddresses) {
                if (output.getAddress().equals(addr)) {
                    coins.add(outpoint);
                    addr.setUsed(true);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Address addr : intAddresses) {
                    if (output.getAddress().equals(addr)) {
                        coins.add(outpoint);
                        addr.setUsed(true);
                        break;
                    }
                }
            }
        }
    }

    public String decryptMnemonic(char[] password) throws InvalidPasswordException {
        SaltedKey key = new SaltedKey(password, ownerSalt);
        byte[] bytes = ByteUtil.hex2bytes(key.decrypt(mnemonicEncrypted));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public ExtendedPrvKey decryptXprv(char[] password) throws InvalidPasswordException {
        SaltedKey key = new SaltedKey(password, ownerSalt);
        return new ExtendedPrvKey(key.decrypt(xprvEncrypted));
    }

    public boolean isPasswordProtected() {
        return passwordProtected;
    }

    public boolean testPassword(char[] password) {
        try {
            this.decryptXprv(password);
            return true;
        } catch (InvalidPasswordException ex) {
            return false;
        }
    }

    public boolean isOwnerOf(Outpoint outpoint) {
        return coins.contains(outpoint);
    }

    public Address deriveExtAddress() {
        ExtendedPubKey extXpub = BIP32.CKDpub(xpub, 0);
        ExtendedPubKey addrXpub = BIP32.CKDpub(extXpub, extAddresses.size());
        Address address = AddressFactory.fromExtendedKey(addrXpub);
        extAddresses.add(address);
        return address;
    }

    public Address deriveIntAddress() {
        ExtendedPubKey intXpub = BIP32.CKDpub(xpub, 1);
        ExtendedPubKey addrXpub = BIP32.CKDpub(intXpub, intAddresses.size());
        Address address = AddressFactory.fromExtendedKey(addrXpub);
        intAddresses.add(address);
        return address;
    }

    public Address getExtAddress() {
        for (Address addr : extAddresses) {
            if (!addr.isUsed()) {
                return addr;
            }
        }
        return this.deriveExtAddress();
    }

    public Address getIntAddress() {
        for (Address addr : intAddresses) {
            if (!addr.isUsed()) {
                return addr;
            }
        }
        return this.deriveIntAddress();
    }

    public ArrayList<Address> getExtAddresses() {
        return extAddresses;
    }

    public ArrayList<Address> getIntAddresses() {
        return intAddresses;
    }

    public ArrayList<TransactionOutput> getUnspentOutputs() {
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        for (Outpoint coin : coins) {
            Transaction txn = FileUtil.getTransaction(coin.getHash(), false);
            outputs.add(txn.getOutputAt(coin.getIndex()));
        }
        return outputs;
    }

    public void rescan() {
        coins.clear();
        String[] txnHashes = FileUtil.listAllTransactions(false);
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        ArrayList<String> outputHashes = new ArrayList<>();
        ArrayList<Integer> outputIndeces = new ArrayList<>();
        for (String txnHash : txnHashes) {
            Transaction txn = FileUtil.getTransaction(txnHash, false);
            for (int i = 0; i < txn.getNumOutputs(); i++) {
                outputs.add(txn.getOutputAt(i));
                outputHashes.add(txnHash);
                outputIndeces.add(i);
            }
            for (int i = 0; i < txn.getNumInputs(); i++) {
                inputs.add(txn.getInputs().get(i));
            }
        }
        for (Address address : extAddresses) {
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                if (output.getAddress() != null && output.getAddress().equals(address)) {
                    address.setUsed(true);
                    String hash = outputHashes.get(i);
                    int index = outputIndeces.get(i);
                    coins.add(new Outpoint(hash, index));
                }
            }
        }
        for (Address address : intAddresses) {
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                if (output.getAddress() != null && output.getAddress().equals(address)) {
                    address.setUsed(true);
                    String hash = outputHashes.get(i);
                    int index = outputIndeces.get(i);
                    coins.add(new Outpoint(hash, index));
                }
            }
        }
        for (TransactionInput input : inputs) {
            Outpoint outpoint = input.getOutpoint();
            if (coins.contains(outpoint)) {
                coins.remove(outpoint);
            }
        }
    }

    public ArrayList<Outpoint> getCoins() {
        return coins;
    }

    public long getBalance() {
        long balance = 0;
        for (Outpoint coin : coins) {
            Transaction txn = FileUtil.getTransaction(coin.getHash(), false);
            balance += txn.getOutputAt(coin.getIndex()).getValue();
        }
        return balance;
    }

    public ECIESData getEncrypted() {
        return ownerPubKey.encrypt(passwordProtected, this.getHex(), ownerSalt);
    }

    public String getFileName() {
        return fileName;
    }

    public String getDerivation() {
        return derivation;
    }

    public ExtendedPubKey getXpub() {
        return xpub;
    }

    public void assignInputs(Transaction transaction, double feerate) {
        transaction.clearInputs();
        ArrayList<TransactionOutput> allCoins = this.getUnspentOutputs();
        int txnWU, vBytes;
        long fee;
        int outputsWU = 0;
        long outputsAmt = 0;
        for (TransactionOutput output : transaction.getOutputs()) {
            outputsWU += output.getAddress().getType().OUTPUT_WEIGHT;
            outputsAmt += output.getValue();
        }
        // Iterate through EVERY POSSIBLE COMBINATION to find best WU
        long bestFee = Long.MAX_VALUE;
        ArrayList<TransactionOutput> bestInputs = null;
        TransactionOutput bestChange = null;
        for (int i = 0; i < (1 << allCoins.size()); i++) {
            // Find subset
            ArrayList<TransactionOutput> subset = new ArrayList<>();
            TransactionOutput change = null;
            for (int j = 0; j < allCoins.size(); j++) {
                if ((i & (1 << j)) > 0) {
                    TransactionOutput input = allCoins.get(j);
                    subset.add(input);
                }
            }
            // Test found subset
            boolean segwit = false;
            int inputsWU = 0;
            long inputsAmt = 0;
            for (TransactionOutput input : subset) {
                inputsWU += input.getAddress().getType().INPUT_WEIGHT;
                inputsAmt += input.getValue();
                if (!segwit
                        && (input.getAddress().getType() == AddressType.SEGWIT
                        || input.getAddress().getType() == AddressType.BECH32)) {
                    segwit = true;
                }
            }
            // Calculate fee for this subset
            txnWU = (segwit ? 42 : 40) + outputsWU + inputsWU;
            vBytes = (int) Math.ceil((double) txnWU / 4);
            fee = (long) (feerate * vBytes);
            // If this subset satisfies output reqs + fee...
            if (inputsAmt - outputsAmt >= fee) {
                // Check if these inputs can pay EXACTLY the requested amount...
                long feeWithChange = fee + (long) (feerate * (Math.ceil((double) xpub.getType().OUTPUT_WEIGHT / 4)));
                long leftover = inputsAmt - outputsAmt - feeWithChange;
                if (leftover >= 546) {
                    // If change is possible and greater than dust limit, recalculate the fee based on additional change output
                    fee = feeWithChange;
                    Address addr = this.getIntAddress();
                    change = new TransactionOutput(leftover, addr);
                } else {
                    leftover = inputsAmt - outputsAmt - fee;
                    fee += leftover;
                }
                if (fee < bestFee) {
                    bestFee = fee;
                    bestInputs = subset;
                    if (change != null) {
                        bestChange = change;
                    } else {
                        bestChange = null;
                    }
                }
            }
        }
        if (bestInputs == null) {
            return;
        }
        if (bestChange != null) {
            transaction.addOutput(bestChange);
        }
        Collections.sort(bestInputs, TransactionOutput.reverseValueCompare());
        Collections.sort(transaction.getOutputs(), TransactionOutput.reverseValueCompare());
        for (TransactionOutput coin : bestInputs) {
            int index = allCoins.indexOf(coin);
            Outpoint outpoint = coins.get(index);
            TransactionInput input = new TransactionInput(outpoint);
            transaction.addInput(input);
        }
    }

    public void assignAllInputs(Transaction transaction, double feerate) {
        if (transaction.getNumInputs() != 0
                || transaction.getNumOutputs() != 1) {
            return;
        }
        for (Outpoint outpoint : coins) {
            transaction.addInput(new TransactionInput(outpoint, "", 0xffffffff));
        }
        long fee = (long) Math.ceil(feerate * transaction.getAssumedWeight() / 4);
        transaction.getOutputAt(0).setValue(this.getBalance() - fee);
    }

    public void assignChangeOutput(Transaction transaction, double feerate) {
        TransactionOutput output = new TransactionOutput(0, this.getIntAddress());
        transaction.addOutput(output);
        long fee = (long) Math.ceil(feerate * transaction.getAssumedWeight() / 4);
        long remainder = transaction.getValueIn() - transaction.getValueOut() - fee;
        if (remainder > 546) {
            output.setValue(remainder);
        }
    }

    public boolean signTransaction(Transaction transaction, char[] password) {
        try {
            ArrayList<Address> addresses = new ArrayList<>();
            ArrayList<ECKeyPair> keyPairs = new ArrayList<>();
            for (TransactionInput input : transaction.getInputs()) {
                Outpoint outpoint = input.getOutpoint();
                if (!coins.contains(outpoint)) {
                    return false;
                }
                Transaction refTxn = FileUtil.getTransaction(outpoint.getHash(), false);
                TransactionOutput output = refTxn.getOutputAt(outpoint.getIndex());
                addresses.add(output.getAddress());
            }
            SaltedKey key = new SaltedKey(password, ownerSalt);
            ExtendedPrvKey accountXprv = new ExtendedPrvKey(key.decrypt(xprvEncrypted));
            ExtendedPrvKey extXprv = BIP32.CKDpriv(accountXprv, 0, false);
            ExtendedPrvKey intXprv = BIP32.CKDpriv(accountXprv, 1, false);
            for (Address addr : addresses) {
                int index;
                if (extAddresses.contains(addr)) {
                    index = extAddresses.indexOf(addr);
                    ECPrvKey prvKey = (ECPrvKey) BIP32.CKDpriv(extXprv, index, false).getKey();
                    keyPairs.add(new ECKeyPair(prvKey));
                } else if (intAddresses.contains(addr)) {
                    index = intAddresses.indexOf(addr);
                    ECPrvKey prvKey = (ECPrvKey) BIP32.CKDpriv(intXprv, index, false).getKey();
                    keyPairs.add(new ECKeyPair(prvKey));
                }
            }
            if (addresses.size() != keyPairs.size()) {
                return false;
            } else {
                return transaction.sign(keyPairs);
            }
        } catch (InvalidPasswordException ex) {
            return false;
        }
    }

    private ArrayList<Address> genAddresses(ExtendedPubKey xpub, int lookahead) {
        String[] txnHashes = FileUtil.listAllTransactions(false);
        ArrayList<Address> addresses = new ArrayList<>();
        int numUnused = 0;
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        ArrayList<String> outputHashes = new ArrayList<>();
        ArrayList<Integer> outputIndeces = new ArrayList<>();
        for (String txnHash : txnHashes) {
            Transaction txn = FileUtil.getTransaction(txnHash, false);
            for (int i = 0; i < txn.getNumOutputs(); i++) {
                outputs.add(txn.getOutputAt(i));
                outputIndeces.add(i);
                outputHashes.add(txnHash);
            }
            for (int i = 0; i < txn.getNumInputs(); i++) {
                inputs.add(txn.getInputs().get(i));
            }
        }
        while (numUnused < lookahead) {
            ExtendedPubKey addrXpub = BIP32.CKDpub(xpub, addresses.size());
            Address address = AddressFactory.fromExtendedKey(addrXpub);
            addresses.add(address);
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                if (output.getAddress() != null && output.getAddress().equals(address)) {
                    numUnused = 0;
                    address.setUsed(true);
                    String hash = outputHashes.get(i);
                    int index = outputIndeces.get(i);
                    coins.add(new Outpoint(hash, index));
                }
            }

            if (!address.isUsed()) {
                numUnused++;
            }
        }
        for (TransactionInput input : inputs) {
            Outpoint outpoint = input.getOutpoint();
            if (coins.contains(outpoint)) {
                coins.remove(outpoint);
            }
        }
        return addresses;
    }

    public String getHex() {
        String derivationHex = ByteUtil.hexify(derivation.getBytes(StandardCharsets.UTF_8));
        String hex = ByteUtil.int2hex(derivationHex.length() / 2, false) + derivationHex + xpub.getHex();
        hex += ByteUtil.int2hex(mnemonicEncrypted.getHex().length() / 2, false) + mnemonicEncrypted.getHex();
        hex += ByteUtil.int2hex(xprvEncrypted.getHex().length() / 2, false) + xprvEncrypted.getHex();
        hex += ByteUtil.int2hex(coins.size(), false);
        for (Outpoint outpoint : coins) {
            hex += outpoint.getHex();
        }
        hex += ByteUtil.int2hex(extAddresses.size(), false);
        for (Address address : extAddresses) {
            hex += address.getHex();
        }
        hex += ByteUtil.int2hex(intAddresses.size(), false);
        for (Address address : intAddresses) {
            hex += address.getHex();
        }
        return hex;
    }

    public static long btc2sats(String btc) {
        String[] split = btc.split("\\.");
        if (split.length > 1) {
            split[1] = String.format("%-8s", split[1]).replace(' ', '0');
            split[1] = split[1].substring(0, 8);
        } else {
            split = new String[]{split[0], "00000000"};
        }
        try {
            return Long.parseLong(split[0] + split[1]);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static String sats2btc(long sats) {
        String btc = String.format("%09d", sats);
        return btc.substring(0, btc.length() - 8) + "." + btc.substring(btc.length() - 8);
    }

}
