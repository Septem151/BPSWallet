package bpswallet.tests;

import bpswallet.ser.Base58Check;
import bpswallet.wallet.BIP32;
import bpswallet.wallet.ExtendedPrvKey;
import bpswallet.wallet.ExtendedPubKey;
import bpswallet.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class BIP32Test {

    private final ArrayList<BIP32TestObject> test_vectors = new ArrayList<>();

    public BIP32Test() {

    }

    public boolean runAllTests() {
        return testDeriveXpub() && testDeriveXprv() && testTestDerivation();
    }

    public boolean testDeriveXpub() {
        System.out.println("deriveXpub");
        boolean pass = true;
        for (int i = 1; i <= 3; i++) {
            setTestVectors(i);
            for (BIP32TestObject vector : test_vectors) {
                ExtendedPubKey expected = vector.extXpub;
                ExtendedPubKey result = BIP32.deriveXpub(vector.extXpub.getType(), vector.seed, vector.derivation);
                if (!expected.equals(result)) {
                    System.out.println("Test failed for seed: " + vector.seed);
                    System.out.println("Derivation: " + vector.derivation);
                    System.out.println("Expected: " + expected.getEncoded());
                    System.out.println("Actual:   " + result.getEncoded());
                    pass = false;
                }
            }
        }
        return pass;
    }

    public boolean testDeriveXprv() {
        System.out.println("deriveXprv");
        boolean pass = true;
        for (int i = 1; i <= 3; i++) {
            setTestVectors(i);
            for (BIP32TestObject vector : test_vectors) {
                ExtendedPrvKey expected = vector.extXprv;
                ExtendedPrvKey result = BIP32.deriveXprv(vector.extXprv.getType(), vector.seed, vector.derivation);
                if (!expected.equals(result)) {
                    System.out.println("Test failed for seed: " + vector.seed);
                    System.out.println("Derivation: " + vector.derivation);
                    System.out.println("Expected: " + expected.getEncoded());
                    System.out.println("Actual:   " + result.getEncoded());
                    pass = false;
                }
            }
        }
        return pass;
    }

    public boolean testTestDerivation() {
        System.out.println("testDerivation");
        boolean pass = true;
        for (int i = 1; i <= 3; i++) {
            setTestVectors(i);
            for (BIP32TestObject vector : test_vectors) {
                String derivation = vector.derivation;
                if (!BIP32.testDerivation(derivation)) {
                    System.out.println("Test failed for derivation: " + derivation);
                    pass = false;
                }
            }
        }
        return pass;
    }

    private void setTestVectors(int test_unit) {
        test_vectors.clear();
        File file = new File(FileUtil.ETC_DIR + "bip32testvectors" + test_unit + ".txt");
        file.mkdirs();
        try {
            ArrayList<String> lines = (ArrayList<String>) Files.readAllLines(file.toPath());
            String seed = lines.get(0);
            for (int i = 1; i < lines.size(); i += 3) {
                String derivation = lines.get(i);
                ExtendedPubKey extXpub = new ExtendedPubKey(Base58Check.base58ToHex(lines.get(i + 1)));
                ExtendedPrvKey extXprv = new ExtendedPrvKey(Base58Check.base58ToHex(lines.get(i + 2)));
                test_vectors.add(new BIP32TestObject(seed, derivation, extXpub, extXprv));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class BIP32TestObject {

        /**
         * Passphrase: TREZOR
         *
         * File Format: entropy mnemonic passphrase seed bip32_xprv
         */
        public String seed;
        public String derivation;
        public ExtendedPubKey extXpub;
        public ExtendedPrvKey extXprv;

        public BIP32TestObject(String seed, String derivation, ExtendedPubKey extXpub, ExtendedPrvKey extXprv) {
            this.seed = seed;
            this.derivation = derivation;
            this.extXpub = extXpub;
            this.extXprv = extXprv;
        }
    }

}
