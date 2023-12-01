/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common;

import java.util.stream.Stream;

public interface Streamable<T> {

    /**
     * Returns a sequential {@code Stream} over the elements.
     *
     * @return a sequential {@code Stream}.
     */
    Stream<T> stream();
}
