

package com.google.common.hash;

import com.google.common.annotations.Beta;


@Beta
public interface PrimitiveSink {

    PrimitiveSink putByte(byte b);


    PrimitiveSink putBytes(byte[] bytes);


    PrimitiveSink putBytes(byte[] bytes, int off, int len);

}
