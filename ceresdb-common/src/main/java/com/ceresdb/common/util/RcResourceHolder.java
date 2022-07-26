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
package com.ceresdb.common.util;

import java.util.IdentityHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jiachun.fjc
 */
public class RcResourceHolder<T> {

    private static final Logger LOG = LoggerFactory.getLogger(RcResourceHolder.class);

    private final Map<ObjectPool.Resource<T>, Instance<T>> instances = new IdentityHashMap<>();

    public synchronized T get(final ObjectPool.Resource<T> resource) {
        Instance<T> ins = this.instances.get(resource);
        if (ins == null) {
            ins = new Instance<>(resource.create());
            this.instances.put(resource, ins);
            LOG.info("[RcResourceHolder] create instance: {}.", ins);
        }
        ins.inc();
        return ins.payload();
    }

    public synchronized void release(final ObjectPool.Resource<T> resource, final T returned) {
        final Instance<T> cached = this.instances.get(resource);
        Requires.requireNonNull(cached, "No cached instance found for " + resource);
        Requires.requireTrue(returned == cached.payload(), "Releasing the wrong instance, expected=%s, actual=%s",
                cached.payload(), returned);
        Requires.requireTrue(cached.rc() > 0, "RefCount has already reached zero");
        if (cached.decAndGet() == 0) {
            LOG.info("[RcResourceHolder] close instance: {}.", cached);
            resource.close(cached.payload());
            this.instances.remove(resource);
        }
    }

    private static class Instance<T> {
        final T payload;
        int     refCount;
        int     maxRefCount;

        Instance(T payload) {
            this.payload = payload;
        }

        void inc() {
            this.refCount++;
            this.maxRefCount = Math.max(this.maxRefCount, this.refCount);
        }

        int decAndGet() {
            return --this.refCount;
        }

        int rc() {
            return this.refCount;
        }

        T payload() {
            return this.payload;
        }

        @Override
        public String toString() {
            return "Instance{" + //
                   "payload=" + payload + //
                   ", refCount=" + refCount + //
                   ", maxRefCount=" + maxRefCount + //
                   '}';
        }
    }
}
