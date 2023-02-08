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
package io.ceresdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.ceresdb.models.Point;
import io.ceresdb.models.Value;
import io.ceresdb.models.WriteRequest;
import io.ceresdb.proto.internal.Common;
import io.ceresdb.proto.internal.Storage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.ceresdb.common.Endpoint;
import io.ceresdb.common.util.Clock;
import io.ceresdb.models.Err;
import io.ceresdb.models.Result;
import io.ceresdb.models.WriteOk;
import io.ceresdb.options.WriteOptions;
import io.ceresdb.rpc.Context;
import io.ceresdb.rpc.Observer;
import io.ceresdb.util.StreamWriteBuf;
import io.ceresdb.util.TestUtil;
import io.ceresdb.util.Utils;

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
        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3");

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
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep1));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(6), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void write3And1InvalidRoute() throws ExecutionException, InterruptedException {
        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3");

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
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table1", //
                "write_client_test_table2", "write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -8646902388192715970L;

                    {
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep1));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -3271323053870289591L;

                    {
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep4));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4340010451723257789L;

                    {
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep4));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(6), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void write3And1InvalidRouteAndRetryFailed() throws ExecutionException, InterruptedException {
        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3");

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
                TestUtil.asSet("write_client_test_table1", "write_client_test_table2", "write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -7535390185627686991L;

                    {
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep1));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -3191375160670801662L;

                    {
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 1341458669202248824L;

                    {
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        final int success = ret.mapOrElse(err -> -1, WriteOk::getSuccess);
        Assert.assertEquals(-1, success);
        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(4, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        Assert.assertEquals(2, ret.getErr().getFailedWrites().size());
    }

    @Test
    public void write3And2FailedAndRetryFailed() throws ExecutionException, InterruptedException {
        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3");

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
                TestUtil.asSet("write_client_test_table1", "write_client_test_table2", "write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -5936788008084035345L;

                    {
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep1));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -4748944007591733357L;

                    {
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient
                .routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table2", "write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -1811964578845864624L;

                    {
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient
                .routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table3", "write_client_test_table2")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 3940955382371644111L;

                    {
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(2, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        Assert.assertTrue(ret.getErr().stream().findFirst().isPresent());
        Assert.assertEquals(2, ret.getErr().stream().count());
        Assert.assertEquals(4, ret.getErr().stream().mapToInt(err -> err.getFailedWrites().size()).sum());

    }

    @Test
    public void write3And2FailedAndSomeNoRetry() throws ExecutionException, InterruptedException {
        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3");

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
                TestUtil.asSet("write_client_test_table1", "write_client_test_table2", "write_client_test_table3")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 1040769477529210661L;

                    {
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep1));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -6892083230027668740L;

                    {
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep3));
                    }
                }));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(TestUtil.asSet("write_client_test_table2")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -9174308983134252825L;

                    {
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep2));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertFalse(ret.isOk());
        Assert.assertEquals(2, ret.getErr().getSubOk().getSuccess());
        Assert.assertEquals(0, ret.getErr().getSubOk().getFailed());
        // TODO
        // How to process multi err
        //Assert.assertEquals(1, ret.getErr().getFailedWrites().size());
        //Assert.assertTrue(ret.getErr().stream().findFirst().isPresent());
        //Assert.assertEquals(1, ret.getErr().stream().findFirst().get().getFailedWrites().size());
    }

    @Test
    public void writeSplitTest() throws ExecutionException, InterruptedException {
        writeSplit(1, 1);
        writeSplit(2, 2);
        writeSplit(4, 4);
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

        final List<Point> data = TestUtil.newMultiTablePoints("write_client_test_table1", //
                "write_client_test_table2", //
                "write_client_test_table3", //
                "write_client_test_table4", //
                "write_client_test_table5", //
                "write_client_test_table6", //
                "write_client_test_table7", //
                "write_client_test_table8");

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
                        put("write_client_test_table1", Route.of("write_client_test_table1", ep));
                        put("write_client_test_table2", Route.of("write_client_test_table2", ep));
                        put("write_client_test_table3", Route.of("write_client_test_table3", ep));
                        put("write_client_test_table4", Route.of("write_client_test_table4", ep));
                        put("write_client_test_table5", Route.of("write_client_test_table5", ep));
                        put("write_client_test_table6", Route.of("write_client_test_table6", ep));
                        put("write_client_test_table7", Route.of("write_client_test_table7", ep));
                        put("write_client_test_table8", Route.of("write_client_test_table8", ep));
                    }
                }));

        final CompletableFuture<Result<WriteOk, Err>> f = this.writeClient.write(new WriteRequest(data),
                Context.newDefault());
        final Result<WriteOk, Err> ret = f.get();

        Assert.assertTrue(ret.isOk());
        Assert.assertEquals(new Integer(16), ret.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(0), ret.mapOr(-1, WriteOk::getFailed));
    }

    @Test
    public void streamWriteTest() {
        final String testTable = "stream_table_test";
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);
        Mockito.when(this.routerClient.routeFor(Mockito.eq(Collections.singleton(testTable)))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {

                    private static final long serialVersionUID = 8473563130528272901L;

                    {
                        put(testTable, Route.of(testTable, ep));
                    }
                }));
        final CompletableFuture<WriteOk> f = new CompletableFuture<>();
        final AtomicInteger dataCount = new AtomicInteger();
        Mockito.when(this.routerClient.invokeClientStreaming(Mockito.any(), Mockito.any(Storage.WriteRequest.class),
                Mockito.any(), Mockito.any())).thenReturn(new Observer<Storage.WriteRequest>() {

                    @Override
                    public void onNext(final Storage.WriteRequest value) {
                        final int c = value.getTableRequestsList().stream() //
                                .flatMap(writeTableRequest -> writeTableRequest.getEntriesList().stream()) //
                                .map(Storage.WriteSeriesEntry::getFieldGroupsCount).reduce(0, Integer::sum);
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

        final StreamWriteBuf<Point, WriteOk> writer = this.writeClient.streamWrite(testTable);
        final CompletableFuture<WriteOk> ret = writer //
                .write(TestUtil.newTablePoints(testTable)) //
                .write(TestUtil.newTablePoints(testTable)) //
                .write(TestUtil.newTablePoints(testTable)) //
                .flush() //
                .write(TestUtil.newTablePoints(testTable)) //
                .flush() //
                .writeAndFlush(TestUtil.newMultiTablePoints(testTable, testTable)) //
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
        final List<Point> table1 = Point.newPointsBuilder("table1") //
                .addPoint().setTimestamp(Clock.defaultClock().getTick()).addTag("t1_tag1", Value.withString("v1")) //
                .addTag("t1_tag2", Value.withString("v2")) //
                .addTag("t1_tag3", Value.withString("v3")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he"))
                .addField("t1_field3", Value.withStringOrNull(null)).build().addPoint()
                .setTimestamp(Clock.defaultClock().getTick() + 10).addTag("t1_tag1", Value.withString("v1")) //
                .addTag("t1_tag2", Value.withString("v2")) //
                .addTag("t1_tag3", Value.withString("v3")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he"))
                .addField("t1_field3", Value.withStringOrNull("surprise!!!")).build().build();

        final List<Point> table2 = Point.newPointsBuilder("table2") //
                .addPoint().setTimestamp(Clock.defaultClock().getTick()).addTag("t2_tag1", Value.withString("v1")) //
                .addTag("t2_tag2", Value.withString("v2")) //
                .addTag("t2_tag3", Value.withString("v3")) //
                .addField("t2_field1", Value.withDouble(0.1)).addField("t2_field2", Value.withString("he")).build()
                .addPoint().setTimestamp(Clock.defaultClock().getTick() + 10).addTag("t2_tag1", Value.withString("v1")) //
                .addTag("t2_tag2", Value.withString("v2")) //
                .addTag("t2_tag3", Value.withString("v3")) //
                .addField("t2_field1", Value.withDouble(0.1)).addField("t2_field2", Value.withString("he"))
                .addField("t2_field3", Value.withStringOrNull("surprise!!!")).build().build();

        final List<Point> table3 = Point.newPointsBuilder("table1").addPoint().setTimestamp(System.currentTimeMillis())
                .addTag("t1_tag1", Value.withString("vv1")) //
                .addTag("t1_tag2", Value.withString("vv2")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he")).build()
                .build();

        final Storage.WriteRequest writeReq = this.writeClient.toWriteRequestObj(
                Stream.of(table1, table2, table3).flatMap(List::stream).collect(Collectors.toList()).stream());

        Assert.assertNotNull(writeReq);
        Assert.assertEquals(2, writeReq.getTableRequestsCount());
        final Set<String> tables = writeReq.getTableRequestsList() //
                .stream() //
                .map(Storage.WriteTableRequest::getTable) //
                .collect(Collectors.toSet());
        Assert.assertEquals(new HashSet<>(Arrays.asList("table1", "table2")), tables);

        {
            final Optional<Storage.WriteTableRequest> opl = writeReq.getTableRequestsList().stream()
                    .filter(wm -> wm.getTable().equals("table1")) //
                    .findFirst(); //
            Assert.assertTrue(opl.isPresent());

            final Storage.WriteTableRequest tableRequest = opl.get();
            Assert.assertEquals(3, tableRequest.getTagNamesCount());
            Assert.assertEquals(3, tableRequest.getFieldNamesCount());
            Assert.assertEquals(2, tableRequest.getEntriesCount());

            {
                final Optional<Storage.WriteSeriesEntry> opl2 = tableRequest.getEntriesList().stream().filter(w -> {
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
            }
        }

        {
            final Optional<Storage.WriteTableRequest> opl = writeReq.getTableRequestsList().stream()
                    .filter(wm -> wm.getTable().equals("table2")) //
                    .findFirst(); //
            Assert.assertTrue(opl.isPresent());

            final Storage.WriteTableRequest tableRequest = opl.get();
            Assert.assertEquals(3, tableRequest.getTagNamesCount());
            Assert.assertEquals(3, tableRequest.getFieldNamesCount());
            Assert.assertEquals(1, tableRequest.getEntriesCount());

            {
                final Optional<Storage.WriteSeriesEntry> opl2 = tableRequest.getEntriesList().stream().filter(w -> {
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
            }
        }
    }
}
