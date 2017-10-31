

package com.google.common.collect;

import android.support.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

import static com.google.common.base.Preconditions.checkNotNull;


@GwtCompatible
@Beta
final class SortedLists {
    private SortedLists() {
    }


    public enum KeyPresentBehavior {

        ANY_PRESENT {
            @Override
            <E> int resultIndex(
                    Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return foundIndex;
            }
        },

        LAST_PRESENT {
            @Override
            <E> int resultIndex(
                    Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                // Of course, we have to use binary search to find the precise
                // breakpoint...
                int lower = foundIndex;
                int upper = list.size() - 1;
                // Everything between lower and upper inclusive compares at >= 0.
                while (lower < upper) {
                    int middle = (lower + upper + 1) >>> 1;
                    int c = comparator.compare(list.get(middle), key);
                    if (c > 0) {
                        upper = middle - 1;
                    } else { // c == 0
                        lower = middle;
                    }
                }
                return lower;
            }
        },

        FIRST_PRESENT {
            @Override
            <E> int resultIndex(
                    Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                // Of course, we have to use binary search to find the precise
                // breakpoint...
                int lower = 0;
                int upper = foundIndex;
                // Of course, we have to use binary search to find the precise breakpoint...
                // Everything between lower and upper inclusive compares at <= 0.
                while (lower < upper) {
                    int middle = (lower + upper) >>> 1;
                    int c = comparator.compare(list.get(middle), key);
                    if (c < 0) {
                        lower = middle + 1;
                    } else { // c == 0
                        upper = middle;
                    }
                }
                return lower;
            }
        },

        FIRST_AFTER {
            @Override
            public <E> int resultIndex(
                    Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return LAST_PRESENT.resultIndex(comparator, key, list, foundIndex) + 1;
            }
        },

        LAST_BEFORE {
            @Override
            public <E> int resultIndex(
                    Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex) {
                return FIRST_PRESENT.resultIndex(comparator, key, list, foundIndex) - 1;
            }
        };

        abstract <E> int resultIndex(
                Comparator<? super E> comparator, E key, List<? extends E> list, int foundIndex);
    }


    public enum KeyAbsentBehavior {

        NEXT_LOWER {
            @Override
            int resultIndex(int higherIndex) {
                return higherIndex - 1;
            }
        },

        NEXT_HIGHER {
            @Override
            public int resultIndex(int higherIndex) {
                return higherIndex;
            }
        },

        INVERTED_INSERTION_INDEX {
            @Override
            public int resultIndex(int higherIndex) {
                return ~higherIndex;
            }
        };

        abstract int resultIndex(int higherIndex);
    }


    public static <E, K extends Comparable> int binarySearch(List<E> list,
                                                             Function<? super E, K> keyFunction, @Nullable K key, KeyPresentBehavior presentBehavior,
                                                             KeyAbsentBehavior absentBehavior) {
        return binarySearch(
                list,
                keyFunction,
                key,
                Ordering.natural(),
                presentBehavior,
                absentBehavior);
    }


    public static <E, K> int binarySearch(
            List<E> list,
            Function<? super E, K> keyFunction,
            @Nullable K key,
            Comparator<? super K> keyComparator,
            KeyPresentBehavior presentBehavior,
            KeyAbsentBehavior absentBehavior) {
        return binarySearch(
                Lists.transform(list, keyFunction), key, keyComparator, presentBehavior, absentBehavior);
    }


    public static <E> int binarySearch(List<? extends E> list, @Nullable E key,
                                       Comparator<? super E> comparator, KeyPresentBehavior presentBehavior,
                                       KeyAbsentBehavior absentBehavior) {
        checkNotNull(comparator);
        checkNotNull(list);
        checkNotNull(presentBehavior);
        checkNotNull(absentBehavior);
        if (!(list instanceof RandomAccess)) {
            list = Lists.newArrayList(list);
        }
        // TODO(lowasser): benchmark when it's best to do a linear search

        int lower = 0;
        int upper = list.size() - 1;

        while (lower <= upper) {
            int middle = (lower + upper) >>> 1;
            int c = comparator.compare(key, list.get(middle));
            if (c < 0) {
                upper = middle - 1;
            } else if (c > 0) {
                lower = middle + 1;
            } else {
                return lower + presentBehavior.resultIndex(
                        comparator, key, list.subList(lower, upper + 1), middle - lower);
            }
        }
        return absentBehavior.resultIndex(lower);
    }
}
