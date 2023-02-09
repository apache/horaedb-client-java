/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Like rust: pub type SharedScheduledPool = RcObjectPool<ScheduledExecutorService>
 *
 */
public class SharedScheduledPool extends RcObjectPool<ScheduledExecutorService> {

    public SharedScheduledPool(Resource<ScheduledExecutorService> resource) {
        super(resource);
    }
}
