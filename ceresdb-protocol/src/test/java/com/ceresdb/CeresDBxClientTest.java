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
package com.ceresdb;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.ceresdb.CeresDBxClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.internal.ReferenceFieldUpdater;
import com.ceresdb.common.util.internal.Updaters;
import com.ceresdb.models.Err;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.WriteOk;
import com.ceresdb.options.CeresDBxOptions;
import com.ceresdb.util.TestUtil;

/**
 *
 * @author jiachun.fjc
 */
@RunWith(value = MockitoJUnitRunner.class)
public class CeresDBxClientTest {

    private static final ReferenceFieldUpdater<CeresDBxClient, WriteClient> WC_UPDATER = Updaters //
            .newReferenceFieldUpdater(CeresDBxClient.class, "writeClient");

    private CeresDBxClient  client;
    private CeresDBxOptions opts;
    @Mock
    private WriteClient     writeClient;

    @Before
    public void before() {
        this.opts = CeresDBxOptions.newBuilder("127.0.0.1", 8081) //
                .tenant("test", "sub_test", "test_token") //
                .writeMaxRetries(1) //
                .readMaxRetries(1) //
                .build();
        this.client = new CeresDBxClient();
    }

    @After
    public void after() {
        MetricsUtil.reportImmediately();
        this.client.shutdownGracefully();
    }

    @Test(expected = IllegalStateException.class)
    public void withoutInitTest() {
        final Rows rows = TestUtil.newRow("test_metric1_not_init");
        this.client.write(rows);
    }

    @Test(expected = IllegalStateException.class)
    public void repeatedStartTest() {
        this.client.init(this.opts);
        this.client.init(this.opts);
    }

    @Test
    public void instancesTest() {
        this.client.init(this.opts);
        Assert.assertEquals(1, CeresDBxClient.instances().size());
        this.client.shutdownGracefully();
        Assert.assertTrue(CeresDBxClient.instances().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void helloWorldTest() throws ExecutionException, InterruptedException {
        initAndMockWriteClient();

        final Rows rows = TestUtil.newRow("test_metric1");

        Mockito.when(this.writeClient.write(Mockito.anyList(), Mockito.any())) //
                .thenReturn(Utils.completedCf(WriteOk.ok(2, 0, null).mapToResult()));
        final CompletableFuture<Result<WriteOk, Err>> f = this.client.write(rows);
        final Result<WriteOk, Err> ret = f.get();
        Assert.assertTrue(ret.isOk());
        final int success = ret.mapOr(-1, WriteOk::getSuccess);
        final int failed = ret.mapOr(-1, WriteOk::getFailed);
        Assert.assertEquals(2, success);
        Assert.assertEquals(0, failed);
    }

    private void initAndMockWriteClient() {
        this.client.init(this.opts);
        WC_UPDATER.set(this.client, this.writeClient);
    }
}
