

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible(emulated = true)
final class Platform {
    private Platform() {
    }


    static long systemNanoTime() {
        return System.nanoTime();
    }

    static CharMatcher precomputeCharMatcher(CharMatcher matcher) {
        return matcher.precomputedInternal();
    }

}
