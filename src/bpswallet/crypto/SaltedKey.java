package bpswallet.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import bpswallet.util.ByteUtil;
import bpswallet.util.HashUtil;

public class SaltedKey {

    private final SecretKey key;
    private final String keyHash;
    private final String salt;

    public SaltedKey(char[] password, String salt) {
        this.salt = salt;
        key = HashUtil.PBKDF2(password, salt, 2048, 32 * 8);
        keyHash = HashUtil.doubleSha256(ByteUtil.hexify(key.getEncoded()));
    }

    public SaltedKey(char[] password) {
        this(password, ByteUtil.randHex(6));
    }

    public AESData encrypt(String data) {
        String ciphertext = "";
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey aesKey = new SecretKeySpec(key.getEncoded(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] dataBytes;
            if (data.matches("-?[0-9a-fA-F]+")) {
                dataBytes = ByteUtil.hex2bytes(data);
            } else {
                dataBytes = data.getBytes(StandardCharsets.UTF_8);
            }
            ciphertext = ByteUtil.hexify(cipher.doFinal(dataBytes));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException(ex);
        }
        return new AESData(this, ciphertext);
    }

    public String decrypt(AESData data) throws InvalidPasswordException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey aesKey = new SecretKeySpec(key.getEncoded(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return ByteUtil.hexify(cipher.doFinal(ByteUtil.hex2bytes(data.getData())));
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException
                | IllegalBlockSizeException | NoSuchPaddingException ex) {
            throw new InvalidPasswordException();
        }
    }

    public String decrypt(ECIESData data) throws InvalidPasswordException {
        ECPubKey ephemPubKey = data.getEphemPubKey();
        ECPrvKey prvKey = new ECPrvKey(ByteUtil.hexify(this.key.getEncoded()));
        ECPubKey encryptionKey = new ECPubKey(ScalarMultiply.scalmult(ephemPubKey.getPoint(), prvKey.getSecret()));
        String encryptionKeyHash = HashUtil.doubleSha256(encryptionKey.getEncoded());
        if (!data.getKeyHash().equalsIgnoreCase(encryptionKeyHash)) {
            throw new InvalidPasswordException();
        }
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey.asSecretKey());
            String plaintext = ByteUtil.hexify(cipher.doFinal(ByteUtil.hex2bytes(data.getData())));
            return plaintext;
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException
                | IllegalBlockSizeException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public SecretKey getKey() {
        return key;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getSalt() {
        return salt;
    }
}
