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

/**
 * An object pool.
 *
 * @author jiachun.fjc
 */
public interface ObjectPool<T> {

    /**
     * Get an object from the pool.
     */
    T getObject();

    /**
     * Return the object to the pool.  The caller should not use the object beyond this point.
     */
    void returnObject(final T returned);

    /**
     * Defines a resource, and the way to create and destroy instances of it.
     */
    interface Resource<T> {

        /**
         * Create a new instance of the resource.
         */
        T create();

        /**
         * Destroy the given instance.
         */
        void close(final T instance);
    }
}
