

package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.BitSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;


@Beta // Possibly change from chars to code points; decide constants vs. methods
@GwtCompatible(emulated = true)
public abstract class CharMatcher implements Predicate<Character> {

    // Constant matcher factory methods


    public static CharMatcher none() {
        return None.INSTANCE;
    }


    public static CharMatcher whitespace() {
        return Whitespace.INSTANCE;
    }


    public static CharMatcher ascii() {
        return Ascii.INSTANCE;
    }


    public static CharMatcher invisible() {
        return Invisible.INSTANCE;
    }


    // Legacy constants
    // TODO(cgdecker): Deprecate these so they can be removed eventually


    public static final CharMatcher WHITESPACE = whitespace();


    public static final CharMatcher ASCII = ascii();


    public static final CharMatcher INVISIBLE = invisible();


    public static final CharMatcher NONE = none();

    // Static factories


    public static CharMatcher is(final char match) {
        return new Is(match);
    }


    public static CharMatcher anyOf(final CharSequence sequence) {
        switch (sequence.length()) {
            case 0:
                return none();
            case 1:
                return is(sequence.charAt(0));
            case 2:
                return isEither(sequence.charAt(0), sequence.charAt(1));
            default:
                // TODO(lowasser): is it potentially worth just going ahead and building a precomputed
                // matcher?
                return new AnyOf(sequence);
        }
    }


    // Constructors


    protected CharMatcher() {
    }

    // Abstract methods


    public abstract boolean matches(char c);

    // Non-static factories


    public CharMatcher or(CharMatcher other) {
        return new Or(this, other);
    }


    public CharMatcher precomputed() {
        return Platform.precomputeCharMatcher(this);
    }

    private static final int DISTINCT_CHARS = Character.MAX_VALUE - Character.MIN_VALUE + 1;


    @GwtIncompatible("java.util.BitSet")
    CharMatcher precomputedInternal() {
        final BitSet table = new BitSet();
        setBits(table);
        int totalCharacters = table.cardinality();
        if (totalCharacters * 2 <= DISTINCT_CHARS) {
            return precomputedPositive(totalCharacters, table, toString());
        } else {
            // TODO(lowasser): is it worth it to worry about the last character of large matchers?
            table.flip(Character.MIN_VALUE, Character.MAX_VALUE + 1);
            int negatedCharacters = DISTINCT_CHARS - totalCharacters;
            String suffix = ".negate()";
            final String description = toString();
            String negatedDescription =
                    description.endsWith(suffix)
                            ? description.substring(0, description.length() - suffix.length())
                            : description + suffix;
            return new NegatedFastMatcher(
                    precomputedPositive(negatedCharacters, table, negatedDescription)) {
                @Override
                public String toString() {
                    return description;
                }
            };
        }
    }


    @GwtIncompatible("java.util.BitSet")
    private static CharMatcher precomputedPositive(
            int totalCharacters, BitSet table, String description) {
        switch (totalCharacters) {
            case 0:
                return none();
            case 1:
                return is((char) table.nextSetBit(0));
            case 2:
                char c1 = (char) table.nextSetBit(0);
                char c2 = (char) table.nextSetBit(c1 + 1);
                return isEither(c1, c2);
            default:
                return isSmall(totalCharacters, table.length())
                        ? SmallCharMatcher.from(table, description)
                        : new BitSetMatcher(table, description);
        }
    }

    @GwtIncompatible("SmallCharMatcher")
    private static boolean isSmall(int totalCharacters, int tableLength) {
        return totalCharacters <= SmallCharMatcher.MAX_SIZE
                && tableLength > (totalCharacters * 4 * Character.SIZE);
        // err on the side of BitSetMatcher
    }


    @GwtIncompatible("java.util.BitSet")
    void setBits(BitSet table) {
        for (int c = Character.MAX_VALUE; c >= Character.MIN_VALUE; c--) {
            if (matches((char) c)) {
                table.set(c);
            }
        }
    }

    // Text processing routines


    public boolean matchesAllOf(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (!matches(sequence.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    public boolean matchesNoneOf(CharSequence sequence) {
        return indexIn(sequence) == -1;
    }


    public int indexIn(CharSequence sequence) {
        return indexIn(sequence, 0);
    }


    public int indexIn(CharSequence sequence, int start) {
        int length = sequence.length();
        checkPositionIndex(start, length);
        for (int i = start; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }


    public String removeFrom(CharSequence sequence) {
        String string = sequence.toString();
        int pos = indexIn(string);
        if (pos == -1) {
            return string;
        }

        char[] chars = string.toCharArray();
        int spread = 1;

        // This unusual loop comes from extensive benchmarking
        OUT:
        while (true) {
            pos++;
            while (true) {
                if (pos == chars.length) {
                    break OUT;
                }
                if (matches(chars[pos])) {
                    break;
                }
                chars[pos - spread] = chars[pos];
                pos++;
            }
            spread++;
        }
        return new String(chars, 0, pos - spread);
    }


    public String trimTrailingFrom(CharSequence sequence) {
        int len = sequence.length();
        for (int last = len - 1; last >= 0; last--) {
            if (!matches(sequence.charAt(last))) {
                return sequence.subSequence(0, last + 1).toString();
            }
        }
        return "";
    }


    @Deprecated
    @Override
    public boolean apply(Character character) {
        return matches(character);
    }


    @Override
    public String toString() {
        return super.toString();
    }


    private static String showCharacter(char c) {
        String hex = "0123456789ABCDEF";
        char[] tmp = {'\\', 'u', '\0', '\0', '\0', '\0'};
        for (int i = 0; i < 4; i++) {
            tmp[5 - i] = hex.charAt(c & 0xF);
            c = (char) (c >> 4);
        }
        return String.copyValueOf(tmp);
    }

    // Fast matchers


    abstract static class FastMatcher extends CharMatcher {

        @Override
        public final CharMatcher precomputed() {
            return this;
        }

    }


    abstract static class NamedFastMatcher extends FastMatcher {

        private final String description;

        NamedFastMatcher(String description) {
            this.description = checkNotNull(description);
        }

        @Override
        public final String toString() {
            return description;
        }
    }


    static class NegatedFastMatcher extends Negated {

        NegatedFastMatcher(CharMatcher original) {
            super(original);
        }

        @Override
        public final CharMatcher precomputed() {
            return this;
        }
    }


    @GwtIncompatible("java.util.BitSet")
    private static final class BitSetMatcher extends NamedFastMatcher {

        private final BitSet table;

        private BitSetMatcher(BitSet table, String description) {
            super(description);
            if (table.length() + Long.SIZE < table.size()) {
                table = (BitSet) table.clone();
                // If only we could actually call BitSet.trimToSize() ourselves...
            }
            this.table = table;
        }

        @Override
        public boolean matches(char c) {
            return table.get(c);
        }

        @Override
        void setBits(BitSet bitSet) {
            bitSet.or(table);
        }
    }

    // Static constant implementation classes


    private static final class None extends NamedFastMatcher {

        static final None INSTANCE = new None();

        private None() {
            super("CharMatcher.none()");
        }

        @Override
        public boolean matches(char c) {
            return false;
        }

        @Override
        public int indexIn(CharSequence sequence) {
            checkNotNull(sequence);
            return -1;
        }

        @Override
        public int indexIn(CharSequence sequence, int start) {
            int length = sequence.length();
            checkPositionIndex(start, length);
            return -1;
        }

        @Override
        public boolean matchesAllOf(CharSequence sequence) {
            return sequence.length() == 0;
        }

        @Override
        public boolean matchesNoneOf(CharSequence sequence) {
            checkNotNull(sequence);
            return true;
        }

        @Override
        public String removeFrom(CharSequence sequence) {
            return sequence.toString();
        }

        @Override
        public String trimTrailingFrom(CharSequence sequence) {
            return sequence.toString();
        }

        @Override
        public CharMatcher or(CharMatcher other) {
            return checkNotNull(other);
        }

    }


    @VisibleForTesting
    static final class Whitespace extends NamedFastMatcher {

        static final String TABLE =
                "\u2002\u3000\r\u0085\u200A\u2005\u2000\u3000"
                        + "\u2029\u000B\u3000\u2008\u2003\u205F\u3000\u1680"
                        + "\u0009\u0020\u2006\u2001\u202F\u00A0\u000C\u2009"
                        + "\u3000\u2004\u3000\u3000\u2028\n\u2007\u3000";
        static final int MULTIPLIER = 1682554634;
        static final int SHIFT = Integer.numberOfLeadingZeros(TABLE.length() - 1);

        static final Whitespace INSTANCE = new Whitespace();

        Whitespace() {
            super("CharMatcher.whitespace()");
        }

        @Override
        public boolean matches(char c) {
            return TABLE.charAt((MULTIPLIER * c) >>> SHIFT) == c;
        }

        @GwtIncompatible("java.util.BitSet")
        @Override
        void setBits(BitSet table) {
            for (int i = 0; i < TABLE.length(); i++) {
                table.set(TABLE.charAt(i));
            }
        }
    }


    private static final class Ascii extends NamedFastMatcher {

        static final Ascii INSTANCE = new Ascii();

        Ascii() {
            super("CharMatcher.ascii()");
        }

        @Override
        public boolean matches(char c) {
            return c <= '\u007f';
        }
    }


    private static class RangesMatcher extends CharMatcher {

        private final String description;
        private final char[] rangeStarts;
        private final char[] rangeEnds;

        RangesMatcher(String description, char[] rangeStarts, char[] rangeEnds) {
            this.description = description;
            this.rangeStarts = rangeStarts;
            this.rangeEnds = rangeEnds;
            checkArgument(rangeStarts.length == rangeEnds.length);
            for (int i = 0; i < rangeStarts.length; i++) {
                checkArgument(rangeStarts[i] <= rangeEnds[i]);
                if (i + 1 < rangeStarts.length) {
                    checkArgument(rangeEnds[i] < rangeStarts[i + 1]);
                }
            }
        }

        @Override
        public boolean matches(char c) {
            int index = Arrays.binarySearch(rangeStarts, c);
            if (index >= 0) {
                return true;
            } else {
                index = ~index - 1;
                return index >= 0 && c <= rangeEnds[index];
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }


    private static final class Invisible extends RangesMatcher {

        private static final String RANGE_STARTS =
                "\u0000\u007f\u00ad\u0600\u061c\u06dd\u070f\u1680\u180e\u2000\u2028\u205f\u2066\u2067"
                        + "\u2068\u2069\u206a\u3000\ud800\ufeff\ufff9\ufffa";
        private static final String RANGE_ENDS =
                "\u0020\u00a0\u00ad\u0604\u061c\u06dd\u070f\u1680\u180e\u200f\u202f\u2064\u2066\u2067"
                        + "\u2068\u2069\u206f\u3000\uf8ff\ufeff\ufff9\ufffb";

        static final Invisible INSTANCE = new Invisible();

        private Invisible() {
            super(
                    "CharMatcher.invisible()",
                    RANGE_STARTS.toCharArray(),
                    RANGE_ENDS.toCharArray());
        }
    }


    // Non-static factory implementation classes


    private static class Negated extends CharMatcher {

        final CharMatcher original;

        Negated(CharMatcher original) {
            this.original = checkNotNull(original);
        }

        @Override
        public boolean matches(char c) {
            return !original.matches(c);
        }

        @Override
        public boolean matchesAllOf(CharSequence sequence) {
            return original.matchesNoneOf(sequence);
        }

        @Override
        public boolean matchesNoneOf(CharSequence sequence) {
            return original.matchesAllOf(sequence);
        }

        @GwtIncompatible("java.util.BitSet")
        @Override
        void setBits(BitSet table) {
            BitSet tmp = new BitSet();
            original.setBits(tmp);
            tmp.flip(Character.MIN_VALUE, Character.MAX_VALUE + 1);
            table.or(tmp);
        }

        @Override
        public String toString() {
            return original + ".negate()";
        }
    }


    private static final class Or extends CharMatcher {

        final CharMatcher first;
        final CharMatcher second;

        Or(CharMatcher a, CharMatcher b) {
            first = checkNotNull(a);
            second = checkNotNull(b);
        }

        @GwtIncompatible("java.util.BitSet")
        @Override
        void setBits(BitSet table) {
            first.setBits(table);
            second.setBits(table);
        }

        @Override
        public boolean matches(char c) {
            return first.matches(c) || second.matches(c);
        }

        @Override
        public String toString() {
            return "CharMatcher.or(" + first + ", " + second + ")";
        }
    }

    // Static factory implementations


    private static final class Is extends FastMatcher {

        private final char match;

        Is(char match) {
            this.match = match;
        }

        @Override
        public boolean matches(char c) {
            return c == match;
        }

        @Override
        public CharMatcher or(CharMatcher other) {
            return other.matches(match) ? other : super.or(other);
        }

        @GwtIncompatible("java.util.BitSet")
        @Override
        void setBits(BitSet table) {
            table.set(match);
        }

        @Override
        public String toString() {
            return "CharMatcher.is('" + showCharacter(match) + "')";
        }
    }


    private static CharMatcher.IsEither isEither(char c1, char c2) {
        return new CharMatcher.IsEither(c1, c2);
    }


    private static final class IsEither extends FastMatcher {

        private final char match1;
        private final char match2;

        IsEither(char match1, char match2) {
            this.match1 = match1;
            this.match2 = match2;
        }

        @Override
        public boolean matches(char c) {
            return c == match1 || c == match2;
        }

        @GwtIncompatible("java.util.BitSet")
        @Override
        void setBits(BitSet table) {
            table.set(match1);
            table.set(match2);
        }

        @Override
        public String toString() {
            return "CharMatcher.anyOf(\"" + showCharacter(match1) + showCharacter(match2) + "\")";
        }
    }


    private static final class AnyOf extends CharMatcher {

        private final char[] chars;

        public AnyOf(CharSequence chars) {
            this.chars = chars.toString().toCharArray();
            Arrays.sort(this.chars);
        }

        @Override
        public boolean matches(char c) {
            return Arrays.binarySearch(chars, c) >= 0;
        }

        @Override
        @GwtIncompatible("java.util.BitSet")
        void setBits(BitSet table) {
            for (char c : chars) {
                table.set(c);
            }
        }

        @Override
        public String toString() {
            StringBuilder description = new StringBuilder("CharMatcher.anyOf(\"");
            for (char c : chars) {
                description.append(showCharacter(c));
            }
            description.append("\")");
            return description.toString();
        }
    }


}
