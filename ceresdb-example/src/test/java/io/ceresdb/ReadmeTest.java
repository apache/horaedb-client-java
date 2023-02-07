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
package io.ceresdb;

import io.ceresdb.models.*;
import io.ceresdb.options.CeresDBOptions;
import io.ceresdb.util.StreamWriteBuf;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * @author kesheng
 */
public class ReadmeTest {

    @Ignore
    @Test
    public void readmeTest() throws ExecutionException, InterruptedException {
        final CeresDBOptions opts = CeresDBOptions.newBuilder("127.0.0.1", 8831, RouteMode.DIRECT) // CeresDB default grpc port 8831
                .tenant("public", "sub_test", "test_token") // tenant info
                // maximum retry times when write fails
                // (only some error codes will be retried, such as the routing table failure)
                .writeMaxRetries(1)
                // maximum retry times when read fails
                // (only some error codes will be retried, such as the routing table failure)
                .readMaxRetries(1).build();

        final CeresDBClient client = new CeresDBClient();
        if (!client.init(opts)) {
            throw new IllegalStateException("Fail to start CeresDBClient");
        }

        // Create table manually, creating table schema ahead of data ingestion is not required
        String createTableSql = "CREATE TABLE IF NOT EXISTS machine_table(" +                                                                                              "ts TIMESTAMP NOT NULL," + //
                "ts TIMESTAMP NOT NULL," +
                "city STRING TAG NOT NULL," +
                "ip STRING TAG NOT NULL," +
                "cpu DOUBLE NULL," +
                "mem DOUBLE NULL," +
                "TIMESTAMP KEY(ts)" + // timestamp column must be specified
                ") ENGINE=Analytic";

        Result<SqlQueryOk, Err> createResult = client.sqlQuery(new SqlQueryRequest(createTableSql)).get();
        if (!createResult.isOk()) {
            throw new IllegalStateException("Fail to create table");
        }

        final long t0 = System.currentTimeMillis();
        final long t1 = t0 + 1000;
        final List<Point> data = Point.newPointsBuilder("machine_table")
                .addPoint() // first point
                    .setTimestamp(t0)
                    .addTag("city", "Singapore")
                    .addTag("ip", "10.0.0.1")
                    .addField("cpu", Value.withFloat64(0.23))
                    .addField("mem", Value.withFloat64(0.55))
                    .build()
                .addPoint() // second point
                    .setTimestamp(t1)
                    .addTag("city", "Singapore")
                    .addTag("ip", "10.0.0.1")
                    .addField("cpu", Value.withFloat64(0.25))
                    .addField("mem", Value.withFloat64(0.56))
                    .build()
                .addPoint()// third point
                    .setTimestamp(t1)
                    .addTag("city", "Shanghai")
                    .addTag("ip", "10.0.0.2")
                    .addField("cpu", Value.withFloat64(0.21))
                    .addField("mem", Value.withFloat64(0.52))
                    .build()
                .build();

        final CompletableFuture<Result<WriteOk, Err>> wf = client.write(new WriteRequest(data));
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<WriteOk, Err> writeResult = wf.get();
        Assert.assertTrue(writeResult.isOk());
        Assert.assertEquals(3, writeResult.getOk().getSuccess());
        // `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
        Assert.assertEquals(3, writeResult.mapOr(0, WriteOk::getSuccess).intValue());
        Assert.assertEquals(0, writeResult.getOk().getFailed());
        Assert.assertEquals(0, writeResult.mapOr(-1, WriteOk::getFailed).intValue());

        final SqlQueryRequest queryRequest = SqlQueryRequest.newBuilder()
                .forTables("machine_table") // table name is optional. If not provided, SQL parser will parse the `ssql` to get the table name and do the routing automaticly
                .sql("select * from machine_table where ts = %d", t0) //
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> qf = client.sqlQuery(queryRequest);
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<SqlQueryOk, Err> queryResult = qf.get();

        Assert.assertTrue(queryResult.isOk());

        final SqlQueryOk queryOk = queryResult.getOk();
        Assert.assertEquals(1, queryOk.getRowCount());

        // get rows as list
        final List<Row> rows = queryOk.getRowList();
        Assert.assertEquals(t0, rows.get(0).getColumnValue("ts").getTimestamp());
        Assert.assertEquals("Singapore", rows.get(0).getColumnValue("city").getString());
        Assert.assertEquals("10.0.0.1", rows.get(0).getColumnValue("ip").getString());
        Assert.assertEquals(0.23, rows.get(0).getColumnValue("cpu").getFloat64(), 0.0000001);
        Assert.assertEquals(0.55, rows.get(0).getColumnValue("mem").getFloat64(), 0.0000001);

        // get rows as stream
        final Stream<Row> rowStream = queryOk.stream();
        rowStream.forEach(row -> System.out.println(row.toString()));

        // write with Stream
        long start = System.currentTimeMillis();
        long t = start;
        final StreamWriteBuf<Point, WriteOk> writeBuf = client.streamWrite("machine_table");
        for (int i = 0; i < 1000; i++) {
            final List<Point> streamData = Point.newPointsBuilder("machine_table")
                    .addPoint()
                        .setTimestamp(t)
                        .addTag("city", "Beijing")
                        .addTag("ip", "10.0.0.3")
                        .addField("cpu", Value.withFloat64(0.42))
                        .addField("mem", Value.withFloat64(0.67))
                        .build()
                    .build();
            writeBuf.writeAndFlush(streamData);
            t = t+1;
        }
        final CompletableFuture<WriteOk> writeOk = writeBuf.completed();
        Assert.assertEquals(1000, writeOk.join().getSuccess());

        final SqlQueryRequest streamQuerySql = SqlQueryRequest.newBuilder()
                .sql("select * from %s where city = '%s' and ts >= %d and ts < %d", "machine_table", "Beijing", start, t).build();
        final Result<SqlQueryOk, Err> streamQueryResult = client.sqlQuery(streamQuerySql).get();
        Assert.assertTrue(streamQueryResult.isOk());
        Assert.assertEquals(1000, streamQueryResult.getOk().getRowCount());
    }

}
