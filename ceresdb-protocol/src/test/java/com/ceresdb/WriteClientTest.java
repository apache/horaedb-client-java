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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ceresdb.common.Endpoint;
import com.ceresdb.common.util.Clock;
import com.ceresdb.models.Err;
import com.ceresdb.models.FieldValue;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.Series;
import com.ceresdb.models.WriteOk;
import com.ceresdb.options.WriteOptions;
import com.ceresdb.proto.Common;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.ceresdb.util.TestUtil;

/**
 * @author jiachun.fjc
 */
@RunWith(value = MockitoJUnitRunner.class)
public class WriteClientTest {

    private WriteClient  writeClient;
    @Mock
    private RouterClient routerClient;

    @Before
    public void before() {
        final WriteOptions writeOpts = new WriteOptions();
        writeOpts.setAsyncPool(ForkJoinPool.commonPool());
        writeOpts.setRoutedClient(this.routerClient);

        this.writeClient = new WriteClient();
        this.writeClient.init(writeOpts);
    }

    @After
    public void after() {
        this.writeClient.shutdownGracefully();
        this.routerClient.shutdownGracefully();
    }

    @Test
    public void writeAllSuccessTest() throws ExecutionException, InterruptedException {
        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3");

        final Endpoint ep1 = Endpoint.of("127.0.0.1", 8081);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 8081);
        final Endpoint ep3 = Endpoint.of("127.0.0.3", 8081);

        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        final Storage.WriteResponse resp = Storage.WriteResponse.newBuilder() //
                .setHeader(header) //
                .setSuccess(2) //
                .build();
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep1), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep2), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep3), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4694599978937545735L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep1));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(6), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void write3And1InvalidRoute() throws ExecutionException, InterruptedException {
        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3");

        final Endpoint ep1 = Endpoint.of("127.0.0.1", 8081);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 8081);
        final Endpoint ep3 = Endpoint.of("127.0.0.3", 8081);
        final Endpoint ep4 = Endpoint.of("127.0.0.4", 8081);

        final Storage.WriteResponse resp = TestUtil.newSuccessWriteResp(2);
        final Storage.WriteResponse errResp = TestUtil.newFailedWriteResp(Result.INVALID_ROUTE, 2);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep1), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep2), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep3), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep4), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric1", //
                "write_client_test_metric2", "write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -8646902388192715970L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep1));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -3271323053870289591L;

                    {
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep4));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4340010451723257789L;

                    {
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep4));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(6), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void write3And1InvalidRouteAndRetryFailed() throws ExecutionException, InterruptedException {
        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3");

        final Endpoint ep1 = Endpoint.of("127.0.0.1", 8081);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 8081);
        final Endpoint ep3 = Endpoint.of("127.0.0.3", 8081);

        final Storage.WriteResponse resp = TestUtil.newSuccessWriteResp(2);
        final Storage.WriteResponse errResp = TestUtil.newFailedWriteResp(Result.INVALID_ROUTE, 2);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep1), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep2), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep3), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(
                TestUtil.asSet("write_client_test_metric1", "write_client_test_metric2", "write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -7535390185627686991L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep1));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -3191375160670801662L;

                    {
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 1341458669202248824L;

                    {
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        final int success = ret.mapOrElse(err -> -1, WriteOk::getSuccess);
        Assert.assertEquals(-1, success);
        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(4, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        Assert.assertEquals(1, ret.getErr().getFailedWrites().size());
    }

    @Test
    public void write3And2FailedAndRetryFailed() throws ExecutionException, InterruptedException {
        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3");

        final Endpoint ep1 = Endpoint.of("127.0.0.1", 8081);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 8081);
        final Endpoint ep3 = Endpoint.of("127.0.0.3", 8081);

        final Storage.WriteResponse resp = TestUtil.newSuccessWriteResp(2);
        final Storage.WriteResponse errResp = TestUtil.newFailedWriteResp(Result.SHOULD_RETRY, 2);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep1), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep2), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep3), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(
                TestUtil.asSet("write_client_test_metric1", "write_client_test_metric2", "write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -5936788008084035345L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep1));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -4748944007591733357L;

                    {
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient
                .routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric2", "write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -1811964578845864624L;

                    {
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient
                .routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric3", "write_client_test_metric2")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 3940955382371644111L;

                    {
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(2, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        Assert.assertEquals(1, ret.getErr().getFailedWrites().size());
        Assert.assertTrue(ret.getErr().stream().findFirst().isPresent());
        Assert.assertEquals(1, ret.getErr().stream().findFirst().get().getFailedWrites().size());
    }

    @Test
    public void write3And2FailedAndSomeNoRetry() throws ExecutionException, InterruptedException {
        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3");

        final Endpoint ep1 = Endpoint.of("127.0.0.1", 8081);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 8081);
        final Endpoint ep3 = Endpoint.of("127.0.0.3", 8081);

        final Storage.WriteResponse resp = TestUtil.newSuccessWriteResp(2);
        final Storage.WriteResponse errResp1 = TestUtil.newFailedWriteResp(Result.SHOULD_RETRY, 2);
        final Storage.WriteResponse errResp2 = TestUtil.newFailedWriteResp(400, 2);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep1), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep2), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp1));
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep3), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(errResp2));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(
                TestUtil.asSet("write_client_test_metric1", "write_client_test_metric2", "write_client_test_metric3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 1040769477529210661L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep1));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -6892083230027668740L;

                    {
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_metric2")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -9174308983134252825L;

                    {
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep2));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(2, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        Assert.assertEquals(1, ret.getErr().getFailedWrites().size());
        Assert.assertTrue(ret.getErr().stream().findFirst().isPresent());
        Assert.assertEquals(1, ret.getErr().stream().findFirst().get().getFailedWrites().size());
    }

    @Test
    public void writeSplitTest() throws ExecutionException, InterruptedException {
        writeSplit(1, 2);
        writeSplit(2, 2);
        writeSplit(3, 2);
        writeSplit(4, 4);
        writeSplit(5, 4);
        writeSplit(8, 8);
    }

    private void writeSplit(final int maxWriteSize, final int partOfSuccess)
            throws ExecutionException, InterruptedException {
        // re-init
        this.writeClient.shutdownGracefully();
        final WriteOptions writeOpts = new WriteOptions();
        writeOpts.setAsyncPool(ForkJoinPool.commonPool());
        writeOpts.setRoutedClient(this.routerClient);
        writeOpts.setMaxWriteSize(maxWriteSize);
        this.writeClient = new WriteClient();
        this.writeClient.init(writeOpts);

        final List<Rows> data = TestUtil.newListOfRows("write_client_test_metric1", //
                "write_client_test_metric2", //
                "write_client_test_metric3", //
                "write_client_test_metric4", //
                "write_client_test_metric5", //
                "write_client_test_metric6", //
                "write_client_test_metric7", //
                "write_client_test_metric8");

        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        final Storage.WriteResponse resp = Storage.WriteResponse.newBuilder() //
                .setHeader(header) //
                .setSuccess(partOfSuccess) //
                .build();
        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4694599978937545735L;

                    {
                        put("write_client_test_metric1", Route.of("write_client_test_metric1", ep));
                        put("write_client_test_metric2", Route.of("write_client_test_metric2", ep));
                        put("write_client_test_metric3", Route.of("write_client_test_metric3", ep));
                        put("write_client_test_metric4", Route.of("write_client_test_metric4", ep));
                        put("write_client_test_metric5", Route.of("write_client_test_metric5", ep));
                        put("write_client_test_metric6", Route.of("write_client_test_metric6", ep));
                        put("write_client_test_metric7", Route.of("write_client_test_metric7", ep));
                        put("write_client_test_metric8", Route.of("write_client_test_metric8", ep));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(data, Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(16), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void streamWriteTest() {
        final String testMetric = "stream_metric_test";
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);
        Mockito.when(this.routerClient.routeFor(Mockito.eq(Collections.singleton(testMetric)))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {

                    private static final long serialVersionUID = 8473563130528272901L;

                    {
                        put(testMetric, Route.of(testMetric, ep));
                    }
                }));
        final CompletableFuture<WriteOk> f = new CompletableFuture<>();
        final AtomicInteger dataCount = new AtomicInteger();
        Mockito.when(this.routerClient.invokeClientStreaming(Mockito.any(), Mockito.any(Storage.WriteRequest.class),
                Mockito.any(), Mockito.any())).thenReturn(new Observer<Storage.WriteRequest>() {

                    @Override
                    public void onNext(final Storage.WriteRequest value) {
                        final int c = value.getMetricsList().stream() //
                                .flatMap(wmc -> wmc.getEntriesList().stream()) //
                                .map(Storage.WriteEntry::getFieldGroupsCount).reduce(0, Integer::sum);
                        dataCount.addAndGet(c);
                    }

                    @Override
                    public void onError(final Throwable err) {
                        // ignored
                    }

                    @Override
                    public void onCompleted() {
                        f.complete(WriteOk.ok(dataCount.get(), 0, null));
                    }
                });

        final StreamWriteBuf<Rows, WriteOk> writer = this.writeClient.streamWrite(testMetric);
        final CompletableFuture<WriteOk> ret = writer //
                .write(TestUtil.newRow(testMetric)) //
                .write(TestUtil.newRow(testMetric)) //
                .write(TestUtil.newRow(testMetric)) //
                .flush() //
                .write(TestUtil.newRow(testMetric)) //
                .flush() //
                .writeAndFlush(TestUtil.newListOfRows(testMetric, testMetric)) //
                .completed();

        f.whenComplete((r, e) -> {
            if (e != null) {
                ret.completeExceptionally(e);
            } else {
                ret.complete(r);
            }
        });

        Assert.assertEquals(12, ret.join().getSuccess());
    }

    @Test
    public void rowsToWriteProtoTest() {
        final Rows rs1 = Series.newBuilder("metric1") //
                .tag("m1_tag1", "v1") //
                .tag("m1_tag2", "v2") //
                .tag("m1_tag3", "v3") //
                .toRowsBuilder(true) //
                .fields(Clock.defaultClock().getTick(), input -> {
                    input.put("m1_field1", FieldValue.withDouble(0.1));
                    input.put("m1_field2", FieldValue.withString("he"));
                    input.put("m1_field3", FieldValue.withStringOrNull(null)); // null
                }) //
                .fields(Clock.defaultClock().getTick() + 10, input -> {
                    input.put("m1_field1", FieldValue.withDouble(0.1));
                    input.put("m1_field2", FieldValue.withString("he"));
                    input.put("m1_field3", FieldValue.withString("surprise!!!"));
                }) //
                .build();

        final Rows rs2 = Series.newBuilder("metric2") //
                .tag("m2_tag1", "v1") //
                .tag("m2_tag2", "v2") //
                .tag("m3_tag3", "v3") //
                .toRowsBuilder(true) //
                .fields(Clock.defaultClock().getTick(), input -> {
                    input.put("m2_field1", FieldValue.withDouble(0.1));
                    input.put("m2_field2", FieldValue.withString("he"));
                }) //
                .fields(Clock.defaultClock().getTick() + 10, input -> {
                    input.put("m2_field1", FieldValue.withDouble(0.1));
                    input.put("m2_field2", FieldValue.withString("he"));
                    input.put("m2_field3", FieldValue.withString("surprise!!!"));
                }) //
                .build();

        final Rows rs3 = Series.newBuilder("metric1") //
                .tag("m1_tag1", "vv1") //
                .tag("m1_tag2", "vv2") //
                .toRowsBuilder(true) //
                .fields(System.currentTimeMillis(), input -> {
                    input.put("m1_field1", FieldValue.withDouble(0.1));
                    input.put("m1_field2", FieldValue.withString("he"));
                }) //
                .build();

        final Storage.WriteRequest writeReq = this.writeClient.toWriteRequestObj(Stream.of(rs1, rs2, rs3));

        Assert.assertNotNull(writeReq);
        Assert.assertEquals(2, writeReq.getMetricsCount());
        final List<String> metrics = writeReq.getMetricsList() //
                .stream() //
                .map(Storage.WriteMetric::getMetric) //
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("metric1", "metric2"), metrics);

        {
            final Optional<Storage.WriteMetric> opl = writeReq.getMetricsList().stream()
                    .filter(wm -> wm.getMetric().equals("metric1")) //
                    .findFirst(); //
            Assert.assertTrue(opl.isPresent());

            final Storage.WriteMetric metric = opl.get();
            Assert.assertEquals(3, metric.getTagNamesCount());
            Assert.assertEquals(3, metric.getFieldNamesCount());
            Assert.assertEquals(2, metric.getEntriesCount());

            {
                final Optional<Storage.WriteEntry> opl2 = metric.getEntriesList().stream().filter(w -> {
                    final Set<String> tagVSet = w.getTagsList().stream().map(tag -> tag.getValue().getStringValue())
                            .collect(Collectors.toSet());

                    return tagVSet.contains("v1") && tagVSet.contains("v2") && tagVSet.contains("v3");
                }).findFirst();
                Assert.assertTrue(opl2.isPresent());
                Assert.assertEquals(0, opl2.get().getTags(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getTags(1).getNameIndex());
                Assert.assertEquals(2, opl2.get().getTags(2).getNameIndex());
                Assert.assertEquals(2, opl2.get().getFieldGroupsCount());
                Assert.assertEquals(2, opl2.get().getFieldGroups(0).getFieldsCount());
                Assert.assertEquals(3, opl2.get().getFieldGroups(1).getFieldsCount());
                Assert.assertEquals(0, opl2.get().getFieldGroups(0).getFields(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getFieldGroups(0).getFields(1).getNameIndex());
                Assert.assertEquals(0, opl2.get().getFieldGroups(1).getFields(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getFieldGroups(1).getFields(1).getNameIndex());
                Assert.assertEquals(2, opl2.get().getFieldGroups(1).getFields(2).getNameIndex());
            }
        }

        {
            final Optional<Storage.WriteMetric> opl = writeReq.getMetricsList().stream()
                    .filter(wm -> wm.getMetric().equals("metric2")) //
                    .findFirst(); //
            Assert.assertTrue(opl.isPresent());

            final Storage.WriteMetric metric = opl.get();
            Assert.assertEquals(3, metric.getTagNamesCount());
            Assert.assertEquals(3, metric.getFieldNamesCount());
            Assert.assertEquals(1, metric.getEntriesCount());

            {
                final Optional<Storage.WriteEntry> opl2 = metric.getEntriesList().stream().filter(w -> {
                    final Set<String> tagVSet = w.getTagsList().stream().map(tag -> tag.getValue().getStringValue())
                            .collect(Collectors.toSet());

                    return tagVSet.contains("v1") && tagVSet.contains("v2") && tagVSet.contains("v3");
                }).findFirst();
                Assert.assertTrue(opl2.isPresent());
                Assert.assertEquals(0, opl2.get().getTags(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getTags(1).getNameIndex());
                Assert.assertEquals(2, opl2.get().getTags(2).getNameIndex());
                Assert.assertEquals(2, opl2.get().getFieldGroupsCount());
                Assert.assertEquals(2, opl2.get().getFieldGroups(0).getFieldsCount());
                Assert.assertEquals(3, opl2.get().getFieldGroups(1).getFieldsCount());
                Assert.assertEquals(0, opl2.get().getFieldGroups(0).getFields(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getFieldGroups(0).getFields(1).getNameIndex());
                Assert.assertEquals(0, opl2.get().getFieldGroups(1).getFields(0).getNameIndex());
                Assert.assertEquals(1, opl2.get().getFieldGroups(1).getFields(1).getNameIndex());
                Assert.assertEquals(2, opl2.get().getFieldGroups(1).getFields(2).getNameIndex());
            }
        }
    }
}
