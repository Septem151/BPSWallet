package bpswallet.txn;

import bpswallet.ser.VarInt;
import bpswallet.util.ByteUtil;
import java.util.Objects;

public class TransactionInput {

    public static final int DEFAULT_SEQUENCE = 0xffffffff;

    private final Outpoint outpoint;
    private String scriptSig;
    private final int sequence;

    public TransactionInput(Outpoint outpoint, String scriptSig, int sequence) {
        this.outpoint = outpoint;
        this.scriptSig = scriptSig;
        this.sequence = sequence;
    }

    public TransactionInput(Outpoint outpoint) {
        this(outpoint, "", DEFAULT_SEQUENCE);
    }
    
    public static TransactionInput getCoinbaseInput() {
      return new TransactionInput(new Outpoint(ByteUtil.hexify(new byte[32]), -1), ByteUtil.randHex(8), 0);
    }

    public void setScriptSig(String scriptSig) {
        this.scriptSig = scriptSig;
    }

    public String clearScriptSig() {
        String oldScriptSig = scriptSig;
        scriptSig = "";
        return oldScriptSig;
    }

    public Outpoint getOutpoint() {
        return outpoint;
    }

    public String getScriptSig() {
        return scriptSig;
    }

    public int getSequence() {
        return sequence;
    }

    public String getHex() {
        return outpoint.getHex() + new VarInt(scriptSig.length() / 2).toHex() + scriptSig + ByteUtil.int2hex(sequence, true);
    }

    public static TransactionInput fromHex(String hex) {
        int offset = 0;
        Outpoint outpoint = Outpoint.fromHex(hex.substring(offset, offset += 72));
        VarInt scriptLength = new VarInt(hex.substring(offset));
        offset += scriptLength.hexLength();
        String scriptSig = hex.substring(offset, offset += scriptLength.toInt() * 2);
        int sequence = ByteUtil.hex2int(hex.substring(offset, offset + 8), true);
        return new TransactionInput(outpoint, scriptSig, sequence);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof TransactionInput) {
            TransactionInput other = (TransactionInput) o;
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
