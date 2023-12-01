/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.util.concurrent.ExecutorService;

/**
 * Like rust: pub type SharedThreadPool = RcObjectPool<ExecutorService>
 *
 */
public class SharedThreadPool extends RcObjectPool<ExecutorService> {

    public SharedThreadPool(Resource<ExecutorService> resource) {
        super(resource);
    }
}
