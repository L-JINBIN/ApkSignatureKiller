

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible(emulated = true)
public final class Splitter {
    private final CharMatcher trimmer;
    private final boolean omitEmptyStrings;
    private final Strategy strategy;
    private final int limit;

    private Splitter(Strategy strategy) {
        this(strategy, false, CharMatcher.NONE, Integer.MAX_VALUE);
    }

    private Splitter(Strategy strategy, boolean omitEmptyStrings, CharMatcher trimmer, int limit) {
        this.strategy = strategy;
        this.omitEmptyStrings = omitEmptyStrings;
        this.trimmer = trimmer;
        this.limit = limit;
    }


    public static Splitter on(char separator) {
        return on(CharMatcher.is(separator));
    }


    public static Splitter on(final CharMatcher separatorMatcher) {
        checkNotNull(separatorMatcher);

        return new Splitter(
                new Strategy() {
                    @Override
                    public SplittingIterator iterator(Splitter splitter, final CharSequence toSplit) {
                        return new SplittingIterator(splitter, toSplit) {
                            @Override
                            int separatorStart(int start) {
                                return separatorMatcher.indexIn(toSplit, start);
                            }

                            @Override
                            int separatorEnd(int separatorPosition) {
                                return separatorPosition + 1;
                            }
                        };
                    }
                });
    }


    @GwtIncompatible("java.util.regex")
    public static Splitter on(final Pattern separatorPattern) {
        checkNotNull(separatorPattern);
        checkArgument(
                !separatorPattern.matcher("").matches(),
                "The pattern may not match the empty string: %s",
                separatorPattern);

        return new Splitter(
                new Strategy() {
                    @Override
                    public SplittingIterator iterator(final Splitter splitter, CharSequence toSplit) {
                        final Matcher matcher = separatorPattern.matcher(toSplit);
                        return new SplittingIterator(splitter, toSplit) {
                            @Override
                            public int separatorStart(int start) {
                                return matcher.find(start) ? matcher.start() : -1;
                            }

                            @Override
                            public int separatorEnd(int separatorPosition) {
                                return matcher.end();
                            }
                        };
                    }
                });
    }


    public Splitter trimResults() {
        return trimResults(CharMatcher.WHITESPACE);
    }


    // TODO(kevinb): throw if a trimmer was already specified!

    public Splitter trimResults(CharMatcher trimmer) {
        checkNotNull(trimmer);
        return new Splitter(strategy, omitEmptyStrings, trimmer, limit);
    }


    public Iterable<String> split(final CharSequence sequence) {
        checkNotNull(sequence);

        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return splittingIterator(sequence);
            }

            @Override
            public String toString() {
                return Joiner.on(", ")
                        .appendTo(new StringBuilder().append('['), this)
                        .append(']')
                        .toString();
            }
        };
    }

    private Iterator<String> splittingIterator(CharSequence sequence) {
        return strategy.iterator(this, sequence);
    }


    private interface Strategy {
        Iterator<String> iterator(Splitter splitter, CharSequence toSplit);
    }

    private abstract static class SplittingIterator extends AbstractIterator<String> {
        final CharSequence toSplit;
        final CharMatcher trimmer;
        final boolean omitEmptyStrings;


        abstract int separatorStart(int start);


        abstract int separatorEnd(int separatorPosition);

        int offset = 0;
        int limit;

        protected SplittingIterator(Splitter splitter, CharSequence toSplit) {
            this.trimmer = splitter.trimmer;
            this.omitEmptyStrings = splitter.omitEmptyStrings;
            this.limit = splitter.limit;
            this.toSplit = toSplit;
        }

        @Override
        protected String computeNext() {

            int nextStart = offset;
            while (offset != -1) {
                int start = nextStart;
                int end;

                int separatorPosition = separatorStart(offset);
                if (separatorPosition == -1) {
                    end = toSplit.length();
                    offset = -1;
                } else {
                    end = separatorPosition;
                    offset = separatorEnd(separatorPosition);
                }
                if (offset == nextStart) {

                    offset++;
                    if (offset >= toSplit.length()) {
                        offset = -1;
                    }
                    continue;
                }

                while (start < end && trimmer.matches(toSplit.charAt(start))) {
                    start++;
                }
                while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
                    end--;
                }

                if (omitEmptyStrings && start == end) {
                    // Don't include the (unused) separator in next split string.
                    nextStart = offset;
                    continue;
                }

                if (limit == 1) {
                    // The limit has been reached, return the rest of the string as the
                    // final item.  This is tested after empty string removal so that
                    // empty strings do not count towards the limit.
                    end = toSplit.length();
                    offset = -1;
                    // Since we may have changed the end, we need to trim it again.
                    while (end > start && trimmer.matches(toSplit.charAt(end - 1))) {
                        end--;
                    }
                } else {
                    limit--;
                }

                return toSplit.subSequence(start, end).toString();
            }
            return endOfData();
        }
    }
}
