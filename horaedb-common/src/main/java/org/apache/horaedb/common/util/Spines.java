/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.common.util;

import java.util.ArrayList;
import java.util.List;

public class Spines {

    public static <E> List<E> newBuf() {
        return new ArrayList<>();
    }

    public static <E> List<E> newBuf(final int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }
}
