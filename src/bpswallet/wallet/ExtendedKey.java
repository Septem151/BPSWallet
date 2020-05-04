package bpswallet.wallet;

import bpswallet.ser.AddressType;
import bpswallet.crypto.ECKey;

public interface ExtendedKey {
	String getId();
	AddressType getType();
	int getDepth();
	String getFingerprint();
	int getChildNum();
	String getChaincode();
	ECKey getKey();
        String getHex();
        String getEncoded();
}