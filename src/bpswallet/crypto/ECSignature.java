package bpswallet.crypto;

import java.math.BigInteger;
import java.util.Arrays;
import bpswallet.ser.VarInt;
import bpswallet.util.ByteUtil;

public class ECSignature {

    private String signature;
    private String r, s;

    public ECSignature(BigInteger r, BigInteger s) {
        this.r = String.format("%064x", r);
        this.s = String.format("%064x", s);
        if (Byte.toUnsignedInt(ByteUtil.hex2bytes(this.r)[0]) > 0x7F) {
            this.r = "00" + this.r;
        }
        if (Byte.toUnsignedInt(ByteUtil.hex2bytes(this.s)[0]) > 0x7F) {
            this.s = "00" + this.s;
        }
        int sequenceLen = (4 + this.r.length() / 2 + this.s.length() / 2);
        signature = "30" + new VarInt(sequenceLen).toHex()
                + "02" + new VarInt(this.r.length() / 2).toHex() + this.r
                + "02" + new VarInt(this.s.length() / 2).toHex() + this.s;
    }

    public ECSignature(byte[] rawSig) {
        int rLen = (int) rawSig[3];
        int sLen = (int) rawSig[3 + rLen + 2];
        r = ByteUtil.hexify(Arrays.copyOfRange(rawSig, 4, 4 + rLen));
        s = ByteUtil.hexify(Arrays.copyOfRange(rawSig, 6 + rLen, 6 + rLen + sLen));
        signature = ByteUtil.hexify(rawSig);
    }

    public ECSignature(String rawHex) {
        this(ByteUtil.hex2bytes(rawHex));
    }

    public BigInteger getR() {
        return new BigInteger(1, ByteUtil.hex2bytes(r));
    }

    public BigInteger getS() {
        return new BigInteger(1, ByteUtil.hex2bytes(s));
    }

    public String getHexR() {
        return r;
    }

    public String getHexS() {
        return s;
    }

    public String getSignature() {
        return signature;
    }

    public String getHex() {
        return this.getSignature();
    }
}
