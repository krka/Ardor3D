/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * This class is not unsimiliar to the Map interface. It provides methods for fast storing and retrieving object with a
 * key, based on hashes and key/value pairs. However there are some important distinctions from a normal HashMap. <br>
 * 
 * Keys are not compared using object-equality but using reference-equality. This means you can only use the original
 * object to retrieve any values from this cache. It also means the equals() method of your key is never used. <br>
 * 
 * This allows system identy hashes to be used instead of normal hashes, which means the potentially slow hashCode()
 * method of your objects (eg. Buffer objects) are never used.<br>
 * 
 * Finally, the key itself is stored through a WeakReference. Once the key object becomes weakly referable, this
 * reference will be added to the internal ReferenceQueue of this cache. This queue is polled everytime when any of the
 * methods of this are invoked. After the reference is polled the key/value pair is removed from the map, and both key
 * and value can be collected. (In case of the value, if no other references to it exist)
 * 
 * @see WeakIdentityCache#expunge() <br>
 * 
 *      This is an implementation from scratch, but some of the concepts came from other implementations, most notably
 *      WeakIdenityHashMap from the jBoss project.
 * 
 *      NOTE: this implementation is not synchronized.
 */
public class WeakIdentityCache<K, V> {

    private Entry<K, V>[] entries;
    private int size;
    private int threshold;
    private final static float LOAD = 0.75f;

    private final ReferenceQueue<K> refqueue = new ReferenceQueue<K>();

    /**
     * Create a new WeakIdenityCache (see main javadoc entry for this class)
     */
    @SuppressWarnings("unchecked")
    public WeakIdentityCache() {
        threshold = 16;
        entries = new Entry[threshold];
    }

    private int hash(final K x) {
        final int hash = System.identityHashCode(x);
        return hash - (hash << 7);
    }

    private int index(final int hash, final int length) {
        return hash & (length - 1);
    }

    @SuppressWarnings("unchecked")
    private void resize(final int newsize) {
        expunge();
        final int oldsize = entries.length;

        if (size < threshold || oldsize > newsize) {
            return;
        }

        final Entry<K, V>[] newentries = new Entry[newsize];

        transfer(entries, newentries);
        entries = newentries;

        if (size >= threshold / 2) {
            threshold = (int) (newsize * LOAD);
        } else {
            expunge();
            transfer(newentries, entries);
        }
    }

    private void transfer(final Entry<K, V>[] src, final Entry<K, V>[] dest) {
        for (int k = 0; k < src.length; ++k) {
            Entry<K, V> entry = src[k];
            src[k] = null;
            while (entry != null) {
                final Entry<K, V> next = entry.nextEntry;
                if (entry.get() == null) {
                    entry.nextEntry = null;
                    entry.value = null;
                    size--;
                } else {
                    final int i = index(entry.hash, dest.length);
                    entry.nextEntry = dest[i];
                    dest[i] = entry;
                }
                entry = next;
            }
        }
    }

    /**
     * Returns value for this key.
     */
    public V get(final K key) {
        expunge();
        final int hash = hash(key);
        final int index = index(hash, entries.length);
        Entry<K, V> entry = entries[index];
        while (entry != null) {
            if (entry.hash == hash && key == entry.get()) {
                return entry.value;
            }
            entry = entry.nextEntry;
        }
        return null;
    }

    /**
     * Put a value in this cache with key. <br>
     * Both key and value should not be null.
     */
    public V put(final K key, final V value) {
        expunge();
        final int hash = hash(key);
        final int index = index(hash, entries.length);

        for (Entry<K, V> entry = entries[index]; entry != null; entry = entry.nextEntry) {
            if (hash == entry.hash && key == entry.get()) {
                final V oldentry = entry.value;
                if (value != oldentry) {
                    entry.value = value;
                }
                return oldentry;
            }
        }

        entries[index] = new Entry<K, V>(key, value, refqueue, hash, entries[index]);
        if (++size >= threshold) {
            resize(entries.length * 2);
        }
        return null;
    }

    /**
     * Removes the value for this key.
     */
    public V remove(final K key) {
        expunge();
        final int hash = hash(key);
        final int index = index(hash, entries.length);
        Entry<K, V> temp = entries[index];
        Entry<K, V> previous = temp;

        while (temp != null) {
            final Entry<K, V> next = temp.nextEntry;
            if (hash == temp.hash && key == temp.get()) {
                size--;
                if (previous == temp) {
                    entries[index] = next;
                } else {
                    previous.nextEntry = next;
                }
                return temp.value;
            }
            previous = temp;
            temp = next;
        }

        return null;
    }

    /**
     * Clear the cache of all entries it has.
     */
    public void clear() {
        while (refqueue.poll() != null) {
            ;
        }

        for (int i = 0; i < entries.length; ++i) {
            entries[i] = null;
        }
        size = 0;

        while (refqueue.poll() != null) {
            ;
        }
    }

    /**
     * Removes all key/value pairs from keys who've become weakly reachable from this cache. This method is called from
     * every other method in this class as well, but can be called seperatly to ensure all (in)direct weak reference are
     * removed, when none of the other methods of this class are called frequently enough.<br>
     * 
     * Note that this method is relativly cheap (espc. if the queue is empty) but does most likely involve
     * synchronization.
     * 
     * @see ReferenceQueue#poll()
     */
    @SuppressWarnings("unchecked")
    public void expunge() {
        Entry<K, V> entry;
        while ((entry = (Entry<K, V>) refqueue.poll()) != null) {
            final int index = index(entry.hash, entries.length);

            Entry<K, V> temp = entries[index];
            Entry<K, V> previous = temp;
            while (temp != null) {
                final Entry<K, V> next = temp.nextEntry;
                if (temp == entry) {
                    if (previous == entry) {
                        entries[index] = next;
                    } else {
                        previous.nextEntry = next;
                    }
                    entry.nextEntry = null;
                    entry.value = null;
                    size--;
                    break;
                }
                previous = temp;
                temp = next;
            }
        }
    }

    private static class Entry<K, V> extends WeakReference<K> {

        private Entry<K, V> nextEntry;
        private V value;
        private final int hash;

        Entry(final K key, final V value, final ReferenceQueue<K> queue, final int hash, final Entry<K, V> next) {
            super(key, queue);
            this.value = value;
            this.hash = hash;
            this.nextEntry = next;
        }
    }
}