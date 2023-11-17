# CeresDB Java Client

[![build](https://github.com/CeresDB/Horaedb-java-client/actions/workflows/build.yml/badge.svg)](https://github.com/CeresDB/Horaedb-java-client/actions/workflows/build.yml)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)

[中文](./README_CN.md)

## Introduction
HoraeDB Client is a high-performance Java client for HoraeDB.
HoraeDB is a high-performance, distributed, schema-less, cloud native time-series database that can handle both time-series and analytics workloads.

## Features
- With the well designed SPI, the network transport layer is extensible. And we provide the default implementation which uses the gRPC framework.
- The client provides high-performance async streaming write API.
- The client also collects lots of performance metrics by default. These metrics can be configured to write to local file.
- We can take memory snapshots that contains the status of critical objects. The snapshots can also be configured to write to local file, which helps a lot when we diagnose complex problems.

## Data ingestion process

```
                   ┌─────────────────────┐  
                   │   HoraeDBClient     │  
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
│   HoraeDB Node1     │      │   HoraeDB Node2     │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Data query process
```
                   ┌─────────────────────┐  
                   │   HoraeDBClient     │  
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
│   HoraeDB Node1     │      │   HoraeDB Node2     │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## Requirements
- Java 8 or later is required for compilation 

## Import
```java
<dependency>
  <groupId>io.ceresdb</groupId>
  <artifactId>ceresdb-all</artifactId>
  <version>1.0.0-alpha</version>
</dependency>
```

## Init HoraeDB client
```java
final CeresDBOptions opts = CeresDBOptions.newBuilder("127.0.0.1", 8831, DIRECT) // CeresDB default grpc port 8831，use DIRECT RouteMode
        .database("public") // use database for client, can be overridden by the RequestContext in request
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
HoraeDB is a Schema-less time-series database, so creating table schema ahead of data ingestion is not required (HoraeDB will create a default schema according to the very first data you write into it). Of course, you can also manually create a schema for fine grained management purposes (eg. managing index).

The following table creation statement（using the SQL API included in SDK ）shows all field types supported by HoraeDB：

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

## How to build write data
```java
final Point point = Point.newPointBuilder("machine_table")
        .setTimestamp(t0)
        .addTag("city", "Singapore")
        .addTag("ip", "10.0.0.1")
        .addField("cpu", Value.withDouble(0.23))
        .addField("mem", Value.withDouble(0.55))
        .build();
```

## Write data example
```java
final CompletableFuture<Result<WriteOk, Err>> wf = client.write(new WriteRequest(pointList));
// here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
final Result<WriteOk, Err> writeResult = wf.get();
Assert.assertTrue(writeResult.isOk());
// `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
Assert.assertEquals(3, writeResult.getOk().getSuccess());
Assert.assertEquals(3, writeResult.mapOr(0, WriteOk::getSuccess).intValue());
Assert.assertEquals(0, writeResult.mapOr(-1, WriteOk::getFailed).intValue());
```
See [write](docs/write.md)

## Query data example
```java
final SqlQueryRequest queryRequest = SqlQueryRequest.newBuilder()
        .forTables("machine_table") // table name is optional. If not provided, SQL parser will parse the `sql` to get the table name and do the routing automaticly
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

// get rows as stream
final Stream<Row> rowStream = queryOk.stream();
rowStream.forEach(row -> System.out.println(row.toString()));
```
See [read](docs/read.md)

## stream write/read Example
HoraeDB support streaming writing and reading，suitable for large-scale data reading and writing。
```java
final StreamWriteBuf<Point, WriteOk> writeBuf = client.streamWrite("machine_table");
for (int i = 0; i < 1000; i++) {
    final Point point = Point.newPointBuilder("machine_table")
        .setTimestamp(timestamp)
        .addTag("city", "Beijing")
        .addTag("ip", "10.0.0.3")
        .addField("cpu", Value.withDouble(0.42))
        .addField("mem", Value.withDouble(0.67))
        .build();
        writeBuf.writeAndFlush(Arrays.asList(point));
        timestamp = timestamp+1;
}

final CompletableFuture<WriteOk> writeOk = writeBuf.completed();
```
See [streaming](docs/streaming.md)

## Licensing
Under [Apache License 2.0](./LICENSE).

## Community and support
- Join the user group on [Slack](https://join.slack.com/t/ceresdbcommunity/shared_invite/zt-1au1ihbdy-5huC9J9s2462yBMIWmerTw)
- Join the use group on WeChat [WeChat QR code](https://github.com/CeresDB/assets/blob/main/WeChatQRCode.jpg)
- Join the user group on DingTalk: 44602802
