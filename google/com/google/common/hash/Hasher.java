

package com.google.common.hash;

import com.google.common.annotations.Beta;

@Beta
public interface Hasher extends PrimitiveSink {
    @Override
    Hasher putByte(byte b);

    @Override
    Hasher putBytes(byte[] bytes);

    @Override
    Hasher putBytes(byte[] bytes, int off, int len);


    HashCode hash();
}
