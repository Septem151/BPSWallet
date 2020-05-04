package bpswallet.ser;

import bpswallet.crypto.ECPubKey;
import bpswallet.util.HashUtil;

public class SegwitAddress extends Address {

    public static final String VERSION = "05";
    private final String scriptPubKey;
    private final String scriptHash;

    public SegwitAddress(ECPubKey pubKey) {
        super(AddressType.SEGWIT);
        String pubKeyHash = HashUtil.hash160(pubKey.getEncoded());
        String redeemScript = "0014" + pubKeyHash;
        scriptHash = HashUtil.hash160(redeemScript);
        scriptPubKey = "a914" + scriptHash + "87";
    }

    public SegwitAddress(String scriptPubKey) {
        super(AddressType.SEGWIT);
        this.scriptPubKey = scriptPubKey;
        scriptHash = scriptPubKey.substring(4, 44);
    }

    public String getScriptHash() {
        return scriptHash;
    }

    @Override
    public String getScriptPubKey() {
        return scriptPubKey;
    }

    @Override
    public String getEncoded() {
        return Base58Check.hexToBase58(VERSION + scriptHash);
    }

    @Override
    public AddressType getType() {
        return AddressType.SEGWIT;
    }

    public static SegwitAddress fromEncoded(String encoded) {
        String scriptHash = Base58Check.base58ToHex(encoded).substring(2);
        return new SegwitAddress("a914" + scriptHash + "87");
    }
}
