

package com.google.common.io;

import com.google.common.annotations.Beta;

import java.io.IOException;


@Beta
public interface ByteProcessor<T> {

    boolean processBytes(byte[] buf, int off, int len) throws IOException;


    T getResult();
}
