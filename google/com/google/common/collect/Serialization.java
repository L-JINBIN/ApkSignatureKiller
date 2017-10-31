

package com.google.common.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;


final class Serialization {
    private Serialization() {
    }


    static int readCount(ObjectInputStream stream) throws IOException {
        return stream.readInt();
    }


    static <E> void writeMultiset(
            Multiset<E> multiset, ObjectOutputStream stream) throws IOException {
        int entryCount = multiset.entrySet().size();
        stream.writeInt(entryCount);
        for (Multiset.Entry<E> entry : multiset.entrySet()) {
            stream.writeObject(entry.getElement());
            stream.writeInt(entry.getCount());
        }
    }


    static <E> void populateMultiset(
            Multiset<E> multiset, ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        int distinctElements = stream.readInt();
        populateMultiset(multiset, stream, distinctElements);
    }


    static <E> void populateMultiset(
            Multiset<E> multiset, ObjectInputStream stream, int distinctElements)
            throws IOException, ClassNotFoundException {
        for (int i = 0; i < distinctElements; i++) {
            @SuppressWarnings("unchecked") // reading data stored by writeMultiset
                    E element = (E) stream.readObject();
            int count = stream.readInt();
            multiset.add(element, count);
        }
    }


    static <K, V> void writeMultimap(
            Multimap<K, V> multimap, ObjectOutputStream stream) throws IOException {
        stream.writeInt(multimap.asMap().size());
        for (Map.Entry<K, Collection<V>> entry : multimap.asMap().entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeInt(entry.getValue().size());
            for (V value : entry.getValue()) {
                stream.writeObject(value);
            }
        }
    }


    static <K, V> void populateMultimap(
            Multimap<K, V> multimap, ObjectInputStream stream, int distinctKeys)
            throws IOException, ClassNotFoundException {
        for (int i = 0; i < distinctKeys; i++) {
            @SuppressWarnings("unchecked") // reading data stored by writeMultimap
                    K key = (K) stream.readObject();
            Collection<V> values = multimap.get(key);
            int valueCount = stream.readInt();
            for (int j = 0; j < valueCount; j++) {
                @SuppressWarnings("unchecked") // reading data stored by writeMultimap
                        V value = (V) stream.readObject();
                values.add(value);
            }
        }
    }

    // Secret sauce for setting final fields; don't make it public.
    static <T> FieldSetter<T> getFieldSetter(
            final Class<T> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return new FieldSetter<>(field);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e); // programmer error
        }
    }

    // Secret sauce for setting final fields; don't make it public.
    static final class FieldSetter<T> {
        private final Field field;

        private FieldSetter(Field field) {
            this.field = field;
            field.setAccessible(true);
        }

        void set(T instance, Object value) {
            try {
                field.set(instance, value);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }

        void set(T instance, int value) {
            try {
                field.set(instance, value);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }
    }
}
