package bpswallet.ser;

import java.util.Objects;

public abstract class Address {

    private AddressType type;
    private boolean used;

    public Address(AddressType type) {
        this.type = type;
        used = false;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public void setType(AddressType type) {
        this.type = type;
    }

    public boolean isUsed() {
        return used;
    }

    public abstract String getScriptPubKey();

    public abstract String getEncoded();
    
    public AddressType getType() {
        return type;
    }
    
    public String getHex() {
        return (used ? "01" : "00") + type.HEX_ID 
                + new VarInt(this.getScriptPubKey().length()/2).toHex()
                + this.getScriptPubKey();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof Address) {
            Address other = (Address) o;
            return this.getScriptPubKey().equalsIgnoreCase(other.getScriptPubKey());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getScriptPubKey());
    }
}
