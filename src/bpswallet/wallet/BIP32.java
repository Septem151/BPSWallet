package bpswallet.wallet;

import bpswallet.ser.AddressType;
import bpswallet.crypto.CurveParams;
import bpswallet.crypto.ECPrvKey;
import bpswallet.crypto.ECPubKey;
import bpswallet.crypto.ScalarMultiply;
import java.math.BigInteger;
import bpswallet.util.ByteUtil;
import bpswallet.util.HashUtil;

public class BIP32 {

    public static ExtendedPrvKey deriveXprv(AddressType addrType, String seed, String derivation) {
        ExtendedPrvKey xprv = BIP39.generateMasterKey(addrType, seed);
        String[] levels = derivation.split("/");
        for (int i = 1; i < levels.length; i++) {
            boolean hardened = false;
            int index = -1;
            try {
                if (levels[i].contains("'")) {
                    index = Integer.parseInt(levels[i].substring(0, levels[i].length() - 1));
                    hardened = true;
                } else {
                    index = Integer.parseInt(levels[i]);
                }
            } catch (NumberFormatException ex) {
                System.out.println("Invalid Derivation Path, unable to proceed.");
                System.exit(1);
            }
            xprv = CKDpriv(xprv, index, hardened);
        }
        return xprv;
    }

    public static ExtendedPubKey deriveXpub(AddressType addrType, String seed, String derivation) {
        return neuter(deriveXprv(addrType, seed, derivation));
    }

    public static ExtendedPrvKey CKDpriv(ExtendedPrvKey xkeyPar, int childNum, boolean hardened) {
        childNum = (hardened) ? 0x80000000 | childNum : childNum;
        String data = ((hardened) ? "00" + xkeyPar.getKey().getEncoded() : ((ECPrvKey) xkeyPar.getKey()).getPubKey().getEncoded())
                + ByteUtil.int2hex(childNum, false);
        String I = HashUtil.hmac(xkeyPar.getChaincode(), data);
        BigInteger I_L = new BigInteger(1, ByteUtil.hex2bytes(I.substring(0, I.length() / 2)));
        String I_R = I.substring(I.length() / 2);
        BigInteger parSecret = ((ECPrvKey) xkeyPar.getKey()).getSecret();
        ECPrvKey prvKey = new ECPrvKey(I_L.add(parSecret).mod(CurveParams.n));
        return new ExtendedPrvKey(xkeyPar, childNum, prvKey, I_R);
    }

    public static ExtendedPubKey CKDpub(ExtendedPubKey xkeyPar, int childNum) {
        String I = HashUtil.hmac(xkeyPar.getChaincode(), ((ECPubKey) xkeyPar.getKey()).getEncoded() + ByteUtil.int2hex(childNum, false));
        ECPubKey I_L = new ECPrvKey(I.substring(0, I.length() / 2)).getPubKey();
        String I_R = I.substring(I.length() / 2);
        ECPubKey pubKeyPar = (ECPubKey) xkeyPar.getKey();
        ECPubKey pubKey = new ECPubKey(ScalarMultiply.addPoint(I_L.getPoint(), pubKeyPar.getPoint()));
        return new ExtendedPubKey(xkeyPar, childNum, pubKey, I_R);
    }

    public static ExtendedPubKey NCKDpriv(ExtendedPrvKey xkeyPar, int childNum, boolean hardened) {
        if (hardened) {
            ExtendedPrvKey xkey = CKDpriv(xkeyPar, childNum, hardened);
            return neuter(xkey);
        } else {
            ExtendedPubKey xkey = neuter(xkeyPar);
            return CKDpub(xkey, childNum);
        }
    }

    public static ExtendedPubKey neuter(ExtendedPrvKey xkeyPar) {
        ECPubKey pubKey = ((ECPrvKey) xkeyPar.getKey()).getPubKey();
        return new ExtendedPubKey(xkeyPar.getType(), xkeyPar.getDepth(), xkeyPar.getFingerprint(), xkeyPar.getChildNum(), xkeyPar.getChaincode(), pubKey);
    }

    public static boolean testDerivation(String derivation) {
        if (derivation.isEmpty()) {
            return false;
        }
        String[] depths = derivation.split("/");
        if (!depths[0].equalsIgnoreCase("m")) {
            return false;
        }
        for (int i = 1; i < depths.length; i++) {
            String depth = depths[i];
            if (depth.isEmpty()) {
                return false;
            }
            boolean hardened = depth.charAt(depth.length() - 1) == '\'';
            if (hardened) {
                depth = depth.substring(0, depth.length() - 1);
            }
            try {
                Integer.parseInt(depth);
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }
}
