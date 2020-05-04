package bpswallet.ser;

public enum AddressType {
    LEGACY("1", "0488ade4", "0488b21e", 592, 136, "01"),
    SEGWIT("3", "049d7878", "049d7cb2", 364, 128, "03"),
    BECH32("bc1q", "04b2430c", "04b24746", 272, 124, "04");

    public final String ADDR_PREFIX, XPUB_PREFIX, XPRV_PREFIX, HEX_ID;
    public final int INPUT_WEIGHT, OUTPUT_WEIGHT;

    private AddressType(String addrPrefix, String xprvPrefix, String xpubPrefix, int inputWeight, int outputWeight, String hexId) {
        ADDR_PREFIX = addrPrefix;
        XPRV_PREFIX = xprvPrefix;
        XPUB_PREFIX = xpubPrefix;
        INPUT_WEIGHT = inputWeight;
        OUTPUT_WEIGHT = outputWeight;
        HEX_ID = hexId;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }

    public static AddressType fromXkeyPrefix(String prefix) {
        for (AddressType type : AddressType.values()) {
            if (prefix.equals(type.XPRV_PREFIX) || prefix.equals(type.XPUB_PREFIX)) {
                return type;
            }
        }
        return null;
    }

    public static AddressType fromName(String name) {
        for (AddressType type : AddressType.values()) {
            if(name.equalsIgnoreCase(type.name())) {
                return type;
            }
        }
        return null;
    }

    public static AddressType fromHexId(String hexId) {
        for (AddressType type : AddressType.values()) {
            if (hexId.equals(type.HEX_ID)) {
                return type;
            }
        }
        return null;
    }
}
