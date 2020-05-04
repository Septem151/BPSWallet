package bpswallet.txn;

import bpswallet.ser.VarInt;
import java.util.ArrayList;
import java.util.Objects;

public class WitnessProgram {

    private final ArrayList<String> pushes;

    public WitnessProgram() {
        pushes = new ArrayList<>();
    }

    public void addPush(String data) {
        pushes.add(data);
    }

    public int getNumPushes() {
        return pushes.size();
    }

    public String[] getPushes() {
        return pushes.toArray(new String[pushes.size()]);
    }

    public String getHex() {
        String hex = new VarInt(pushes.size()).toHex();
        for (String data : pushes) {
            hex += (new VarInt(data.length() / 2).toHex() + data);
        }
        return hex;
    }

    public static WitnessProgram fromHex(String hex) {
        WitnessProgram program = new WitnessProgram();
        int offset = 0;
        VarInt numPushes = new VarInt(hex);
        offset += numPushes.hexLength();
        for (int i = 0; i < numPushes.toInt(); i++) {
            VarInt dataLength = new VarInt(hex.substring(offset));
            offset += dataLength.hexLength();
            program.addPush(hex.substring(offset, offset += dataLength.toInt() * 2));
        }
        return program;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof WitnessProgram) {
            WitnessProgram other = (WitnessProgram) o;
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
