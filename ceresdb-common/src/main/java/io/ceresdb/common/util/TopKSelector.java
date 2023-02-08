/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ceresdb.common.util;

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
