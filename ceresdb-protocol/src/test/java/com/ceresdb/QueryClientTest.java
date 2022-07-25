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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ceresdb.common.Endpoint;
import com.ceresdb.common.util.internal.ThrowUtil;
import com.ceresdb.errors.IteratorException;
import com.ceresdb.errors.StreamException;
import com.ceresdb.models.Err;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.QueryRequest;
import com.ceresdb.models.Record;
import com.ceresdb.models.Result;
import com.ceresdb.options.QueryOptions;
import com.ceresdb.proto.Common;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.google.protobuf.ByteStringHelper;

/**
 * @author jiachun.fjc
 */
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

        this.queryClient = new QueryClient();
        this.queryClient.init(queryOpts);
    }

    @After
    public void after() {
        this.queryClient.shutdownGracefully();
        this.routerClient.shutdownGracefully();
    }

    @Test
    public void queryOkNoRouteTest() throws ExecutionException, InterruptedException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        final Storage.QueryResponse resp = Storage.QueryResponse.newBuilder().setHeader(header) //
                .addRows(ByteStringHelper.wrap(new byte[] { 1, 2, 3 })) //
                .build();
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<>()));
        Mockito.when(this.routerClient.clusterRoute()) //
                .thenReturn(Route.of(ep));

        final QueryRequest req = QueryRequest.newBuilder().forMetrics("query_test_table") //
                .ql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> f = this.queryClient.query(req, Context.newDefault());

        final Result<QueryOk, Err> r = f.get();

        Assert.assertTrue(r.isOk());

        final QueryOk queryOk = r.getOk();

        final Stream<String> strs = queryOk.map(Arrays::toString);

        Assert.assertEquals(Collections.singletonList("[1, 2, 3]"), strs.collect(Collectors.toList()));
    }

    @Test
    public void queryOkByValidRouteTest() throws ExecutionException, InterruptedException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        final Storage.QueryResponse resp = Storage.QueryResponse.newBuilder().setHeader(header) //
                .addRows(ByteStringHelper.wrap(new byte[] { 1, 2, 3 })) //
                .build();
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(Collections.singletonList("query_test_table")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = -6260265905617276356L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));

        final QueryRequest req = QueryRequest.newBuilder().forMetrics("query_test_table") //
                .ql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> f = this.queryClient.query(req, Context.newDefault());

        final Result<QueryOk, Err> r = f.get();

        Assert.assertTrue(r.isOk());

        final QueryOk queryOk = r.getOk();

        final Stream<String> strs = queryOk.map(Arrays::toString);

        Assert.assertEquals(Collections.singletonList("[1, 2, 3]"), strs.collect(Collectors.toList()));
    }

    @Test
    public void queryFailedTest() throws ExecutionException, InterruptedException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(500) // failed code
                .build();
        final Storage.QueryResponse resp = Storage.QueryResponse.newBuilder().setHeader(header) //
                .build();
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.eq(Collections.singletonList("query_test_table")))) //
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 4517371149948738282L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));
        Mockito.when(this.routerClient.routeRefreshFor(Mockito.any()))
                .thenReturn(Utils.completedCf(new HashMap<String, Route>() {
                    private static final long serialVersionUID = 2347114952231996366L;

                    {
                        put("query_test_table", Route.of("query_test_table", ep));
                    }
                }));

        final QueryRequest req = QueryRequest.newBuilder().forMetrics("query_test_table") //
                .ql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> f = this.queryClient.query(req, Context.newDefault());

        final Result<QueryOk, Err> r = f.get();

        Assert.assertFalse(r.isOk());

        final Err err = r.getErr();

        Assert.assertEquals(ep, err.getErrTo());
        Assert.assertEquals(Collections.singletonList("query_test_table"), err.getFailedMetrics());
    }

    @Test
    public void queryByAvroAndReturnGenericRecordTest() throws ExecutionException, InterruptedException, IOException {
        final Result<QueryOk, Err> r = queryByAvro();
        Assert.assertTrue(r.isOk());
        final QueryOk queryOk = r.getOk();
        queryOk.mapToRecord().forEach(this::checkRecord);
    }

    private void checkRecord(final Record row) {
        Assert.assertEquals(7, row.getFieldCount());
        Assert.assertEquals("f1", row.getFieldDescriptors().get(0).getName());
        Assert.assertEquals("f2", row.getFieldDescriptors().get(1).getName());
        Assert.assertEquals("f3", row.getFieldDescriptors().get(2).getName());
        Assert.assertEquals("f4", row.getFieldDescriptors().get(3).getName());
        Assert.assertEquals("f5", row.getFieldDescriptors().get(4).getName());
        Assert.assertEquals("f6", row.getFieldDescriptors().get(5).getName());
        Assert.assertEquals("f7", row.getFieldDescriptors().get(6).getName());

        Assert.assertEquals(Record.Type.String, row.getFieldDescriptors().get(0).getType().getType());
        Assert.assertEquals(Record.Type.Int, row.getFieldDescriptors().get(1).getType().getType());
        Assert.assertEquals(Record.Type.Long, row.getFieldDescriptors().get(2).getType().getType());
        Assert.assertEquals(Record.Type.Float, row.getFieldDescriptors().get(3).getType().getType());
        Assert.assertEquals(Record.Type.Double, row.getFieldDescriptors().get(4).getType().getType());
        Assert.assertEquals(Record.Type.Boolean, row.getFieldDescriptors().get(5).getType().getType());
        Assert.assertEquals(Record.Type.Null, row.getFieldDescriptors().get(6).getType().getType());

        Assert.assertEquals("a string value", row.getString("f1"));
        Assert.assertEquals(new Integer(100), row.getInteger("f2"));
        Assert.assertEquals(new Long(200), row.getLong("f3"));
        Assert.assertEquals(new Float(2.6), row.getFloat("f4"));
        Assert.assertEquals(new Double(3.75), row.getDouble("f5"));
        Assert.assertEquals(Boolean.TRUE, row.getBoolean("f6"));
        Assert.assertNull(row.get("f7"));

        Assert.assertEquals("a string value", row.getString(0));
        Assert.assertEquals(new Integer(100), row.getInteger(1));
        Assert.assertEquals(new Long(200), row.getLong(2));
        Assert.assertEquals(new Float(2.6), row.getFloat(3));
        Assert.assertEquals(new Double(3.75), row.getDouble(4));
        Assert.assertEquals(Boolean.TRUE, row.getBoolean(5));
        Assert.assertNull(row.get(6));
    }

    @Test
    public void queryByAvroAndReturnObjectsTest() throws ExecutionException, InterruptedException, IOException {
        final Result<QueryOk, Err> r = queryByAvro();
        Assert.assertTrue(r.isOk());
        final QueryOk queryOk = r.getOk();
        queryOk.mapToArray().forEach(row -> {
            Assert.assertEquals(7, row.length);
            Assert.assertEquals("a string value", String.valueOf(row[0]));
            Assert.assertEquals(100, row[1]);
            Assert.assertEquals(200L, row[2]);
            Assert.assertEquals((float) 2.6, row[3]);
            Assert.assertEquals(3.75, row[4]);
            Assert.assertEquals(Boolean.TRUE, row[5]);
            Assert.assertNull(row[6]);
        });
    }

    private Result<QueryOk, Err> queryByAvro() throws IOException, ExecutionException, InterruptedException {
        final Storage.QueryResponse resp = mockQueryResponse();
        final Endpoint ep = Endpoint.of("127.0.0.1", 8081);

        Mockito.when(this.routerClient.invoke(Mockito.eq(ep), Mockito.any(), Mockito.any())) //
                .thenReturn(Utils.completedCf(resp));
        Mockito.when(this.routerClient.routeFor(Mockito.any())) //
                .thenReturn(Utils.completedCf(new HashMap<>()));
        Mockito.when(this.routerClient.clusterRoute()) //
                .thenReturn(Route.of(ep));

        final QueryRequest req = QueryRequest.newBuilder().forMetrics("query_test_table") //
                .ql("select number from query_test_table") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> f = this.queryClient.query(req, Context.newDefault());

        return f.get();
    }

    private QueryOk mockQueryOk() {
        try {
            Thread.sleep(20);
            return queryByAvro().unwrapOr(QueryOk.emptyOk());
        } catch (final Throwable t) {
            ThrowUtil.throwException(t);
        }
        return null;
    }

    private Storage.QueryResponse mockQueryResponse() throws IOException {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        final String userSchema = "{\"type\":\"record\"," + "\"name\":\"my_record\"," + "\"fields\":[" //
                                  + "{\"name\":\"f1\",\"type\":\"string\"}," //
                                  + "{\"name\":\"f2\",\"type\":\"int\"}," //
                                  + "{\"name\":\"f3\",\"type\":\"long\"}," //
                                  + "{\"name\":\"f4\",\"type\":\"float\"}," //
                                  + "{\"name\":\"f5\",\"type\":\"double\"}," //
                                  + "{\"name\":\"f6\",\"type\":\"boolean\"}," //
                                  + "{\"name\":\"f7\",\"type\":\"null\"}" //
                                  + "]}";
        final Schema.Parser parser = new Schema.Parser();
        final Schema schema = parser.parse(userSchema);
        final GenericRecord record = new GenericData.Record(schema);
        record.put("f1", "a string value");
        record.put("f2", 100);
        record.put("f3", 200L);
        record.put("f4", 2.6f);
        record.put("f5", 3.75d);
        record.put("f6", true);
        final DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
        datumWriter.write(record, encoder);
        encoder.flush();
        baos.flush();

        return Storage.QueryResponse.newBuilder() //
                .setHeader(header) //
                .setSchemaType(Storage.QueryResponse.SchemaType.AVRO) //
                .setSchemaContent(userSchema) //
                .addRows(ByteStringHelper.wrap(baos.toByteArray())) //
                .build();
    }

    @Test
    public void blockingStreamQueryTest() {
        final QueryOk data = mockQueryOk();
        final int respCount = 10;
        final BlockingStreamIterator streams = new BlockingStreamIterator(1000, TimeUnit.MILLISECONDS);
        final Observer<QueryOk> obs = streams.getObserver();
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

        final Iterator<Record> it = new RecordIterator(streams);

        int i = 0;
        while (it.hasNext()) {
            i++;
            checkRecord(it.next());
        }
        Assert.assertEquals(respCount, i);
    }

    @Test(expected = IteratorException.class)
    public void blockingStreamQueryOnErrTest() {
        final int respCount = 10;
        final BlockingStreamIterator streams = new BlockingStreamIterator(1000, TimeUnit.MILLISECONDS);
        final Observer<QueryOk> obs = streams.getObserver();
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

        final Iterator<Record> it = new RecordIterator(streams);

        int i = 0;
        while (it.hasNext()) {
            i++;
            checkRecord(it.next());
        }
        Assert.assertEquals(respCount, i);
    }
}
