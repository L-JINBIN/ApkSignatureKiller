

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.CollectPreconditions.checkNonnegative;


@GwtCompatible(serializable = true, emulated = true)
public final class ArrayListMultimap<K, V> extends AbstractListMultimap<K, V> {
    // Default from ArrayList
    private static final int DEFAULT_VALUES_PER_KEY = 3;

    @VisibleForTesting
    transient int expectedValuesPerKey;


    public static <K, V> ArrayListMultimap<K, V> create() {
        return new ArrayListMultimap<>();
    }


    private ArrayListMultimap() {
        super(new HashMap<K, Collection<V>>());
        expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;
    }

    private ArrayListMultimap(int expectedKeys, int expectedValuesPerKey) {
        super(Maps.<K, Collection<V>>newHashMapWithExpectedSize(expectedKeys));
        checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
        this.expectedValuesPerKey = expectedValuesPerKey;
    }

    private ArrayListMultimap(Multimap<? extends K, ? extends V> multimap) {
        this(
                multimap.keySet().size(),
                (multimap instanceof ArrayListMultimap)
                        ? ((ArrayListMultimap<?, ?>) multimap).expectedValuesPerKey
                        : DEFAULT_VALUES_PER_KEY);
        putAll(multimap);
    }


    @Override
    List<V> createCollection() {
        return new ArrayList<>(expectedValuesPerKey);
    }


    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        Serialization.writeMultimap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;
        int distinctKeys = Serialization.readCount(stream);
        Map<K, Collection<V>> map = Maps.newHashMap();
        setMap(map);
        Serialization.populateMultimap(this, stream, distinctKeys);
    }

    @GwtIncompatible("Not needed in emulated source.")
    private static final long serialVersionUID = 0;
}
