package bpswallet.crypto;

import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import bpswallet.util.ByteUtil;

public class ECKeyPair {
    private final ECPrvKey prvKey;
    private final ECPubKey pubKey;
    
    public static ECKeyPair randomKeyPair() {
      try {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] pk = new byte[32];
        sr.nextBytes(pk);
        return new ECKeyPair(ByteUtil.hexify(pk));
      } catch (NoSuchAlgorithmException ex) {
        throw new RuntimeException(ex);
      }
    }

    public ECKeyPair(String prvKeyEncoded) {
        this(new ECPrvKey(prvKeyEncoded));
    }
    
    public ECKeyPair(ECPrvKey prvKey) {
        this.prvKey = prvKey;
        pubKey = setPubKey();
    }

    private ECPubKey setPubKey() {
        try {
            ECPoint W = ScalarMultiply.scalmult(CurveParams.G, prvKey.getSecret());
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(W, CurveParams.ecSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            ECPublicKey K = (ECPublicKey)keyFactory.generatePublic(pubKeySpec);
            return new ECPubKey(K);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public ECPrvKey getPrv() {
        return prvKey;
    }
    
    public ECPubKey getPub() {
        return pubKey;
    }
}