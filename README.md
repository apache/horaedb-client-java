# CeresDB Java Client

[![build](https://github.com/CeresDB/ceresdb-java-client/actions/workflows/build.yml/badge.svg)](https://github.com/CeresDB/ceresdb-java-client/actions/workflows/build.yml)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)

[中文](./README_CN.md)

## Introduction
CeresDB Client is a high-performance Java client for CeresDB.
CeresDB is a high-performance, distributed, schema-less, cloud native time-series database that can handle both time-series and analytics workloads.

## Features
- With the well designed SPI, the network transport layer is extensible. And we provide the default implementation which uses the gRPC framework.
- The client provides high-performance async streaming write API.
- The client also collects lots of performance metrics by default. These metrics can be configured to write to local file.
- We can take memory snapshots that contains the status of critical objects. The snapshots can also be configured to write to local file, which helps a lot when we diagnose complex problems.

## Data ingestion process

```
                   ┌─────────────────────┐  
                   │   CeresDBClient     │  
                   └─────────────────────┘  
                              │  
                              ▼  
                   ┌─────────────────────┐  
                   │     WriteClient     │───┐  
                   └─────────────────────┘   │  
                              │     Async to retry and merge responses  
                              │              │  
                 ┌────Split requests         │  
                 │                           │  
                 │  ┌─────────────────────┐  │   ┌─────────────────────┐       ┌─────────────────────┐
                 └─▶│    RouterClient     │◀─┴──▶│     RouterCache     │◀─────▶│      RouterFor      │
                    └─────────────────────┘      └─────────────────────┘       └─────────────────────┘
                               ▲                                                          │  
                               │                                                          │  
                               ▼                                                          │  
                    ┌─────────────────────┐                                               │  
                    │      RpcClient      │◀──────────────────────────────────────────────┘  
                    └─────────────────────┘  
                               ▲  
                               │  
                               ▼  
                    ┌─────────────────────┐  
                    │  Default gRPC impl  │  
                    └─────────────────────┘  
                               ▲  
                               │  
           ┌───────────────────┴ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐  
           │                            │  
           ▼                            ▼                            ▼  
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐  
│   CeresDB Node1     │      │   CeresDB Node2     │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Data query process
```
                   ┌─────────────────────┐  
                   │   CeresDBClient     │  
                   └─────────────────────┘  
                              │  
                              ▼  
                   ┌─────────────────────┐  
                   │     QueryClient     │───┐  
                   └─────────────────────┘   │  
                              │              │Async to retry  
                              │              │  
                 ┌────────────┘              │  
                 │                           │  
                 │  ┌─────────────────────┐  │   ┌─────────────────────┐       ┌─────────────────────┐
                 └─▶│    RouterClient     │◀─┴──▶│     RouterCache     │◀─────▶│      RouterFor      │
                    └─────────────────────┘      └─────────────────────┘       └─────────────────────┘
                               ▲                                                          │  
                               │                                                          │  
                               ▼                                                          │  
                    ┌─────────────────────┐                                               │  
                    │      RpcClient      │◀──────────────────────────────────────────────┘  
                    └─────────────────────┘  
                               ▲  
                               │  
                               ▼  
                    ┌─────────────────────┐  
                    │  Default gRPC impl  │  
                    └─────────────────────┘  
                               ▲  
                               │  
           ┌───────────────────┴ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐  
           │                            │  
           ▼                            ▼                            ▼  
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐  
│   CeresDB Node1     │      │   CeresDB Node2     │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Requirements
- Java 8 or later is required for compilation 

## Import
```java
<dependency>
  <groupId>io.ceresdb</groupId>
  <artifactId>ceresdb-all</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Init CeresDB client
```java
final CeresDBOptions opts = CeresDBOptions.newBuilder("30.54.154.64", 8831) // CeresDB default grpc port 8831
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
```
For more configuration options, see [configuration](docs/configuration.md)

## Create table example
CeresDB is a Schema-less time-series database, so creating table schema ahead of data ingestion is not required (CeresDB will create a default schema according to the very first data you write into it). Of course, you can also manually create a schema for fine grained management purposes (eg. managing index).

The following table creation statement（using the SQL API included in SDK ）shows all field types supported by CeresDB：

```java
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
```

## Write data example
```java
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
// `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
Assert.assertEquals(3, writeResult.getOk().getSuccess());
Assert.assertEquals(3, writeResult.mapOr(0, WriteOk::getSuccess).intValue());
Assert.assertEquals(0, writeResult.getOk().getFailed());
Assert.assertEquals(0, writeResult.mapOr(-1, WriteOk::getFailed).intValue());
```
See [write](docs/write.md)

## Query data example
```java
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
```
See [read](docs/read.md)

## stream write/read Example
CeresDB support streaming writing and reading，suitable for large-scale data reading and writing。
```java
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
```
See [streaming](docs/streaming.md)

## Licensing
Under [Apache License 2.0](./LICENSE).

## Community and support
- Join the user group on [Slack](https://join.slack.com/t/ceresdbcommunity/shared_invite/zt-1au1ihbdy-5huC9J9s2462yBMIWmerTw)
- Join the use group on WeChat [WeChat QR code](https://github.com/CeresDB/assets/blob/main/WeChatQRCode.jpg)
- Join the user group on DingTalk: 44602802
