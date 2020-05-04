package bpswallet.ser;

import bpswallet.crypto.ECPubKey;
import bpswallet.util.HashUtil;

public class LegacyAddress extends Address {

    public static final String VERSION = "00";
    private final String scriptPubKey;
    private final String pubKeyHash;

    public LegacyAddress(ECPubKey pubKey) {
        super(AddressType.LEGACY);
        pubKeyHash = HashUtil.hash160(pubKey.getEncoded());
        scriptPubKey = "76a914" + pubKeyHash + "88ac";
    }

    public LegacyAddress(String scriptPubKey) {
        super(AddressType.LEGACY);
        this.scriptPubKey = scriptPubKey;
        pubKeyHash = scriptPubKey.substring(6, 46);
    }

    public String getPubKeyHash() {
        return pubKeyHash;
    }

    @Override
    public String getScriptPubKey() {
        return scriptPubKey;
    }

    @Override
    public String getEncoded() {
        return Base58Check.hexToBase58(VERSION + pubKeyHash);
    }

    @Override
    public AddressType getType() {
        return AddressType.LEGACY;
    }

    public static LegacyAddress fromEncoded(String encoded) {
        String pubKeyHash = Base58Check.base58ToHex(encoded).substring(2);
        return new LegacyAddress("76a914" + pubKeyHash + "88ac");
    }
}
