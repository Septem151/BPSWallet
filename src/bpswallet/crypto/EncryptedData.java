package bpswallet.crypto;

public interface EncryptedData {
    String getSalt();
    String getKeyHash();
    String getData();
}
