package bpswallet.crypto;

import bpswallet.ser.Base58Check;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import bpswallet.util.ByteUtil;

public class ECPrvKey implements ECKey {

    private final String encoded;
    private final BigInteger secret;
    private final ECPrivateKey prvKey;

    public ECPrvKey(BigInteger secret) {
        this(String.format("%064x", secret));
    }

    public ECPrvKey(String encoded) {
        this.encoded = encoded;
        secret = new BigInteger(1, ByteUtil.hex2bytes(encoded));
        prvKey = setPrvKey();
    }

    private ECPrivateKey setPrvKey() {
        try {
            ECPrivateKeySpec prvKeySpec = new ECPrivateKeySpec(secret, CurveParams.ecSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            ECPrivateKey k = (ECPrivateKey) keyFactory.generatePrivate(prvKeySpec);
            return k;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ECSignature sign(String message) {
        BigInteger z = new BigInteger(1, ByteUtil.hex2bytes(message));
        BigInteger r = BigInteger.ZERO;
        BigInteger s = BigInteger.ZERO;
        while (r.compareTo(BigInteger.ZERO) == 0
                || s.compareTo(BigInteger.ZERO) == 0) {
            ECKeyPair ephemKeyPair = ECKeyPair.randomKeyPair();
            r = ephemKeyPair.getPub().getPoint().getAffineX().mod(CurveParams.n);
            if (r.compareTo(BigInteger.ZERO) != 0) {
                BigInteger kInv = ephemKeyPair.getPrv().getSecret().modInverse(CurveParams.n);
                s = kInv.multiply(z.add(r.multiply(this.getSecret()))).mod(CurveParams.n);
                if (s.compareTo(CurveParams.n.divide(BigInteger.valueOf(2))) > 0) {
                    s = CurveParams.n.subtract(s);
                }
            }
        }
        return new ECSignature(r, s);
    }

    public ECPubKey getPubKey() {
        return new ECKeyPair(this).getPub();
    }

    @Override
    public String getEncoded() {
        return encoded;
    }

    public BigInteger getSecret() {
        return secret;
    }

    public ECPrivateKey getKey() {
        return prvKey;
    }

    public String getWIF() {
        return Base58Check.bytesToBase58(ByteUtil.hex2bytes("80" + encoded + "01"));
    }
}
