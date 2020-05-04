package bpswallet.txn;

import bpswallet.util.ByteUtil;
import java.util.Objects;

public class Outpoint {

    private final String hash;
    private final int index;

    public Outpoint(String hash, int index) {
        this.hash = hash;
        this.index = index;
    }

    public String getHash() {
        return hash;
    }

    public int getIndex() {
        return index;
    }

    public String getHex() {
        return hash + ByteUtil.int2hex(index, true);
    }

    public static Outpoint fromHex(String hex) {
        String hash = hex.substring(0, 64);
        int index = ByteUtil.hex2int(hex.substring(64, 72), true);
        return new Outpoint(hash, index);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof Outpoint) {
            Outpoint other = (Outpoint) o;
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
