package bpswallet.ser;

import bpswallet.wallet.ExtendedKey;
import bpswallet.wallet.ExtendedPrvKey;
import bpswallet.wallet.ExtendedPubKey;
import bpswallet.crypto.ECPrvKey;
import bpswallet.crypto.ECPubKey;

public final class AddressFactory {

    public static Address fromEncoded(String encoded) {
        try {
            if (encoded.startsWith(AddressType.LEGACY.ADDR_PREFIX)) {
                return LegacyAddress.fromEncoded(encoded);
            } else if (encoded.startsWith(AddressType.SEGWIT.ADDR_PREFIX)) {
                return SegwitAddress.fromEncoded(encoded);
            } else if (encoded.startsWith(AddressType.BECH32.ADDR_PREFIX)) {
                return Bech32Address.fromEncoded(encoded);
            } else {
                return null;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Address fromScriptPubKey(String scriptPubKey) {
        try {
            if (scriptPubKey.length() == 50 && scriptPubKey.startsWith("76a914") && scriptPubKey.endsWith("88ac")) {
                return new LegacyAddress(scriptPubKey);
            } else if (scriptPubKey.length() == 46 && scriptPubKey.startsWith("a914") && scriptPubKey.endsWith("87")) {
                return new SegwitAddress(scriptPubKey);
            } else if ((scriptPubKey.length() == 44) && scriptPubKey.startsWith("0014")) {
                return new Bech32Address(scriptPubKey);
            } else {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static Address fromECPubKey(ECPubKey pubKey, AddressType type) {
        try {
            if (null == type) {
                return null;
            } else {
                switch (type) {
                    case LEGACY:
                        return new LegacyAddress(pubKey);
                    case SEGWIT:
                        return new SegwitAddress(pubKey);
                    case BECH32:
                        return new Bech32Address(pubKey);
                    default:
                        return null;
                }
            }
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static Address fromExtendedKey(ExtendedKey xkey) {
        if(xkey instanceof ExtendedPrvKey) {
            ECPubKey pubKey = ((ECPrvKey)xkey.getKey()).getPubKey();
            return fromECPubKey(pubKey, xkey.getType());
        }else if(xkey instanceof ExtendedPubKey) {
            ECPubKey pubKey = (ECPubKey)xkey.getKey();
            return fromECPubKey(pubKey, xkey.getType());
        }else return null;
    }
    
    public static Address fromHex(String hex) {
        try {
            int offset = 0;
            boolean used = hex.substring(offset, offset += 2).equals("01");
            AddressType type = AddressType.fromHexId(hex.substring(offset, offset += 2));
            VarInt scriptLength = new VarInt(hex.substring(offset));
            offset += scriptLength.hexLength();
            String scriptPubKey = hex.substring(offset, offset + scriptLength.toInt()*2);
            Address addr = fromScriptPubKey(scriptPubKey);
            addr.setUsed(used);
            if(addr.getType() != type) {
                return null;
            }
            return addr;
        } catch (Exception ex) {
            return null;
        }
    }
}
