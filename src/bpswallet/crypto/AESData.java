package bpswallet.crypto;

import bpswallet.ser.VarInt;

public class AESData implements EncryptedData {

    private final String keyHash;
    private final String salt;
    private final String ciphertext;

    public AESData(SaltedKey saltedKey, String ciphertext) {
        keyHash = saltedKey.getKeyHash();
        salt = saltedKey.getSalt();
        this.ciphertext = ciphertext;
    }

    public AESData(String aesData) {
        int offset = 0;
        keyHash = aesData.substring(offset, offset += 64);
        VarInt saltVar = new VarInt(aesData.substring(offset));
        offset += saltVar.hexLength();
        int saltLength = saltVar.toInt() * 2;
        salt = aesData.substring(offset, offset += saltLength);
        VarInt ciphertextVar = new VarInt(aesData.substring(offset));
        offset += ciphertextVar.hexLength();
        int ciphertextLength = ciphertextVar.toInt() * 2;
        ciphertext = aesData.substring(offset, offset + ciphertextLength);
    }

    @Override
    public String getKeyHash() {
        return keyHash;
    }

    @Override
    public String getSalt() {
        return salt;
    }

    @Override
    public String getData() {
        return ciphertext;
    }

    public String getHex() {
        String saltLength = new VarInt(salt.length() / 2).toHex();
        String ciphertextLength = new VarInt(ciphertext.length() / 2).toHex();
        return keyHash + saltLength + salt + ciphertextLength + ciphertext;
    }
}
