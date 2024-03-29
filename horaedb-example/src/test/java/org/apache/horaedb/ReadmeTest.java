/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb;

import io.ceresdb.models.*;
import org.apache.HoraeDBClient;
import org.apache.RouteMode;
import org.apache.horaedb.models.*;
import org.apache.models.*;
import org.apache.horaedb.options.HoraeDBOptions;
import org.apache.horaedb.util.StreamWriteBuf;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class ReadmeTest {

    @Ignore
    @Test
    public void readmeTest() throws ExecutionException, InterruptedException {
        final HoraeDBOptions opts = HoraeDBOptions.newBuilder("127.0.0.1", 8831, RouteMode.DIRECT) // CeresDB default grpc port 8831
                .database("public") // use database public
                // maximum retry times when write fails
                // (only some error codes will be retried, such as the routing table failure)
                .writeMaxRetries(1)
                // maximum retry times when read fails
                // (only some error codes will be retried, such as the routing table failure)
                .readMaxRetries(1)
                .build();

        final HoraeDBClient client = new HoraeDBClient();
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

        final long timestamp = System.currentTimeMillis();
        Point point1 = Point.newPointBuilder("machine_table")
                    .setTimestamp(timestamp)
                    .addTag("city", "Singapore")
                    .addTag("ip", "10.0.0.1")
                    .addField("cpu", Value.withDouble(0.23))
                    .addField("mem", Value.withDouble(0.55))
                    .build();
        Point point2 = Point.newPointBuilder("machine_table")
                    .setTimestamp(timestamp+1000)
                    .addTag("city", "Singapore")
                    .addTag("ip", "10.0.0.1")
                    .addField("cpu", Value.withDouble(0.25))
                    .addField("mem", Value.withDouble(0.56))
                    .build();
        Point point3 = Point.newPointBuilder("machine_table")
                    .setTimestamp(timestamp+1000)
                    .addTag("city", "Shanghai")
                    .addTag("ip", "10.0.0.2")
                    .addField("cpu", Value.withDouble(0.21))
                    .addField("mem", Value.withDouble(0.52))
                    .build();
        List<Point> pointList = Arrays.asList(point1, point2, point3);

        final CompletableFuture<Result<WriteOk, Err>> wf = client.write(new WriteRequest(pointList));
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<WriteOk, Err> writeResult = wf.get();
        Assert.assertTrue(writeResult.isOk());
        Assert.assertEquals(3, writeResult.getOk().getSuccess());
        // `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
        Assert.assertEquals(3, writeResult.mapOr(0, WriteOk::getSuccess).intValue());
        Assert.assertEquals(0, writeResult.getOk().getFailed());
        Assert.assertEquals(0, writeResult.mapOr(-1, WriteOk::getFailed).intValue());

        final SqlQueryRequest queryRequest = SqlQueryRequest.newBuilder()
                .forTables("machine_table") // table name is optional. If not provided, SQL parser will parse the `sql` to get the table name and do the routing automaticly
                .sql("select * from machine_table where ts >= %d", timestamp)
                .build();
        final CompletableFuture<Result<SqlQueryOk, Err>> qf = client.sqlQuery(queryRequest);
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<SqlQueryOk, Err> queryResult = qf.get();

        Assert.assertTrue(queryResult.isOk());

        final SqlQueryOk queryOk = queryResult.getOk();
        Assert.assertEquals(3, queryOk.getRowCount());

        // get rows as stream
        final Stream<Row> rowStream = queryOk.stream();
        rowStream.forEach(row -> System.out.println(row.toString()));

        // write with Stream
        long start = System.currentTimeMillis();
        long t = start;
        final StreamWriteBuf<Point, WriteOk> writeBuf = client.streamWrite("machine_table");
        for (int i = 0; i < 1000; i++) {
            final Point streamData = Point.newPointBuilder("machine_table")
                        .setTimestamp(t)
                        .addTag("city", "Beijing")
                        .addTag("ip", "10.0.0.3")
                        .addField("cpu", Value.withDouble(0.42))
                        .addField("mem", Value.withDouble(0.67))
                    .build();
            writeBuf.writeAndFlush(Collections.singletonList(streamData));
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
