/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb;

import java.util.ArrayList;
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

import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.RequestContext;
import org.apache.horaedb.models.Value;
import org.apache.horaedb.models.WriteRequest;
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
import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.models.WriteOk;
import org.apache.horaedb.options.WriteOptions;
import org.apache.horaedb.rpc.Context;
import org.apache.horaedb.rpc.Observer;
import org.apache.horaedb.util.StreamWriteBuf;
import org.apache.horaedb.util.TestUtil;
import org.apache.horaedb.util.Utils;

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
        writeOpts.setDatabase("public");

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
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.any())) //
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
        writeOpts.setDatabase("public");
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
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.any())) //
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
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.eq(Collections.singleton(testTable)))) //
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
                .write(TestUtil.newTableTwoPoints(testTable)) //
                .write(TestUtil.newTableTwoPoints(testTable)) //
                .write(TestUtil.newTableTwoPoints(testTable)) //
                .flush() //
                .write(TestUtil.newTableTwoPoints(testTable)) //
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
        List<Point> table1 = new ArrayList<>();
        table1.add(Point.newPointBuilder("table1") //
                .setTimestamp(Clock.defaultClock().getTick()).addTag("t1_tag1", Value.withString("v1")) //
                .addTag("t1_tag2", Value.withString("v2")) //
                .addTag("t1_tag3", Value.withString("v3")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he"))
                .addField("t1_field3", Value.withStringOrNull(null)).build());
        table1.add(Point.newPointBuilder("table1") //
                .setTimestamp(Clock.defaultClock().getTick()).addTag("t1_tag1", Value.withString("v1")) //
                .addTag("t1_tag2", Value.withString("v2")) //
                .addTag("t1_tag3", Value.withString("v3")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he"))
                .addField("t1_field3", Value.withStringOrNull("surprise!!!")).build());

        List<Point> table2 = new ArrayList<>();
        table2.add(Point.newPointBuilder("table2") //
                .setTimestamp(Clock.defaultClock().getTick()).addTag("t2_tag1", Value.withString("v1")) //
                .addTag("t2_tag2", Value.withString("v2")) //
                .addTag("t2_tag3", Value.withString("v3")) //
                .addField("t2_field1", Value.withDouble(0.1)).addField("t2_field2", Value.withString("he")).build());
        table2.add(Point.newPointBuilder("table2").setTimestamp(Clock.defaultClock().getTick() + 10)
                .addTag("t2_tag1", Value.withString("v1")) //
                .addTag("t2_tag2", Value.withString("v2")) //
                .addTag("t2_tag3", Value.withString("v3")) //
                .addField("t2_field1", Value.withDouble(0.1)).addField("t2_field2", Value.withString("he"))
                .addField("t2_field3", Value.withStringOrNull("surprise!!!")).build());

        final List<Point> table3 = Collections.singletonList(Point.newPointBuilder("table1")
                .setTimestamp(System.currentTimeMillis()).addTag("t1_tag1", Value.withString("vv1")) //
                .addTag("t1_tag2", Value.withString("vv2")) //
                .addField("t1_field1", Value.withDouble(0.1)).addField("t1_field2", Value.withString("he")).build());

        RequestContext reqCtx = new RequestContext();
        reqCtx.setDatabase("public");

        final Storage.WriteRequest writeReq = this.writeClient.toWriteRequestObj(reqCtx,
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
