package bpswallet.tests;

import bpswallet.ser.Base58Check;
import bpswallet.wallet.BIP39;
import bpswallet.wallet.ExtendedPrvKey;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
/**
 *
 * @author Carso
 */
public class BIP39Test {

    private final ArrayList<BIP39TestObject> test_vectors = new ArrayList<>();
    private final Random rand = new Random();

    public BIP39Test() {
        setTestVectors();
    }
    
    public boolean runAllTests() {
        return testGenerateMasterKey() && testGenerateSeed() && testGenerateMnemonic() && testGenerateMnemonicEntropy();
        
    }
    
    public boolean testGenerateMasterKey() {
        System.out.println("generateMasterKey");
        boolean pass = true;
        for(BIP39TestObject vector : test_vectors) {
            ExtendedPrvKey actual = BIP39.generateMasterKey(vector.bip32_xprv.getType(), vector.seed);
            ExtendedPrvKey expected = vector.bip32_xprv;
            if(!actual.equals(expected)) {
                System.out.println("Test failed for seed: " + vector.seed);
                System.out.println("Expected: " + expected.getEncoded());
                System.out.println("Actual:   " + actual.getEncoded());
                pass = false;
            }
        }
        return pass;
    }

    public boolean testGenerateSeed() {
        System.out.println("generateSeed");
        boolean pass = true;
        for (BIP39TestObject vector : test_vectors) {
            String mnemonic = vector.mnemonic;
            char[] passphrase = vector.passphrase;
            String expResult = vector.seed;
            String result = BIP39.generateSeed(mnemonic, passphrase);
            if(!result.equalsIgnoreCase(expResult)) {
                System.out.println("Test failed for mnemonic: " + vector.mnemonic);
                System.out.println("Expected: " + expResult);
                System.out.println("Actual:   " + result);
                pass = false;
            }
        }
        return pass;
    }

    public boolean testGenerateMnemonic() {
        System.out.println("generateMnemonic");
        boolean pass = true;
        for (int i = 0; i < 100; i++) {
            int mnemonicLength = 12;
            String mnemonic = BIP39.generateMnemonic(mnemonicLength);
            boolean result = BIP39.testMnemonic(mnemonic);
            if(!result) {
                System.out.println("Test failed to validate when mnemonic is valid (12 words): " + mnemonic);
                pass = false;
            }
        }
        for (int i = 0; i < 100; i++) {
            int mnemonicLength = 24;
            String mnemonic = BIP39.generateMnemonic(mnemonicLength);
            boolean result = BIP39.testMnemonic(mnemonic);
            if(!result) {
                System.out.println("Test failed to validate when mnemonic is valid (24 words): " + mnemonic);
                pass = false;
            }
        }
        for (int i = 0; i < 100; i++) {
            int mnemonicLength = rand.nextInt(42) + 1;
            mnemonicLength = mnemonicLength - (mnemonicLength % 3);
            String mnemonic = BIP39.generateMnemonic(mnemonicLength);
            boolean result = BIP39.testMnemonic(mnemonic);
            if (mnemonicLength != 0 && mnemonicLength % 3 == 0) {
                if(!result) {
                    System.out.println("Test failed to validate when mnemonic is valid (random # words): " + mnemonic);
                    pass = false;
                }
            } else {
                if(result) {
                    System.out.println("Test failed to invalidate when mnemonic is invalid (random # words): " + mnemonic);
                    pass = false;
                }
            }
        }
        return pass;
    }

    public boolean testGenerateMnemonicEntropy() {
        System.out.println("generateMnemonicEntropy");
        boolean pass = true;
        for (int i = 0; i < 100; i++) {
            int byteLength = 16;
            boolean[] entropy = BIP39.generateMnemonicEntropy(byteLength);
            boolean result = BIP39.testMnemonicEntropy(entropy);
            if(!result) {
                System.out.print("Test failed to validate entropy (128 bits): ");
                for(int j=0; j<entropy.length; j++) System.out.print(entropy[j] ? "1" : "0");
                System.out.println();
                pass = false;
            }
        }
        for (int i = 0; i < 100; i++) {
            int byteLength = 32;
            boolean[] entropy = BIP39.generateMnemonicEntropy(byteLength);
            boolean result = BIP39.testMnemonicEntropy(entropy);
            if(!result) {
                System.out.print("Test failed to validate entropy (256 bits): ");
                for(int j=0; j<entropy.length; j++) System.out.print(entropy[j] ? "1" : "0");
                System.out.println();
                pass = false;
            }
        }
        for (int i = 0; i < 100; i++) {
            int byteLength = rand.nextInt(64) + 1;
            byteLength -= (byteLength % 4);
            boolean[] entropy = BIP39.generateMnemonicEntropy(byteLength);
            boolean result = BIP39.testMnemonicEntropy(entropy);
            if(!result) {
                System.out.print("Test failed to validate entropy (random bits divisible by 32): ");
                for(int j=0; j<entropy.length; j++) System.out.print(entropy[j] ? "1" : "0");
                System.out.println();
                pass = false;
            }
        }
        return pass;
    }

    private void setTestVectors() {
        File file = new File(FileUtil.ETC_DIR + "bip39testvectors.txt");
        try {
            ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(file.toPath());
            for (int i = 0; i < lines.size(); i += 4) {
                byte[] entropy = ByteUtil.hex2bytes(lines.get(i).trim());
                String mnemonic = lines.get(i + 1).trim();
                String seed = lines.get(i + 2).trim();
                ExtendedPrvKey bip32_xprv = new ExtendedPrvKey(Base58Check.base58ToHex(lines.get(i + 3).trim()));
                test_vectors.add(new BIP39TestObject(entropy, mnemonic, "TREZOR".toCharArray(),seed, bip32_xprv));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class BIP39TestObject {

        /**
         * Passphrase: TREZOR
         *
         * File Format: entropy mnemonic passphrase seed bip32_xprv
         */
        public byte[] entropy;
        public String mnemonic;
        public char[] passphrase;
        public String seed;
        public ExtendedPrvKey bip32_xprv;

        public BIP39TestObject(byte[] entropy, String mnemonic, char[] passphrase, String seed, ExtendedPrvKey bip32_xprv) {
            this.entropy = entropy;
            this.mnemonic = mnemonic;
            this.passphrase = passphrase;
            this.seed = seed;
            this.bip32_xprv = bip32_xprv;
        }
    }
}
