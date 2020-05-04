package bpswallet.txn;

import bpswallet.ser.Address;
import bpswallet.ser.AddressFactory;
import bpswallet.ser.AddressType;
import bpswallet.ser.VarInt;
import bpswallet.util.ByteUtil;
import java.util.Comparator;
import java.util.Objects;

public class TransactionOutput {

    private long value;
    private final String scriptPubKey;

    public TransactionOutput(long value, String scriptPubKey) {
        this.value = value;
        this.scriptPubKey = scriptPubKey;
    }

    public TransactionOutput(long value, Address address) {
        this.value = value;
        this.scriptPubKey = address.getScriptPubKey();
    }

    public Address getAddress() {
        return AddressFactory.fromScriptPubKey(scriptPubKey);
    }
    
    public void setValue(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public String getScriptPubKey() {
        return scriptPubKey;
    }

    public String getHex() {
        return ByteUtil.long2hex(value, true) + new VarInt(scriptPubKey.length() / 2).toHex() + scriptPubKey;
    }

    public static Comparator<TransactionOutput> reverseValueCompare() {
        return (TransactionOutput coin1, TransactionOutput coin2) -> Long.compare(coin1.value, coin2.value);
    }

    public static Comparator<TransactionOutput> valueCompare() {
        return (TransactionOutput coin1, TransactionOutput coin2) -> Long.compare(coin2.value, coin1.value);
    }

    public static Comparator<TransactionOutput> typeCompare() {
        return (TransactionOutput coin1, TransactionOutput coin2) -> {
            AddressType addrType1 = coin1.getAddress().getType();
            AddressType addrType2 = coin2.getAddress().getType();
            if (addrType1 == addrType2) {
                return 0;
            } else if (addrType2 == AddressType.BECH32) {
                return 1;
            } else if (addrType2 == AddressType.SEGWIT) {
                if (addrType1 == AddressType.LEGACY) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        };
    }

    public static TransactionOutput fromHex(String hex) {
        int offset = 0;
        long value = ByteUtil.hex2long(hex.substring(offset, offset += 16), true);
        VarInt scriptLength = new VarInt(hex.substring(offset));
        offset += scriptLength.hexLength();
        String scriptPubKey = hex.substring(offset, offset + scriptLength.toInt() * 2);
        return new TransactionOutput(value, scriptPubKey);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof TransactionOutput) {
            TransactionOutput other = (TransactionOutput) o;
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
