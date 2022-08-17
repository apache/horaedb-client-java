
## 写入流程图

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
                    │  Default GRPC impl  │  
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

## 名词解释
- CeresDBClient
    - CeresDB 的 java client 实现，面向使用用户，提供写入、查询等 API
- WriteClient
    - 写数据的默认实现，纯异步写入
    - 包含异步获取路由表，自动拆分请求
    - 支持异步重试以及异步合并多个服务端的响应
- RouterClient
    - 路由表客户端，会在本地维护路由表信息，并从服务端刷新路由表
- RpcClient
    - 一个纯异步的高性能 RPC 客户端，默认传输层基于 gRPC 实现

## 创建表
CeresDB 是一个 schema-less 时序数据库，如果你不指定 schema，会在你第一次写入时自动创建默认的 schema，你也可以自己指定 schema，下面是建表语句的一个例子，包含了所有支持的类型：

```
CREATE TABLE my_first_table(
    ts TIMESTAMP NOT NULL,
    c1 STRING TAG NOT NULL,
    c2 STRING TAG NOT NULL,
    c3 DOUBLE NULL,
    c4 STRING NULL,
    c5 INT64 NULL,
    c6 FLOAT NULL,
    c7 INT32 NULL,
    c8 INT16 NULL,
    c9 INT8 NULL,
    c10 BOOLEAN NULL,
    c11 UINT64 NULL,
    c12 UINT32 NULL,
    c13 UINT16 NULL,
    c14 UINT8 NULL,
    c15 TIMESTAMP NULL,
    c16 VARBINARY NULL,
    TIMESTAMP KEY(ts)
) ENGINE=Analytic
```

## 写入 API 说明

```java
/**
 * Write the data stream to the database.
 *
 * @param data rows
 * @param ctx  the invoke context
 * @return write result
 */
CompletableFuture<Result<WriteOk, Err>> write(Collection<Rows> data, Context ctx);
```

### 参数说明
| name | desc |
| --- | --- |
| `Collection<Rows> data` | 要写入的数据，每一个 Rows 为一条时间线上多个时间点的点的集合，对于按照时间线来写入的场景这个结构将很大程度减少数据传输的大小，不过通常监控场景更多的是写一个横截面，整体上这个结构是一个综合考虑的结果。|
| `Context ctx` | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata |

### 返回值说明
CompletableFuture<Result<WriteOk, Err>>: 返回结果一个 future，因为 write 是纯异步 API，整个链路上没有一处是阻塞的。

Result<WriteOk, Err>: Result 的灵感来自 Rust 中的 Result，其中 WriteOk 和 Err 同时只能一个有值，由于 Write API 还会根据路由表拆分请求，所以 Result 必须有合并的能力。
WriteOk: 写入成功的结果展示，包含 success(成功条数) 和 failed(失败条数) 以及写入的 metrics 列表，多个 WriteOk 支持合并
Err: 写入失败结果展示，包含错误状态码、错误文本信息、抛错的服务器地址，同样支持多个 Err 合并

### Rows 结构
| rows | tag1 | tag2 | tag3 | timestamp | field1 | field2 |
| --- | --- | --- | --- | --- | --- | --- |
|0| a1 | a2 | a3 | 2021-07-15 18:00:00 | fa1 | fa2 |
|0| a1 | a2 | a3 | 2021-07-15 18:00:01 | fb1 | fb2 |
|0| a1 | a2 | a3 | 2021-07-15 18:00:02 | fc1 | fc2 |
|1| d1 | d2 | d3 | 2021-07-15 18:00:02 | fe1 | fe2 |

如上表格，4 行数据都是相同 metric，其中前 3 条数据是相同时间线 `metric-a1-a2-a3` 所以前 3 条是一个 Rows，第 4 条数据是另一个 Rows

### 如果构建一个 Rows?
```java
//
final long time = System.currentTimeMillis() - 1;
final Rows rows = Series.newBuilder(metric) // 指定 metric
    .tag("tag1", "tag_v1") // tagK1 = tagV1
    .tag("tag2", "tag_v2") // tagK2 = tagV2
    .toRowsBuilder() // 时间线已经确定，下面转为 Rows 的 Builder
    .field(time, "field1", FieldValue.withDouble(0.1)) // 添加一个时间点为 `time` 的 field1
    .field(time, "field2", FieldValue.withString("string_value")) // 添加一个时间点为 `time` 的 field2，与上面的 filed1 是同一行数据
    .field(time + 1, "field1", FieldValue.withDouble(0.2)) // 添加一个时间点为 `time + 1` 的 field1，这是第二行数据了
    .field(time + 1, "field2", FieldValue.withString("string_value_2")) // 添加一个时间点为 `time + 1` 的 field2，与上面的 filed1 是同一行数据
    .build();
```
以下代码也具备相同效果
```java
final long time = System.currentTimeMillis() - 1;
final Rows rows = Series.newBuilder(metric) // 指定 metric
    .tag("tag1", "tag_v1") // tagK1 = tagV1
    .tag("tag2", "tag_v2") // tagK2 = tagV2
    .toRowsBuilder() // 时间线已经确定，下面转为 Rows 的 Builder
    .fields(time, input -> { // 一次写一个时间点的一整行数据
        input.put("field1", FieldValue.withDouble(0.1));
        input.put("field2", FieldValue.withString("string_value"));
    })
    .fields(time + 1, input -> { // 一次写一个时间点的一整行数据
        input.put("field1", FieldValue.withDouble(0.2));
        input.put("field2", FieldValue.withString("string_value_2"));
    })
    .build();
```
