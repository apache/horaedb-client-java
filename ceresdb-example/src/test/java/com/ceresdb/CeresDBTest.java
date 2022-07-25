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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.UnsignedUtil;
import com.ceresdb.common.util.internal.ThrowUtil;
import com.ceresdb.models.Err;
import com.ceresdb.models.FieldValue;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.QueryRequest;
import com.ceresdb.models.Record;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.Series;
import com.ceresdb.models.SqlResult;
import com.ceresdb.models.TagValue;
import com.ceresdb.models.WriteOk;
import com.ceresdb.options.CeresDBxOptions;
import com.ceresdb.rpc.RpcOptions;

/**
 *
 * @author jiachun.fjc
 */
public class CeresDBTest {

    private static final Logger LOG = LoggerFactory.getLogger(CeresDBTest.class);

    private String TEST_TABLE_NAME = "all_type_test_table_%d";

    private CeresDBxOptions opts;
    private CeresDBxClient  client;

    @Before
    public void before() {
        final RpcOptions rpcOpts = RpcOptions.newDefault();
        rpcOpts.setBlockOnLimit(false);
        rpcOpts.setInitialLimit(32);
        rpcOpts.setLimitKind(RpcOptions.LimitKind.Gradient);
        rpcOpts.setLogOnLimitChange(true);
        this.opts = CeresDBxOptions.newBuilder("127.0.0.1", 8831, 5000) //
                .tenant("public", "sub_test", "test_token") //
                .rpcOptions(rpcOpts) //
                .writeMaxRetries(0) //
                .readMaxRetries(1) //
                .build();
        this.client = new CeresDBxClient();
        final boolean ret = this.client.init(this.opts);

        TEST_TABLE_NAME = String.format(TEST_TABLE_NAME, System.currentTimeMillis());

        final Management management = this.client.management();

        final SqlResult existsResult = management.executeSql("EXISTS TABLE %s", TEST_TABLE_NAME);
        LOG.info("EXISTS TABLE before: {}.", existsResult);

        final SqlResult result = management.executeSql(String.format("CREATE TABLE %s(" + //
                                                                     "ts TIMESTAMP NOT NULL," + //
                                                                     "c1 STRING TAG NOT NULL," + //
                                                                     "c2 INT64 TAG NULL," + //
                                                                     "c3 DOUBLE NULL," + //
                                                                     "c4 STRING NULL," + //
                                                                     "c5 INT64 NULL," + //
                                                                     "c6 FLOAT NULL," + //
                                                                     "c7 INT32 NULL," + //
                                                                     "c8 INT16 NULL," + //
                                                                     "c9 INT8 NULL," + //
                                                                     "c10 BOOLEAN NULL," + //
                                                                     "c11 UINT64 NULL," + //
                                                                     "c12 UINT32 NULL," + //
                                                                     "c13 UINT16 NULL," + //
                                                                     "c14 UINT8 NULL," + //
                                                                     "c15 TIMESTAMP NULL," + //
                                                                     "c16 VARBINARY NULL," + //
                                                                     "TIMESTAMP KEY(ts)) ENGINE=Analytic WITH (ttl='7d')",
                TEST_TABLE_NAME));

        LOG.info("Start CeresDBx client {}, with options: {}, create table {}: {}.", result(ret), this.opts,
                TEST_TABLE_NAME, result);

        final SqlResult existsResult2 = management.executeSql("EXISTS TABLE %s", TEST_TABLE_NAME);
        LOG.info("EXISTS TABLE after: {}.", existsResult2);
    }

    private static String result(final boolean result) {
        return result ? "success" : "failed";
    }

    @After
    public void after() {
        final SqlResult descResult = this.client.management().executeSql("DROP TABLE %s", TEST_TABLE_NAME);
        LOG.info("DROP TABLE: {}.", descResult);
        MetricsUtil.reportImmediately();
        this.client.shutdownGracefully();
    }

    public CeresDBxOptions getOpts() {
        return opts;
    }

    @Ignore
    @Test
    public void comprehensiveTest() throws ExecutionException, InterruptedException {
        final Calendar time = Calendar.getInstance();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        final String timeString = format.format(time.getTime());
        System.out.println("time=" + timeString + " " + time.getTimeInMillis() + " " + time.getTime());

        final int writeCount = ThreadLocalRandom.current().nextInt(10);

        final Result<WriteOk, Err> writeR = write(time, writeCount);

        LOG.info("#comprehensiveTest write result={}.", writeR);

        Assert.assertTrue(writeR.isOk());

        final QueryRequest req = QueryRequest.newBuilder() //
                .ql("select * from %s where ts < to_timestamp_millis('%s')", TEST_TABLE_NAME, timeString) //
                .build();

        final Result<QueryOk, Err> queryR = this.client.query(req).get();

        LOG.info("#comprehensiveTest query result={}.", queryR);

        Assert.assertTrue(queryR.isOk());

        final QueryOk ok = queryR.getOk();

        ok.mapToRecord().forEach(rd -> {
            LOG.info("Field descriptor: {}", rd.getFieldDescriptors());
            LOG.info("Data: ts={}, c1={}, c2={}, c3={}, c4={}, c5={}, c6={}, c7={}, c8={}, c9={}," + //
            "c10={}, c11={}, c12={}, c13={}, c14={}, c15={}, c16={}", rd.getTimestamp("ts"), //
                    rd.getString("c1"), //
                    rd.getInt64("c2"), //
                    rd.getDouble("c3"), //
                    rd.getString("c4"), //
                    rd.getInt64("c5"), //
                    rd.getFloat("c6"), //
                    rd.getInteger("c7"), //
                    rd.getInt16("c8"), //
                    rd.getInt8("c9"), //
                    rd.getBoolean("c10"), //
                    rd.getUInt64("c11"), //
                    rd.getUInt32("c12"), //
                    rd.getUInt16("c13"), //
                    rd.getUInt8("c14"), //
                    rd.getTimestamp("c15"), rd.getBytes("c16"));
        });

        final Management management = this.client.management();

        final SqlResult alterResult1 = management.executeSql("ALTER TABLE %s ADD COLUMN (c18 UINT64, c19 STRING TAG)",
                TEST_TABLE_NAME);
        LOG.info("ALTER TABLE 1: {}.", alterResult1);

        final SqlResult alterResult2 = management.executeSql("ALTER TABLE %s ADD COLUMN c20 STRING TAG",
                TEST_TABLE_NAME);
        LOG.info("ALTER TABLE 2: {}.", alterResult2);

        final SqlResult descResult = management.executeSql("DESCRIBE %s", TEST_TABLE_NAME);
        LOG.info("DESCRIBE TABLE: {}.", descResult);

        final SqlResult showResult = management.executeSql("SHOW CREATE TABLE %s", TEST_TABLE_NAME);
        LOG.info("SHOW CREATE TABLE: {}.", showResult);

        final SqlResult queryResult = management.executeSql("SELECT * FROM %s", TEST_TABLE_NAME);
        LOG.info("QUERY TABLE: {}.", queryResult);
    }

    private Result<WriteOk, Err> write(final Calendar time, final int writeCount) {
        try {
            return writeAsync(time, writeCount).get();
        } catch (final Throwable t) {
            ThrowUtil.throwException(t);
        }
        return WriteOk.emptyOk().mapToResult();
    }

    private CompletableFuture<Result<WriteOk, Err>> writeAsync(final Calendar time, final int writeCount) {
        return this.client.write(makeRows(time, writeCount));
    }

    private Collection<Rows> makeRows(final Calendar time, final int count) {
        final long timestamp = time.getTimeInMillis();
        final Collection<Rows> rows = new ArrayList<>();
        for (long ts = (timestamp - count); ts < timestamp; ts++) {
            final Rows.Builder builder = Series.newBuilder(TEST_TABLE_NAME).tag("c1", "first_c1")
                    .tag("c2", TagValue.withInt64(12)).toRowsBuilder() //
                    .fields(ts, in -> {
                        in.put("c3", FieldValue.withDouble(0.1));
                        in.put("c4", FieldValue.withString("string value"));
                        in.put("c5", FieldValue.withInt64(64));
                        in.put("c6", FieldValue.withFloat(1.0f));
                        in.put("c7", FieldValue.withInt(32));
                        in.put("c8", FieldValue.withInt16(16));
                        in.put("c9", FieldValue.withInt8(8));
                        in.put("c10", FieldValue.withBoolean(true));
                        in.put("c11",
                                FieldValue.withUInt64(UnsignedUtil.getUInt64(Long.MAX_VALUE).add(BigInteger.ONE)));
                        in.put("c12", FieldValue.withUInt32(33));
                        in.put("c13", FieldValue.withUInt16(17));
                        in.put("c14", FieldValue.withUInt8(9));
                        in.put("c15", FieldValue.withTimestamp(time.getTimeInMillis()));
                        in.put("c16", FieldValue.withVarbinary("test".getBytes(StandardCharsets.UTF_8)));
                    });

            rows.add(builder.build());
        }

        return rows;
    }

    @Ignore
    @Test
    public void holdConnectionTest() throws ExecutionException, InterruptedException {
        comprehensiveTest();
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(1000);
        }
    }

    @SuppressWarnings("all")
    @Ignore
    @Test
    public void loopToWriteTest() throws Exception {
        final int concurrence = 32;
        final List<CompletableFuture<Result<WriteOk, Err>>> fs = new ArrayList<>();
        for (;;) {
            try {
                for (int i = 0; i < concurrence; i++) {
                    fs.add(writeAsync(Calendar.getInstance(), 32));
                }

                for (final CompletableFuture<Result<WriteOk, Err>> f : fs) {
                    f.get();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                fs.clear();
            }
        }
    }

    @Ignore
    @Test
    public void streamWriteTest() {
        final StreamWriteBuf<Rows, WriteOk> writeBuf = this.client.streamWrite(TEST_TABLE_NAME);
        final CompletableFuture<WriteOk> future = writeBuf.write(makeRows(Calendar.getInstance(), 2)) //
                .write(makeRows(Calendar.getInstance(), 3)) //
                .flush() //
                .writeAndFlush(makeRows(Calendar.getInstance(), 10)) //
                .write(makeRows(Calendar.getInstance(), 10)) //
                .writeAndFlush(makeRows(Calendar.getInstance(), 10)) //
                .completed();

        Assert.assertEquals(35, future.join().getSuccess());
    }

    @Ignore
    @Test
    public void streamQueryTest() {
        final Calendar time = Calendar.getInstance();
        final StreamWriteBuf<Rows, WriteOk> writeBuf = this.client.streamWrite(TEST_TABLE_NAME);
        for (int i = 0; i < 1000; i++) {
            time.add(Calendar.MILLISECOND, 1);
            writeBuf.writeAndFlush(makeRows(time, 1));
        }
        final CompletableFuture<WriteOk> writeOk = writeBuf.completed();
        Assert.assertEquals(1000, writeOk.join().getSuccess());

        final QueryRequest req = QueryRequest.newBuilder().ql("select * from %s", TEST_TABLE_NAME).build();
        final Iterator<Record> it = this.client.blockingStreamQuery(req, 3, TimeUnit.SECONDS);

        int i = 0;
        while (it.hasNext()) {
            LOG.info("The {} row, timestamp={}", ++i, it.next().getTimestamp("ts"));
        }

        Assert.assertEquals(1000, i);
    }
}
