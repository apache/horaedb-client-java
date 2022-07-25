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
package com.ceresdb.rpc;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.rpc.limit.Gradient2Limit;
import com.ceresdb.rpc.limit.VegasLimit;
import com.netflix.concurrency.limits.Limit;

/**
 * @author jiachun.fjc
 */
public class LimitTest {

    private static final Logger LOG = LoggerFactory.getLogger(LimitTest.class);

    static Gradient2Limit createGradient2() {
        return Gradient2Limit.newBuilder() //
                .initialLimit(512) //
                .maxConcurrency(1024) //
                .smoothing(0.2) //
                .longWindow(100) //
                .queueSize(16) //
                .build();
    }

    static VegasLimit createVegas() {
        return VegasLimit.newBuilder() //
                .initialLimit(512) //
                .maxConcurrency(1024) //
                .smoothing(0.2) //
                .build();
    }

    @Test
    public void increaseLimit() {
        {
            final Limit limit = createVegas();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 512, false);
            Assert.assertEquals(512, limit.getLimit());
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 513, false);
            Assert.assertEquals(514, limit.getLimit());
        }

        {
            Limit limit = createGradient2();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 512, false);
            Assert.assertEquals(515, limit.getLimit());
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 513, false);
            Assert.assertEquals(518, limit.getLimit());
        }
    }

    @Test
    public void decreaseLimit() {
        {
            final Limit limit = createVegas();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 512, false);
            Assert.assertEquals(512, limit.getLimit());
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(50), 513, false);
            Assert.assertEquals(511, limit.getLimit());
        }

        {
            final Limit limit = createGradient2();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), 512, false);
            Assert.assertEquals(515, limit.getLimit());
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(50), 513, false);
            Assert.assertEquals(508, limit.getLimit());
        }
    }

    @Test
    public void longRttGradient2Test() {
        longRttTest(createGradient2());
    }

    @Test
    public void longRttVegasTest() {
        longRttTest(createVegas());
    }

    private void longRttTest(final Limit limit) {
        // avg
        for (int i = 0; i < 100; i++) {
            final int inflight = limit.getLimit();
            if (i % 10 == 0) {
                // rtt_noload
                limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(10), inflight, false);
            } else {
                limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(100), inflight, false);
            }
        }

        LOG.info("1 ---------------------> {}", limit);

        for (int i = 0; i < 200; i++) {
            final int inflight = limit.getLimit();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(100 * 10), inflight, false);
        }

        LOG.info("2 ---------------------> {}", limit);

        for (int i = 0; i < 1500; i++) {
            final int inflight = limit.getLimit();
            limit.onSample(0, TimeUnit.MILLISECONDS.toNanos(200), inflight, false);
        }
    }
}
