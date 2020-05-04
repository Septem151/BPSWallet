package bpswallet.crypto;

//import com.google.gson.JsonDeserializationContext;
//import com.google.gson.JsonDeserializer;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonPrimitive;
//import com.google.gson.JsonSerializationContext;
//import com.google.gson.JsonSerializer;
//import java.lang.reflect.Type;
import java.security.interfaces.ECPublicKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import bpswallet.util.ByteUtil;
import bpswallet.util.HashUtil;

public class ECPubKey implements ECKey {

    private final String encoded;
    private final ECPoint point;
    private final ECPublicKey pubKey;

    public ECPubKey(String encoded) {
        pubKey = setPubKey(encoded);
        point = pubKey.getW();
        if (encoded.startsWith("04")) {
            this.encoded = encode();
        } else {
            this.encoded = encoded;
        }
    }

    public ECPubKey(ECPublicKey pubKey) {
        this.pubKey = pubKey;
        point = pubKey.getW();
        encoded = encode();
    }

    public ECPubKey(ECPoint point) {
        this.point = point;
        try {
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, CurveParams.ecSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            pubKey = (ECPublicKey) keyFactory.generatePublic(pubKeySpec);
            encoded = encode();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ECPublicKey setPubKey(String encodedKey) {
        if (encodedKey.isEmpty()) {
            return null;
        }
        try {
            String decompressedKey;
            if (!encodedKey.startsWith("04")) {
                decompressedKey = decompressPubKey(encodedKey);
            } else {
                decompressedKey = encodedKey;
            }
            BigInteger bi_x = new BigInteger(1, ByteUtil.hex2bytes(decompressedKey.substring(2, 66)));
            BigInteger bi_y = new BigInteger(1, ByteUtil.hex2bytes(decompressedKey.substring(66, 130)));
            ECPoint keyPoint = new ECPoint(bi_x, bi_y);
            ECPublicKeySpec keySpec = new ECPublicKeySpec(keyPoint, CurveParams.ecSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return (ECPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String decompressPubKey(String encodedKey) {
        byte[] keyBytes = ByteUtil.hex2bytes(encodedKey);
        byte[] K_x_bytes = new byte[32];
        System.arraycopy(keyBytes, 1, K_x_bytes, 0, K_x_bytes.length);
        byte parity = keyBytes[0];
        BigInteger K_x = new BigInteger(1, K_x_bytes);
        BigInteger y_square
                = K_x.modPow(BigInteger.valueOf(3), CurveParams.p).add(CurveParams.b).mod(CurveParams.p);
        BigInteger y_root = y_square.modPow(
                CurveParams.p.add(BigInteger.ONE).divide(BigInteger.valueOf(4)),
                CurveParams.p
        );
        BigInteger K_y;
        if (parity == 0x02 && y_root.testBit(0) || parity == 0x03 && !y_root.testBit(0)) {
            K_y = y_root.negate().mod(CurveParams.p);
        } else {
            K_y = y_root;
        }
        String K_uncomp = "04" + String.format("%064x", K_x) + String.format("%064x", K_y);
        return K_uncomp;
    }

    private String encode() {
        BigInteger K_x = pubKey.getW().getAffineX();
        BigInteger K_y = pubKey.getW().getAffineY();
        String parity = (K_y.testBit(0) ? "03" : "02");
        return parity + String.format("%064x", K_x);
    }

    public boolean verify(String sigHash, ECSignature signature) {
        BigInteger z = new BigInteger(1, ByteUtil.hex2bytes(sigHash));
        BigInteger r = signature.getR();
        BigInteger s = signature.getS();
        BigInteger sInv = s.modInverse(CurveParams.n);
        BigInteger u1 = z.multiply(sInv).mod(CurveParams.n);
        BigInteger u2 = r.multiply(sInv).mod(CurveParams.n);
        ECPoint u1Point = ScalarMultiply.scalmult(CurveParams.G, u1);
        ECPoint u2Point = ScalarMultiply.scalmult(this.getPoint(), u2);
        ECPoint sigPoint = ScalarMultiply.addPoint(u1Point, u2Point);
        return r.compareTo(sigPoint.getAffineX().mod(CurveParams.n)) == 0;
    }

    public ECIESData encrypt(boolean passwordProtected, String data, String salt) {
        ECKeyPair ephemKeyPair = ECKeyPair.randomKeyPair();
        ECPubKey encryptionKey = new ECPubKey(ScalarMultiply.scalmult(this.point, ephemKeyPair.getPrv().getSecret()));
        String ciphertext = "";
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey.asSecretKey());
            byte[] dataBytes;
            if (data.matches("-?[0-9a-fA-F]+")) {
                dataBytes = ByteUtil.hex2bytes(data);
            } else {
                dataBytes = data.getBytes(StandardCharsets.UTF_8);
            }
            ciphertext = ByteUtil.hexify(cipher.doFinal(dataBytes));
        } catch (InvalidKeyException | NoSuchAlgorithmException
                | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
            throw new RuntimeException(ex);
        }
        String encryptionKeyHash = HashUtil.doubleSha256(encryptionKey.getEncoded());
        return new ECIESData(passwordProtected, salt, ephemKeyPair.getPub(), encryptionKeyHash, ciphertext);
    }

    public SecretKey asSecretKey() {
        return new SecretKeySpec(ByteUtil.hex2bytes(encoded.substring(2, 66)), "AES");
    }

    public ECPoint getPoint() {
        return point;
    }

    @Override
    public String getEncoded() {
        return encoded;
    }

    public ECPublicKey getKey() {
        return pubKey;
    }

    /*
    public static JsonSerializer<ECPubKey> serializer() {
        return (ECPubKey src, Type srcType, JsonSerializationContext context) -> {
            return new JsonPrimitive(src.encoded);
        };
    }
    
    public static JsonDeserializer<ECPubKey> deserializer() {
        return (JsonElement json, Type type, JsonDeserializationContext context) -> {
            return new ECPubKey(json.getAsJsonPrimitive().getAsString());
        };
    }
     */
}
