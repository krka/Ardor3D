/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Quick and merge sort implementations that create no garbage, unlike {@link Arrays#sort}. The merge sort is stable,
 * the quick sort is not.
 */
public abstract class SortUtil {
    /**
     * Quick sorts the supplied array using the specified comparator.
     */
    public static void qsort(final Object[] a, final Comparator comp) {
        qsort(a, 0, a.length - 1, comp);
    }

    /**
     * Quick sorts the supplied array using the specified comparator.
     * 
     * @param lo0
     *            the index of the lowest element to include in the sort.
     * @param hi0
     *            the index of the highest element to include in the sort.
     */
    @SuppressWarnings("unchecked")
    public static void qsort(final Object[] a, final int lo0, final int hi0, final Comparator comp) {
        // bail out if we're already done
        if (hi0 <= lo0) {
            return;
        }

        // if this is a two element list, do a simple sort on it
        Object t;
        if (hi0 - lo0 == 1) {
            // if they're not already sorted, swap them
            if (comp.compare(a[hi0], a[lo0]) < 0) {
                t = a[lo0];
                a[lo0] = a[hi0];
                a[hi0] = t;
            }
            return;
        }

        // the middle element in the array is our partitioning element
        final Object mid = a[(lo0 + hi0) / 2];

        // set up our partitioning boundaries
        int lo = lo0 - 1, hi = hi0 + 1;

        // loop through the array until indices cross
        for (;;) {
            // find the first element that is greater than or equal to
            // the partition element starting from the left Index.
            while (comp.compare(a[++lo], mid) < 0) {
                ;
            }

            // find an element that is smaller than or equal to
            // the partition element starting from the right Index.
            while (comp.compare(mid, a[--hi]) < 0) {
                ;
            }

            // swap the two elements or bail out of the loop
            if (hi > lo) {
                t = a[lo];
                a[lo] = a[hi];
                a[hi] = t;
            } else {
                break;
            }
        }

        // if the right index has not reached the left side of array
        // must now sort the left partition
        if (lo0 < lo - 1) {
            qsort(a, lo0, lo - 1, comp);
        }

        // if the left index has not reached the right side of array
        // must now sort the right partition
        if (hi + 1 < hi0) {
            qsort(a, hi + 1, hi0, comp);
        }
    }

    public static void qsort(final int[] a, final int lo0, final int hi0, final Comparator comp) {
        // bail out if we're already done
        if (hi0 <= lo0) {
            return;
        }

        // if this is a two element list, do a simple sort on it
        int t;
        if (hi0 - lo0 == 1) {
            // if they're not already sorted, swap them
            if (comp.compare(a[hi0], a[lo0]) < 0) {
                t = a[lo0];
                a[lo0] = a[hi0];
                a[hi0] = t;
            }
            return;
        }

        // the middle element in the array is our partitioning element
        final int mid = a[(lo0 + hi0) / 2];

        // set up our partitioning boundaries
        int lo = lo0 - 1, hi = hi0 + 1;

        // loop through the array until indices cross
        for (;;) {
            // find the first element that is greater than or equal to
            // the partition element starting from the left Index.
            while (comp.compare(a[++lo], mid) < 0) {
                ;
            }

            // find an element that is smaller than or equal to
            // the partition element starting from the right Index.
            while (comp.compare(mid, a[--hi]) < 0) {
                ;
            }

            // swap the two elements or bail out of the loop
            if (hi > lo) {
                t = a[lo];
                a[lo] = a[hi];
                a[hi] = t;
            } else {
                break;
            }
        }

        // if the right index has not reached the left side of array
        // must now sort the left partition
        if (lo0 < lo - 1) {
            qsort(a, lo0, lo - 1, comp);
        }

        // if the left index has not reached the right side of array
        // must now sort the right partition
        if (hi + 1 < hi0) {
            qsort(a, hi + 1, hi0, comp);
        }
    }

    /**
     * Merge sorts the supplied array using the specified comparator.
     * 
     * @param src
     *            contains the elements to be sorted.
     * @param dest
     *            must contain the same values as the src array.
     */
    public static void msort(final Object[] src, final Object[] dest, final Comparator comp) {
        msort(src, dest, 0, src.length, 0, comp);
    }

    /**
     * Merge sorts the supplied array using the specified comparator.
     * 
     * @param src
     *            contains the elements to be sorted.
     * @param dest
     *            must contain the same values as the src array.
     */
    public static void msort(final Object[] src, final Object[] dest, final int low, final int high,
            final Comparator comp) {
        msort(src, dest, low, high, 0, comp);
    }

    /** Implements the actual merge sort. */
    @SuppressWarnings("unchecked")
    protected static void msort(final Object[] src, final Object[] dest, int low, int high, final int offset,
            final Comparator comp) {
        // use an insertion sort on small arrays
        final int length = high - low;
        if (length < INSERTION_SORT_THRESHOLD) {
            for (int ii = low; ii < high; ii++) {
                for (int jj = ii; jj > low && comp.compare(dest[jj - 1], dest[jj]) > 0; jj--) {
                    final Object temp = dest[jj];
                    dest[jj] = dest[jj - 1];
                    dest[jj - 1] = temp;
                }
            }
            return;
        }

        // recursively sort each half of dest into src
        final int destLow = low, destHigh = high;
        low += offset;
        high += offset;
        final int mid = (low + high) >> 1;
        msort(dest, src, low, mid, -offset, comp);
        msort(dest, src, mid, high, -offset, comp);

        // if the list is already sorted, just copy from src to dest; this
        // optimization results in faster sorts for nearly ordered lists
        if (comp.compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // merge the sorted halves (now in src) into dest
        for (int ii = destLow, pp = low, qq = mid; ii < destHigh; ii++) {
            if (qq >= high || pp < mid && comp.compare(src[pp], src[qq]) <= 0) {
                dest[ii] = src[pp++];
            } else {
                dest[ii] = src[qq++];
            }
        }
    }

    /**
     * The size at or below which we will use insertion sort because it's probably faster.
     */
    private static final int INSERTION_SORT_THRESHOLD = 7;
}
