

package com.google.common.hash;

import com.google.common.annotations.Beta;

@Beta
public interface HashFunction {

    Hasher newHasher();


    HashCode hashBytes(byte[] input, int off, int len);

}
