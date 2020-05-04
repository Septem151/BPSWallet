package bpswallet.wallet;

import bpswallet.ser.AddressType;
import bpswallet.ser.Base58Check;
import bpswallet.crypto.ECKey;
import bpswallet.crypto.ECPrvKey;
import bpswallet.crypto.ECPubKey;
import bpswallet.util.ByteUtil;
import bpswallet.util.HashUtil;
import java.util.Objects;

public class ExtendedPrvKey implements ExtendedKey {

    private final String id;
    private final AddressType type;
    private final int depth;
    private final String fingerprint;
    private final int childNum;
    private final String chaincode;
    private final ECPrvKey prvKey;

    public ExtendedPrvKey(AddressType type, ECPrvKey prvKey, String chaincode) {
        ECPubKey pubKey = prvKey.getPubKey();
        id = HashUtil.hash160(pubKey.getEncoded());
        this.type = type;
        depth = 0;
        fingerprint = "00000000";
        childNum = 0;
        this.chaincode = chaincode;
        this.prvKey = prvKey;
    }

    public ExtendedPrvKey(ExtendedKey xkeyPar, int childNum, ECPrvKey prvKey, String chaincode) {
        ECPubKey pubKey = prvKey.getPubKey();
        id = HashUtil.hash160(pubKey.getEncoded());
        this.type = xkeyPar.getType();
        depth = xkeyPar.getDepth() + 1;
        fingerprint = xkeyPar.getId().substring(0, 8);
        this.childNum = childNum;
        this.chaincode = chaincode;
        this.prvKey = prvKey;
    }
    
    public ExtendedPrvKey(String hex) {
        int offset = 0;
        type = AddressType.fromXkeyPrefix(hex.substring(offset, offset += 8));
        depth = Integer.parseInt(hex.substring(offset, offset += 2), 16);
        fingerprint = hex.substring(offset, offset += 8);
        childNum = ByteUtil.hex2int(hex.substring(offset, offset += 8), false);
        chaincode = hex.substring(offset, offset += 64);
        // skip leading "00"
        offset += 2;
        prvKey = new ECPrvKey(hex.substring(offset, offset + 64));
        ECPubKey pubKey = prvKey.getPubKey();
        id = HashUtil.hash160(pubKey.getEncoded());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public AddressType getType() {
        return type;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public int getChildNum() {
        return childNum;
    }

    @Override
    public String getChaincode() {
        return chaincode;
    }

    @Override
    public ECKey getKey() {
        return prvKey;
    }

    @Override
    public String getHex() {
        return type.XPRV_PREFIX + String.format("%02x", depth) + fingerprint
                + ByteUtil.int2hex(childNum, false)
                + chaincode + "00" + prvKey.getEncoded();
    }

    @Override
    public String getEncoded() {
        return Base58Check.hexToBase58(this.getHex());
    }
    
    @Override
    public String toString() {
        return this.getEncoded();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof ExtendedPrvKey) {
            ExtendedPrvKey other = (ExtendedPrvKey) o;
            return this.getHex().equalsIgnoreCase(other.getHex());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getHex());
    }
}
