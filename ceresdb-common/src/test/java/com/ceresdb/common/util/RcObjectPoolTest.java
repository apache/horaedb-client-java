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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jiachun.fjc
 */
public class RcObjectPoolTest {

    static final ObjectPool.Resource<Object> RESOURCE = new ObjectPool.Resource<Object>() {

        @Override
        public Object create() {
            return new Object();
        }

        @Override
        public void close(final Object instance) {
        }
    };

    static final RcObjectPool<Object> POOL = new RcObjectPool<>(RESOURCE);

    @Test
    public void sharedTest() {
        final Object obj1 = POOL.getObject();
        final Object obj2 = POOL.getObject();
        Assert.assertEquals(obj1, obj2);
        POOL.returnObject(obj1);
        POOL.returnObject(obj2);
    }

    @Test
    public void reCreateTest() {
        final Object obj1 = POOL.getObject();
        POOL.returnObject(obj1);
        final Object obj2 = POOL.getObject();
        Assert.assertNotEquals(obj1, obj2);
        POOL.returnObject(obj2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void returnWrongInstanceTest() {
        POOL.getObject();
        POOL.returnObject(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void returnBeforeGetObjectTest() {
        POOL.returnObject(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void returnTooMoreTest() {
        final Object obj = POOL.getObject();
        POOL.returnObject(obj);
        POOL.returnObject(obj);
    }
}
