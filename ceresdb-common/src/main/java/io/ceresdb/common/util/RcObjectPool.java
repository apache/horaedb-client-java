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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
package io.ceresdb.common.util;

/**
 * An shared object pool with ref count.
 *
 */
public class RcObjectPool<T> implements ObjectPool<T> {

    private final Resource<T>         resource;
    private final RcResourceHolder<T> resourceHolder = new RcResourceHolder<>();

    public RcObjectPool(Resource<T> resource) {
        this.resource = resource;
    }

    @Override
    public T getObject() {
        return this.resourceHolder.get(this.resource);
    }

    @Override
    public void returnObject(final T returned) {
        this.resourceHolder.release(this.resource, returned);
    }
}
