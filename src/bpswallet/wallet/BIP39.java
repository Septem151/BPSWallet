package bpswallet.wallet;

import bpswallet.ser.AddressType;
import bpswallet.crypto.ECPrvKey;
import bpswallet.util.ByteUtil;
import bpswallet.util.FileUtil;
import bpswallet.util.HashUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.SecretKey;

public class BIP39 {

    public static ExtendedPrvKey generateMasterKey(AddressType addrType, String seed) {
        String xkey_hex = HashUtil.hmac(ByteUtil.hexify("Bitcoin seed".getBytes(StandardCharsets.UTF_8)),
                seed);
        ECPrvKey prvKey = new ECPrvKey(xkey_hex.substring(0, xkey_hex.length() / 2));
        String chaincode = xkey_hex.substring(xkey_hex.length() / 2);
        return new ExtendedPrvKey(addrType, prvKey, chaincode);
    }

    public static String generateSeed(String mnemonic, char[] passphrase) {
        SecretKey entropy = HashUtil.PBKDF2(mnemonic.toCharArray(),
                ByteUtil.hexify(("mnemonic" + new String(passphrase)).getBytes(StandardCharsets.UTF_8)),
                2048,
                64 * 8);
        return ByteUtil.hexify(entropy.getEncoded());
    }

    public static String generateMnemonic(int mnemonicLength) {
        int entropy_length = ((mnemonicLength * 11) - ((mnemonicLength * 11) % 32)) / 8;
        boolean[] entropy = generateMnemonicEntropy(entropy_length);
        // Split ENT_CS into groups of 11 bits and creates String array for
        // mnemonicWords
        String mnemonicWords = "";
        for (int i = 0; i < mnemonicLength; i++) {
            boolean[] numBits = Arrays.copyOfRange(entropy, i * 11, i * 11 + 11);
            int index = ByteUtil.bits2int(numBits);
            mnemonicWords += FileUtil.getBIP39Words().get(index);
            if (i < mnemonicLength - 1) {
                mnemonicWords += " ";
            }
        }
        return mnemonicWords;
    }

    public static boolean[] generateMnemonicEntropy(int byteLength) {
        // Generate Random Number for Entropy
        String ENT = ByteUtil.randHex(byteLength);
        // Hash the Entropy value
        String HASH = HashUtil.sha256(ENT);
        // Copy first 4 bits of Hash as Checksum
        boolean[] CS = Arrays.copyOfRange(ByteUtil.hex2bits(HASH), 0, (byteLength * 8 / 32));
        // Add Checksum to the end of Entropy bits
        boolean[] ENT_bits = ByteUtil.hex2bits(ENT);
        boolean[] SEED = Arrays.copyOf(ENT_bits, ENT_bits.length + CS.length);
        System.arraycopy(CS, 0, SEED, ENT_bits.length, CS.length);
        return SEED;
    }

    public static boolean testMnemonicEntropy(boolean[] entropy) {
        int ENT_length = (int) ((double) entropy.length / (1.0 + (1.0 / 32)));
        boolean[] ENT_bits = Arrays.copyOfRange(entropy, 0, ENT_length);
        boolean[] checksum = Arrays.copyOfRange(entropy, ENT_length, entropy.length);
        if (ENT_bits.length % 32 != 0) {
            return false;
        }
        String ENT = ByteUtil.bits2hex(ENT_bits);
        String HASH = HashUtil.sha256(ENT);
        boolean[] CS = Arrays.copyOfRange(ByteUtil.hex2bits(HASH), 0, checksum.length);
        return Arrays.equals(checksum, CS);
    }

    public static boolean testMnemonic(String mnemonic) {
        String[] words = mnemonic.trim().split("\\s+");
        ArrayList<String> allWords = FileUtil.getBIP39Words();
        int seedLength = words.length;
        int numBits = (seedLength * 11) - (seedLength * 11) % 32;
        int checksumLength = numBits / 32;
        boolean[] bits = new boolean[numBits + checksumLength];
        for (int i = 0; i < seedLength; i++) {
            String word = words[i];
            int index = allWords.indexOf(word);
            if (index == -1) {
                return false;
            }
            boolean[] indexBits = ByteUtil.int2bip39bits(index);
            for (int j = i * 11; j < indexBits.length * i + 11; j++) {
                //System.out.println("j = " + j);
                bits[j] = indexBits[j % 11];
            }
        }
        return testMnemonicEntropy(bits);
    }
}
