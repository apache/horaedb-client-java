# HoraeDB Java Client

[![build](https://github.com/CeresDB/horaedb-client-java/actions/workflows/build.yml/badge.svg)](https://github.com/CeresDB/horaedb-client-java/actions/workflows/build.yml)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)

## 介绍
HoraeDBClient 是 HoraeDB 的高性能 Java 版客户端。HoraeDB 是定位为高性能的、分布式的、Schema-less 的云原生时序数据库。它可以同时支持时间序列和数据分析型的工作负载。

## 功能特性
- 通信层基于 SPI 可扩展，默认提供 gRPC 的实现
- 提供纯异步的流式高性能写入 API
- 默认提供丰富的性能指标采集，可输指标统计到本地文件
- 支持关键对象内存状态快照或配置输出到本地文件以协助排查问题

## 写入流程图

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
│   HoraeDB Node1     │      │   HoraeDB  Node2    │      │         ...         │  
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘  
```

## 查询流程
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

## 需要
编译需要 Java 8 及以上

## 引用
```java
<dependency>
  <groupId>io.ceresdb</groupId>
  <artifactId>horaedb-all</artifactId>
  <version>1.0.0-alpha</version>
</dependency>
```

## 初始化 HoraeDB Client
```java
// HoraeDB options
final HoraeOptions opts = HoraeOptions.newBuilder("127.0.0.1", 8831, DIRECT) // 默认 gprc 端口号，DIRECT 模式
        .database("public") // Client所使用的database，可被RequestContext的database覆盖
        .writeMaxRetries(1) // 写入失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
        .readMaxRetries(1)  // 查询失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
        .build();

final HoraeClient client = new HoraeClient();
if (!client.init(opts)) {
        throw new IllegalStateException("Fail to start HoraeClient");
}
```
配置详情见 [configuration](docs/configuration.md)


## 建表 Example
HoraeDB 是一个 Schema-less 的时序数据引擎，你可以不必创建 schema 就立刻写入数据（HoraeDB 会根据你的第一次写入帮你创建一个默认的 schema）。
当然你也可以自行创建一个 schema 来更精细化的管理的表（比如索引等）

下面的建表语句（使用 SDK 的 SQL API）包含了 HoraeDB 支持的所有字段类型：

```java
String createTableSql = "CREATE TABLE IF NOT EXISTS machine_table(" +                                                                                              "ts TIMESTAMP NOT NULL," + //
        "ts TIMESTAMP NOT NULL," +
        "city STRING TAG NOT NULL," +
        "ip STRING TAG NOT NULL," +
        "cpu DOUBLE NULL," +
        "mem DOUBLE NULL," +
        "TIMESTAMP KEY(ts)" + // 建表时必须指定时间戳序列
        ") ENGINE=Analytic";

Result<SqlQueryOk, Err> createResult = client.sqlQuery(new SqlQueryRequest(createTableSql)).get();
if (!createResult.isOk()) {
        throw new IllegalStateException("Fail to create table");
}
```
详情见 [table](docs/table.md)

## 构建写入数据
```java
final Point point = Point.newPointBuilder("machine_table")
        .setTimestamp(t0)
        .addTag("city", "Singapore")
        .addTag("ip", "10.0.0.1")
        .addField("cpu", Value.withDouble(0.23))
        .addField("mem", Value.withDouble(0.55))
        .build();
```

## 写入 Example
```java
final CompletableFuture<Result<WriteOk, Err>> wf = client.write(new WriteRequest(pointList));
// 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
final Result<WriteOk, Err> writeResult = wf.get();

Assert.assertTrue(writeResult.isOk());
Assert.assertEquals(3, writeResult.getOk().getSuccess());
// `Result` 类参考了 Rust 语言，提供了丰富的 mapXXX、andThen 类 function 方便对结果值进行转换，提高编程效率，欢迎参考 API 文档使用
Assert.assertEquals(3, writeResult.mapOr(0, WriteOk::getSuccess).intValue());
Assert.assertEquals(0, writeResult.mapOr(-1, WriteOk::getFailed).intValue());
```
详情见 [write](docs/write.md)

## 查询 Example
```java
final SqlQueryRequest queryRequest = SqlQueryRequest.newBuilder()
        .forTables("machine_table") // 这里表名是可选的，如果未提供，SDK将自动解析SQL填充表名并自动路由
        .sql("select * from machine_table where ts = %d", t0) //
        .build();
final CompletableFuture<Result<SqlQueryOk, Err>> qf = client.sqlQuery(queryRequest);
// 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
final Result<SqlQueryOk, Err> queryResult = qf.get();

Assert.assertTrue(queryResult.isOk());

final SqlQueryOk queryOk = queryResult.getOk();
Assert.assertEquals(1, queryOk.getRowCount());

// 直接获取结果数组
final List<Row> rows = queryOk.getRowList();

// 获取结果流
final Stream<Row> rowStream = queryOk.stream();
rowStream.forEach(row -> System.out.println(row.toString()));
```
详情见 [read](docs/read.md)

## 流式读写 Example
HoraeDB 支持流式读写，适用于大规模数据读写。
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
详情见 [streaming](docs/streaming.md)


## Licensing
遵守 [Apache License 2.0](./LICENSE).

## 社区与技术支持
- 加入 Slack 社区与用户群 [Slack 入口](https://join.slack.com/t/ceresdbcommunity/shared_invite/zt-1au1ihbdy-5huC9J9s2462yBMIWmerTw)
- 加入微信社区与用户群 [微信二维码](https://github.com/CeresDB/assets/blob/main/WeChatQRCode.jpg)
- 搜索并加入钉钉社区与用户群 44602802
