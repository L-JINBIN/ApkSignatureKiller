

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;


@GwtCompatible
public final class Ascii {

    private Ascii() {
    }


    public static final char MAX = 127;


    public static String toLowerCase(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (isUpperCase(string.charAt(i))) {
                char[] chars = string.toCharArray();
                for (; i < length; i++) {
                    char c = chars[i];
                    if (isUpperCase(c)) {
                        chars[i] = (char) (c ^ 0x20);
                    }
                }
                return String.valueOf(chars);
            }
        }
        return string;
    }


    public static char toLowerCase(char c) {
        return isUpperCase(c) ? (char) (c ^ 0x20) : c;
    }


    public static char toUpperCase(char c) {
        return isLowerCase(c) ? (char) (c & 0x5f) : c;
    }


    public static boolean isLowerCase(char c) {
        // Note: This was benchmarked against the alternate expression "(char)(c - 'a') < 26" (Nov '13)
        // and found to perform at least as well, or better.
        return (c >= 'a') && (c <= 'z');
    }


    public static boolean isUpperCase(char c) {
        return (c >= 'A') && (c <= 'Z');
    }


}
