/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.stream.Stream;

/**
 * TopK selector based on a priority heap.
 *
 */
public class TopKSelector {

    /**
     * Select topK items in the given iterable data by given comparator.
     *
     * @param it         the iterable data
     * @param k          num of item to select
     * @param comparator the comparator of the given data
     * @param <T>        data type
     * @return topK data stream
     */
    public static <T> Stream<T> selectTopK(final Iterable<? extends T> it, //
                                           final int k, //
                                           final Comparator<? super T> comparator) {
        Requires.requireNonNull(it, "Empty.source");
        Requires.requireTrue(k > 0, "'k' must be a positive number");
        final PriorityQueue<T> heap = new PriorityQueue<>(k, comparator);

        int itemCount = 0;
        for (final T t : it) {
            if (itemCount++ < k) {
                heap.add(t);
            } else {
                final T top = heap.peek();
                if (comparator.compare(top, t) < 0) {
                    heap.poll();
                    heap.add(t);
                }
            }
        }

        return heap.stream();
    }
}
