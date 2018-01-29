package org.sputnikdev.bluetooth.miot;

public class BLECipher {

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private static native int nativeEncrypt(byte[] bArr, byte[] bArr2, byte[] bArr3);

    private static native int nativeMixA(byte[] bArr, byte[] bArr2, byte[] bArr3);

    private static native int nativeMixB(byte[] bArr, byte[] bArr2, byte[] bArr3);

    static {
        System.loadLibrary("blecipher");
    }

    public static byte[] encrypt(byte[] bArr, byte[] bArr2) {
        if (isEmpty(bArr) || isEmpty(bArr2)) {
            return EMPTY_ARRAY;
        }
        byte[] bArr3 = new byte[bArr2.length];
        if (nativeEncrypt(bArr, bArr2, bArr3) != 0) {
            return EMPTY_ARRAY;
        }
        return bArr3;
    }

    public static byte[] mixA(String str, int i) {
        if (isEmpty(str) || i < 0) {
            return EMPTY_ARRAY;
        }
        byte[] bArr = new byte[8];
        if (nativeMixA(mac2Bytes(str), pid2Bytes(i), bArr) != 0) {
            return EMPTY_ARRAY;
        }
        return bArr;
    }

    public static byte[] mixB(String str, int i) {
        if (isEmpty(str) || i < 0) {
            return EMPTY_ARRAY;
        }
        byte[] bArr = new byte[8];
        if (nativeMixB(mac2Bytes(str), pid2Bytes(i), bArr) != 0) {
            return EMPTY_ARRAY;
        }
        return bArr;
    }

    private static byte[] mac2Bytes(String str) {
        String[] split = str.split(":");
        int length = split.length;
        byte[] bArr = new byte[length];
        for (int i = 0; i < length; i++) {
            bArr[(length - i) - 1] = int2Byte(Integer.parseInt(split[i], 16));
        }
        return bArr;
    }

    private static byte[] pid2Bytes(int i) {
        return new byte[]{int2Byte(i), int2Byte(i >>> 8)};
    }

    private static byte int2Byte(int i) {
        return (byte) (i & 255);
    }

    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }
}
