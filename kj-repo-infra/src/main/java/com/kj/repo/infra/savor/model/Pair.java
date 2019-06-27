package com.kj.repo.infra.savor.model;

/**
 * @author kuojian21
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        super();
        this.key = key;
        this.value = value;
    }

    public static <K, V> Pair<K, V> pair(K key, V value) {
        return new Pair<>(key, value);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || other.getClass() != Pair.class) {
            return false;
        }
        Pair<?, ?> oPair = (Pair<?, ?>) other;
        if (this.key != null && !this.key.equals(oPair.key)) {
            return false;
        } else if (this.key == null && oPair.key != null) {
            return false;
        } else if (this.value != null && !this.value.equals(oPair.value)) {
            return false;
        } else {
            return this.value != null || oPair.value == null;
        }
    }

    @Override
    public int hashCode() {
        return (this.key == null ? 0 : this.key.hashCode()) / 2 + (this.value == null ? 0 : this.value.hashCode()) / 2;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}