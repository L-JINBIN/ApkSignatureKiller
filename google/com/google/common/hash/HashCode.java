

package com.google.common.hash;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;


@Beta
public abstract class HashCode {
    HashCode() {
    }


    public abstract int bits();


    public abstract int asInt();


    // TODO(user): consider ByteString here, when that is available

    public abstract byte[] asBytes();


    byte[] getBytesInternal() {
        return asBytes();
    }


    abstract boolean equalsSameBits(HashCode that);


    @Override
    public final boolean equals(@Nullable Object object) {
        if (object instanceof HashCode) {
            HashCode that = (HashCode) object;
            return bits() == that.bits() && equalsSameBits(that);
        }
        return false;
    }


    @Override
    public final int hashCode() {
        // If we have at least 4 bytes (32 bits), just take the first 4 bytes. Since this is
        // already a (presumably) high-quality hash code, any four bytes of it will do.
        if (bits() >= 32) {
            return asInt();
        }
        // If we have less than 4 bytes, use them all.
        byte[] bytes = getBytesInternal();
        int val = (bytes[0] & 0xFF);
        for (int i = 1; i < bytes.length; i++) {
            val |= ((bytes[i] & 0xFF) << (i * 8));
        }
        return val;
    }

    @Override
    public final String toString() {
        byte[] bytes = getBytesInternal();
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
}
