package bpswallet.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;

public final class ByteUtil {

    public static boolean isHex(String str) {
        return !str.isEmpty() && str.matches("-?[0-9a-fA-F]+");
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String hexify(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hex2bytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String int2hex(int num, boolean littleEndian) {
        return hexify(int2bytes(num, littleEndian));
    }

    public static int hex2int(String num, boolean littleEndian) {
        return bytes2int(hex2bytes(num), littleEndian);
    }

    public static byte[] int2bytes(int num, boolean littleEndian) {
        ByteOrder order = (littleEndian) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer buffer = ByteBuffer.allocate(4).order(order).putInt(num);
        return buffer.array();
    }

    public static int bytes2int(byte[] num, boolean littleEndian) {
        byte[] numCopy = Arrays.copyOfRange(num, 0, 4);
        if (littleEndian) {
            numCopy = flipendian(numCopy);
        }
        return ByteBuffer.wrap(numCopy).getInt();
    }

    public static String long2hex(long num, boolean littleEndian) {
        return hexify(long2bytes(num, littleEndian));
    }

    public static long hex2long(String num, boolean littleEndian) {
        return bytes2long(hex2bytes(num), littleEndian);
    }

    public static byte[] long2bytes(long num, boolean littleEndian) {
        ByteOrder order = (littleEndian) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(order).putLong(num);
        return buffer.array();
    }

    public static long bytes2long(byte[] num, boolean littleEndian) {
        byte[] numCopy = Arrays.copyOfRange(num, 0, 8);
        if (littleEndian) {
            numCopy = flipendian(numCopy);
        }
        return ByteBuffer.wrap(numCopy).getLong();
    }

    public static byte[] flipendian(byte[] bytes) {
        byte[] rev = Arrays.copyOf(bytes, bytes.length);
        for (int i = 0; i < rev.length / 2; i++) {
            byte temp = rev[i];
            rev[i] = rev[rev.length - i - 1];
            rev[rev.length - i - 1] = temp;
        }
        return rev;
    }

    public static String flipendian(String hex) {
        return hexify(flipendian(hex2bytes(hex)));
    }

    public static byte[] randBytes(int byteLength) {
        byte[] bytes = new byte[byteLength];
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.nextBytes(bytes);
            return bytes;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String randHex(int byteLength) {
        return hexify(randBytes(byteLength));
    }

    public static boolean[] int2bits(int num) {
        return bytes2bits(int2bytes(num, false));
    }

    public static int bits2int(boolean[] bits) {
        int n = 0, l = bits.length;
        for (int i = 0; i < l; ++i) {
            n = (n << 1) + (bits[i] ? 1 : 0);
        }
        return n;
    }

    public static boolean[] int2bip39bits(int num) {
        boolean[] bits = int2bits(num);
        return Arrays.copyOfRange(bits, bits.length - 11, bits.length);
    }

    public static boolean[] bytes2bits(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                bits[(i * 8) + j] = (bytes[i] & (1 << (7 - j))) != 0;
            }
        }
        return bits;
    }

    public static byte[] bits2bytes(boolean[] bits) {
        boolean[] bitsCopy = bits;
        if (bits.length % 8 != 0) {
            bitsCopy = new boolean[(bits.length / 8 + 1) * 8];
            System.arraycopy(bits, 0, bitsCopy, bitsCopy.length - bits.length, bits.length);
        }
        byte[] toReturn = new byte[bitsCopy.length / 8];
        for (int entry = 0; entry < toReturn.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (bitsCopy[entry * 8 + bit]) {
                    toReturn[entry] |= (128 >> bit);
                }
            }
        }
        return toReturn;
    }

    public static boolean[] hex2bits(String hex) {
        return bytes2bits(hex2bytes(hex));
    }

    public static String bits2hex(boolean[] bits) {
        return hexify(bits2bytes(bits));
    }

}
