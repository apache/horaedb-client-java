/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.*;
import org.apache.horaedb.models.Row;
import io.ceresdb.proto.internal.Common;
import io.ceresdb.proto.internal.Storage;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TimeStampMilliVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarBinaryVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import io.ceresdb.common.Endpoint;
import io.ceresdb.common.util.internal.ThrowUtil;
import org.apache.horaedb.errors.IteratorException;
import org.apache.horaedb.errors.StreamException;
import org.apache.horaedb.models.Err;
import org.apache.horaedb.models.SqlQueryOk;
import org.apache.horaedb.models.SqlQueryRequest;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.options.QueryOptions;
import org.apache.horaedb.rpc.Context;
import org.apache.horaedb.rpc.Observer;
import org.apache.horaedb.util.Utils;

import com.google.protobuf.ByteStringHelper;

@RunWith(value = MockitoJUnitRunner.class)
public class QueryClientTest {

    private QueryClient  queryClient;
    @Mock
    private RouterClient routerClient;

    @Before
    public void before() {
        final QueryOptions queryOpts = new QueryOptions();
        queryOpts.setAsyncPool(ForkJoinPool.commonPool());
        queryOpts.setRouterClient(this.routerClient);
        queryOpts.setDatabase("public");

        this.queryClient = new QueryClient();
        this.queryClient.init(queryOpts);
    }

    @After
    public void after() {
        this.queryClient.shutdownGracefully();
        this.routerClient.shutdownGracefully();
    }

    @Test
    public void queryOkNoRouteTest() throws ExecutionException, InterruptedException, IOException {
        final Storage.SqlQueryResponse resp = mockSimpleQueryResponse(1, false);
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<>()));
        Mockito.when(this.routerClient.clusterRoute()) //
                .thenReturn(Route.of(ep));

        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("query_test_table") //
                .sql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> f = this.queryClient.sqlQuery(req, Context.newDefault());

        final Result<SqlQueryOk, Err> r = f.get();

        Assert.assertTrue(r.isOk());

        final SqlQueryOk queryOk = r.getOk();

        final Stream<String> strs = queryOk.map(Row::toString);

        Assert.assertEquals(
                Collections.singletonList("t1:Value{type=String,value=tvtest}|f1:Value{type=Int32,value=123}"),
                strs.collect(Collectors.toList()));
    }

    @Test
    public void queryOkByValidRouteTest() throws ExecutionException, InterruptedException, IOException {
        final Storage.SqlQueryResponse resp = mockSimpleQueryResponse(1, false);
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(
                this.routerClient.routeFor(Mockito.any(), Mockito.eq(Collections.singletonList("query_test_table")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -6260265905617276356L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));

        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("query_test_table") //
                .sql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> f = this.queryClient.sqlQuery(req, Context.newDefault());

        final Result<SqlQueryOk, Err> r = f.get();

        Assert.assertTrue(r.isOk());

        final SqlQueryOk queryOk = r.getOk();

        final Stream<String> strs = queryOk.map(Row::toString);

        Assert.assertEquals(
                Collections.singletonList("t1:Value{type=String,value=tvtest}|f1:Value{type=Int32,value=123}"),
                strs.collect(Collectors.toList()));
    }

    @Test
    public void queryFailedTest() throws ExecutionException, InterruptedException, IOException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(500) // failed code
                .build();
        final Storage.SqlQueryResponse resp = Storage.SqlQueryResponse.newBuilder().setHeader(header) //
                .build();
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(
                this.routerClient.routeFor(Mockito.any(), Mockito.eq(Collections.singletonList("query_test_table")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4517371149948738282L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any(), Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 2347114952231996366L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));

        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("query_test_table") //
                .sql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> f = this.queryClient.sqlQuery(req, Context.newDefault());

        final Result<SqlQueryOk, Err> r = f.get();

        Assert.assertFalse(r.isOk());

        final Err err = r.getErr();

        Assert.assertEquals(ep, err.getErrTo());
        Assert.assertEquals(Collections.singletonList("query_test_table"), err.getFailedTables());
    }

    @Test
    public void queryByArrowFullTypeTest() throws ExecutionException, InterruptedException, IOException {
        final Result<SqlQueryOk, Err> r = queryByArrow();
        Assert.assertTrue(r.isOk());
        final SqlQueryOk queryOk = r.getOk();
        queryOk.stream().forEach(this::checkFullTypeRow);
    }

    private void checkFullTypeRow(final Row row) {
        Assert.assertEquals(14, row.getColumnCount());

        Assert.assertEquals("test", row.getColumn("fString").getValue().getString());
        Assert.assertEquals(Boolean.TRUE, row.getColumn("fBool").getValue().getBoolean());
        Assert.assertEquals(0.64, row.getColumn("fDouble").getValue().getDouble(), 0.000001);
        Assert.assertEquals(0.32f, row.getColumn("fFloat").getValue().getFloat(), 0.000001);
        Assert.assertEquals(-64, row.getColumn("fInt64").getValue().getInt64());
        Assert.assertEquals(-32, row.getColumn("fInt32").getValue().getInt32());
        Assert.assertEquals(-16, row.getColumn("fInt16").getValue().getInt16());
        Assert.assertEquals(-8, row.getColumn("fInt8").getValue().getInt8());
        Assert.assertEquals(64, row.getColumn("fUint64").getValue().getUInt64());
        Assert.assertEquals(32, row.getColumn("fUint32").getValue().getUInt32());
        Assert.assertEquals(16, row.getColumn("fUint16").getValue().getUInt16());
        Assert.assertEquals(8, row.getColumn("fUint8").getValue().getUInt8());
        Assert.assertEquals(1675345488158L, row.getColumn("fTimestamp").getValue().getTimestamp());
        Assert.assertArrayEquals(new byte[] { 1, 2, 3 }, row.getColumn("fVarbinary").getValue().getVarbinary());
    }

    @Test
    public void queryByArrowNullTypeTest() throws ExecutionException, InterruptedException, IOException {
        final Storage.SqlQueryResponse resp = mockSimpleQueryResponse(1, true);
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<>()));
        Mockito.when(this.routerClient.clusterRoute()) //
                .thenReturn(Route.of(ep));

        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("query_test_table") //
                .sql("select * from query_test_table") //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> f = this.queryClient.sqlQuery(req, Context.newDefault());

        final Result<SqlQueryOk, Err> r = f.get();

        Assert.assertTrue(r.isOk());

        final SqlQueryOk queryOk = r.getOk();

        Row row = queryOk.stream().findFirst().get();
        Assert.assertEquals("tvtest", row.getColumn("t1").getValue().getString());
        Assert.assertTrue(row.getColumn("f1").getValue().isNull());
    }

    private Result<SqlQueryOk, Err> queryByArrow() throws IOException, ExecutionException, InterruptedException {
        final Storage.SqlQueryResponse resp = mockAllTypeQueryResponse(1);
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<>()));
        Mockito.when(this.routerClient.clusterRoute()) //
                .thenReturn(Route.of(ep));

        final SqlQueryRequest req = SqlQueryRequest.newBuilder().forTables("query_test_table") //
                .sql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> f = this.queryClient.sqlQuery(req, Context.newDefault());

        return f.get();
    }

    private SqlQueryOk mockQueryOk() {
        try {
            Thread.sleep(20);
            return queryByArrow().unwrapOr(SqlQueryOk.emptyOk());
        } catch (final Throwable t) {
            ThrowUtil.throwException(t);
        }
        return null;
    }

    private Storage.SqlQueryResponse mockSimpleQueryResponse(int rowCount, boolean hasNullField) throws IOException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();

        BufferAllocator allocator = new RootAllocator();
        VarCharVector varCharVector = new VarCharVector("t1", allocator);
        IntVector intVector = new IntVector("f1", allocator);

        for (int i = 0; i < rowCount; i++) {
            varCharVector.setSafe(i, "tvtest".getBytes());
            if (hasNullField) {
                intVector.setNull(i);
            } else {
                intVector.setSafe(i, 123);
            }
        }
        varCharVector.setValueCount(rowCount);
        intVector.setValueCount(rowCount);

        List<Field> fields = Arrays.asList(varCharVector.getField(), intVector.getField());
        List<FieldVector> vectors = Arrays.asList(varCharVector, intVector);
        VectorSchemaRoot root = new VectorSchemaRoot(fields, vectors);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArrowStreamWriter writer = new ArrowStreamWriter(root, /*DictionaryProvider=*/null, out);
        writer.writeBatch();
        writer.close();

        Storage.ArrowPayload arrowPayload = Storage.ArrowPayload.newBuilder()
                .setCompression(Storage.ArrowPayload.Compression.NONE)
                .addRecordBatches(ByteStringHelper.wrap(out.toByteArray())).build();

        return Storage.SqlQueryResponse.newBuilder() //
                .setHeader(header) //
                .setArrow(arrowPayload).build();
    }

    private Storage.SqlQueryResponse mockAllTypeQueryResponse(int rowCount) throws IOException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();

        BufferAllocator allocator = new RootAllocator();
        VarCharVector stringVector = new VarCharVector("fString", allocator);
        BitVector boolVector = new BitVector("fBool", allocator);
        Float8Vector doubleVector = new Float8Vector("fDouble", allocator);
        Float4Vector floatVector = new Float4Vector("fFloat", allocator);
        BigIntVector int64Vector = new BigIntVector("fInt64", allocator);
        IntVector int32Vector = new IntVector("fInt32", allocator);
        SmallIntVector int16Vector = new SmallIntVector("fInt16", allocator);
        TinyIntVector int8Vector = new TinyIntVector("fInt8", allocator);
        UInt8Vector uint64Vector = new UInt8Vector("fUint64", allocator);
        UInt4Vector uint32Vector = new UInt4Vector("fUint32", allocator);
        UInt2Vector uint16Vector = new UInt2Vector("fUint16", allocator);
        UInt1Vector uint8Vector = new UInt1Vector("fUint8", allocator);
        TimeStampMilliVector tsVector = new TimeStampMilliVector("fTimestamp", allocator);
        VarBinaryVector varBinaryVector = new VarBinaryVector("fVarbinary", allocator);

        for (int i = 0; i < rowCount; i++) {
            stringVector.setSafe(i, "test".getBytes());
            boolVector.setSafe(i, 1);
            doubleVector.setSafe(i, 0.64);
            floatVector.setSafe(i, 0.32f);
            int64Vector.setSafe(i, -64);
            int32Vector.setSafe(i, -32);
            int16Vector.setSafe(i, -16);
            int8Vector.setSafe(i, -8);
            uint64Vector.setSafe(i, 64);
            uint32Vector.setSafe(i, 32);
            uint16Vector.setSafe(i, 16);
            uint8Vector.setSafe(i, 8);
            tsVector.setSafe(i, 1675345488158L);
            varBinaryVector.setSafe(i, new byte[] { 1, 2, 3 });
        }
        stringVector.setValueCount(rowCount);
        boolVector.setValueCount(rowCount);
        doubleVector.setValueCount(rowCount);
        floatVector.setValueCount(rowCount);
        int64Vector.setValueCount(rowCount);
        int32Vector.setValueCount(rowCount);
        int16Vector.setValueCount(rowCount);
        int8Vector.setValueCount(rowCount);
        uint64Vector.setValueCount(rowCount);
        uint32Vector.setValueCount(rowCount);
        uint16Vector.setValueCount(rowCount);
        uint8Vector.setValueCount(rowCount);
        tsVector.setValueCount(rowCount);
        varBinaryVector.setValueCount(rowCount);

        List<Field> fields = Arrays.asList(stringVector.getField(), boolVector.getField(), doubleVector.getField(),
                floatVector.getField(), int64Vector.getField(), int32Vector.getField(), int16Vector.getField(),
                int8Vector.getField(), uint64Vector.getField(), uint32Vector.getField(), uint16Vector.getField(),
                uint8Vector.getField(), tsVector.getField(), varBinaryVector.getField());

        List<FieldVector> vectors = Arrays.asList(stringVector, boolVector, doubleVector, floatVector, int64Vector,
                int32Vector, int16Vector, int8Vector, uint64Vector, uint32Vector, uint16Vector, uint8Vector, tsVector,
                varBinaryVector);
        VectorSchemaRoot root = new VectorSchemaRoot(fields, vectors);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArrowStreamWriter writer = new ArrowStreamWriter(root, /*DictionaryProvider=*/null, out);
        writer.writeBatch();
        writer.close();

        Storage.ArrowPayload arrowPayload = Storage.ArrowPayload.newBuilder()
                .setCompression(Storage.ArrowPayload.Compression.NONE)
                .addRecordBatches(ByteStringHelper.wrap(out.toByteArray())).build();

        return Storage.SqlQueryResponse.newBuilder() //
                .setHeader(header) //
                .setArrow(arrowPayload).build();
    }

    @Test
    public void blockingStreamQueryTest() {
        final SqlQueryOk data = mockQueryOk();
        final int respCount = 10;
        final BlockingStreamIterator streams = new BlockingStreamIterator(1000, TimeUnit.MILLISECONDS);
        final Observer<SqlQueryOk> obs = streams.getObserver();
        new Thread(() -> {
            for (int i = 0; i < respCount; i++) {
                if (i == 0) {
                    obs.onNext(data);
                } else {
                    obs.onNext(mockQueryOk());
                }
            }
            obs.onCompleted();
        }).start();

        final Iterator<Row> it = new RowIterator(streams);

        int i = 0;
        while (it.hasNext()) {
            i++;
            checkFullTypeRow(it.next());
        }
        Assert.assertEquals(respCount, i);
    }

    @Test(expected = IteratorException.class)
    public void blockingStreamQueryOnErrTest() {
        final int respCount = 10;
        final BlockingStreamIterator streams = new BlockingStreamIterator(1000, TimeUnit.MILLISECONDS);
        final Observer<SqlQueryOk> obs = streams.getObserver();
        new Thread(() -> {
            for (int i = 0; i < respCount; i++) {
                if (i == 5) {
                    obs.onError(new StreamException("fail to stream query"));
                } else {
                    obs.onNext(mockQueryOk());
                }
            }
            obs.onCompleted();
        }).start();

        final Iterator<Row> it = new RowIterator(streams);

        int i = 0;
        while (it.hasNext()) {
            i++;
            checkFullTypeRow(it.next());
        }
        Assert.assertEquals(respCount, i);
    }
}
