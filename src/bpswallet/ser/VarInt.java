package bpswallet.ser;

import bpswallet.util.ByteUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class VarInt {

    private final long value;
    private final byte[] varInt;

    public VarInt(long num) {
        value = num;
        if (num < 0) {
            varInt = new byte[0];
            return;
        }
        if (num < 0xFD) {
            // Length 1
            varInt = new byte[]{(byte) num};
        } else if (num <= 0xFFFF) {
            // Length 3
            byte[] numBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) num).array();
            varInt = ByteUtil.hex2bytes("FD" + ByteUtil.hexify(numBytes));
        } else if (num <= Long.parseUnsignedLong("FFFFFFFF", 16)) {
            // Length 5
            byte[] numBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) num).array();
            varInt = ByteUtil.hex2bytes("FE" + ByteUtil.hexify(numBytes));
        } else {
            // Length 9
            byte[] numBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(num).array();
            varInt = ByteUtil.hex2bytes("FF" + ByteUtil.hexify(numBytes));
        }
    }

    public VarInt(int num) {
        this((long) num);
    }

    public VarInt(byte[] varInt) {
        this(ByteUtil.hexify(varInt));
    }

    public VarInt(String hexVarInt) {
        String flag = hexVarInt.substring(0, 2);
        int numLen;
        byte[] numBytes;
        if (flag.equalsIgnoreCase("fd")) {
            numLen = 2;
            this.varInt = ByteUtil.hex2bytes(hexVarInt.substring(0, 6));
            numBytes = Arrays.copyOfRange(this.varInt, 1, numLen + 1);
            value = (long) ByteBuffer.wrap(numBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        } else if (flag.equalsIgnoreCase("fe")) {
            numLen = 4;
            this.varInt = ByteUtil.hex2bytes(hexVarInt.substring(0, 8));
            numBytes = Arrays.copyOfRange(this.varInt, 1, numLen + 1);
            value = (long) ByteBuffer.wrap(numBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        } else if (flag.equalsIgnoreCase("ff")) {
            numLen = 8;
            this.varInt = ByteUtil.hex2bytes(hexVarInt.substring(0, 8));
            numBytes = Arrays.copyOfRange(this.varInt, 1, numLen + 1);
            value = (long) ByteBuffer.wrap(numBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        } else {
            this.varInt = ByteUtil.hex2bytes(flag);
            value = Byte.toUnsignedLong(ByteUtil.hex2bytes(flag)[0]);
        }
    }

    public int hexLength() {
        return this.toHex().length();
    }

    public int byteLength() {
        return varInt.length;
    }

    public byte[] toArray() {
        return varInt;
    }

    public String toHex() {
        return ByteUtil.hexify(varInt);
    }

    public long toLong() {
        return value;
    }

    public int toInt() {
        return (int) value;
    }

    @Override
    public String toString() {
        return this.toHex();
    }
}
