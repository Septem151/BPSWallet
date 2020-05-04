package bpswallet.ser;

import bpswallet.crypto.ECPubKey;
import bpswallet.util.HashUtil;

public class Bech32Address extends Address {

    public static final String HRP = "bc";
    public static final int VERSION = 0;
    private final String scriptPubKey;
    private final String pubKeyHash;

    public Bech32Address(ECPubKey pubKey) {
        super(AddressType.BECH32);
        pubKeyHash = HashUtil.hash160(pubKey.getEncoded());
        scriptPubKey = "0014" + pubKeyHash;
    }

    public Bech32Address(String scriptPubKey) {
        super(AddressType.BECH32);
        this.scriptPubKey = scriptPubKey;
        pubKeyHash = scriptPubKey.substring(4, 44);
    }

    @Override
    public String getScriptPubKey() {
        return scriptPubKey;
    }

    @Override
    public String getEncoded() {
        return Bech32.hexToBech32(HRP, VERSION, pubKeyHash);
    }

    public static Bech32Address fromEncoded(String encoded) {
        String pubKeyHash = (String) (Bech32.bech32ToHex(encoded)[2]);
        return new Bech32Address("0014" + pubKeyHash);
    }
}
