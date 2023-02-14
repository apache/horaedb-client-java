/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import org.junit.Assert;
import org.junit.Test;

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
