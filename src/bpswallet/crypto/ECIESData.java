package bpswallet.crypto;

import bpswallet.ser.VarInt;
import bpswallet.util.ByteUtil;

public class ECIESData implements EncryptedData {

    private final boolean passwordProtected;
    private final String salt;
    private final ECPubKey ephemPubKey;
    private final String encryptionKeyHash;
    private final String ciphertext;

    public ECIESData(boolean passwordProtected, String salt, ECPubKey ephemPubKey, String encryptionKeyHash, String ciphertext) {
        this.passwordProtected = passwordProtected;
        this.salt = salt;
        this.ephemPubKey = ephemPubKey;
        this.encryptionKeyHash = encryptionKeyHash;
        this.ciphertext = ciphertext;
    }

    public ECIESData(byte[] bytes) {
        this(ByteUtil.hexify(bytes));
    }

    public ECIESData(String hex) {
        int offset = 0;
        passwordProtected = hex.substring(offset, offset += 2).equals("01");
        VarInt saltVar = new VarInt(hex.substring(offset));
        offset += saltVar.hexLength();
        int saltLength = saltVar.toInt() * 2;
        salt = hex.substring(offset, offset += saltLength);
        ephemPubKey = new ECPubKey(hex.substring(offset, offset += 66));
        encryptionKeyHash = hex.substring(offset, offset += 64);
        VarInt ciphertextVar = new VarInt(hex.substring(offset));
        offset += ciphertextVar.hexLength();
        int ciphertextLength = ciphertextVar.toInt() * 2;
        ciphertext = hex.substring(offset, offset + ciphertextLength);
    }

    public ECPubKey getEphemPubKey() {
        return ephemPubKey;
    }
    
    public boolean isPasswordProtected() {
        return passwordProtected;
    }

    @Override
    public String getSalt() {
        return salt;
    }

    @Override
    public String getKeyHash() {
        return encryptionKeyHash;
    }

    @Override
    public String getData() {
        return ciphertext;
    }

    public String getHex() {
        String saltLength = new VarInt(salt.length() / 2).toHex();
        String ciphertextLength = new VarInt(ciphertext.length() / 2).toHex();
        return (passwordProtected ? "01" : "00") + saltLength + salt + ephemPubKey.getEncoded() 
                + encryptionKeyHash + ciphertextLength + ciphertext;
    }

    public byte[] getFileFormat() {
        return ByteUtil.hex2bytes(this.getHex());
    }
}
