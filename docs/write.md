
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
    c3 STRING NULL,
    c4 BOOLEAN NULL,    
    c5 DOUBLE NULL,
    c6 FLOAT NULL,    
    c7 INT64 NULL,
    c8 INT32 NULL,
    c9 INT16 NULL,
    c10 INT8 NULL,
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
 * @param req  the write request
 * @param ctx  the invoke context
 * @return write result
 */
CompletableFuture<Result<WriteOk, Err>> write(WriteRequest req, Context ctx);
```

### 参数说明
| name             | desc                                                             |
|------------------|------------------------------------------------------------------|
| `WriteRuest req` | 要写入的请求，主要是一个写入的平铺的 Point 集合，其中 Point 是一个支持多值的数据点，允许不同表的数据点放在一起写入 |
| `Context ctx`    | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata               |

### 返回值说明
CompletableFuture<Result<WriteOk, Err>>: 返回结果一个 future，因为 write 是纯异步 API，整个链路上没有一处是阻塞的。

Result<WriteOk, Err>: Result 的灵感来自 Rust 中的 Result，其中 WriteOk 和 Err 同时只能一个有值，由于 Write API 还会根据路由表拆分请求，所以 Result 必须有合并的能力。
WriteOk: 写入成功的结果展示，包含 success(成功条数) 和 failed(失败条数) 以及写入的 metrics 列表，多个 WriteOk 支持合并
Err: 写入失败结果展示，包含错误状态码、错误文本信息、抛错的服务器地址，同样支持多个 Err 合并

### 如果构建一个 Point?
```java
//
final long time = System.currentTimeMillis() - 1;
final List<Point> points = Point.newPointsBuilder(table) // 指定 table
        .addPoint() // 添加一个数据点
            .setTimestamp(time) // 设置第一个点的时间戳
            .addTag("tag1", "tag_v1")
            .addTag("tag2", "tag_v2") 
            .addField("field1", Value.withFloat64(0.64)) 
            .addField("field2", Value.withString("string_value"))
            .build() // 完成第一个点的构建
        .addPoint() // 添加第二个点
            .setTimestamp(time+10) // 设置第二个点的时间戳
            .addTag("tag1", "tag_v1")
            .addTag("tag2", "tag_v2")
            .addField("field1", Value.withFloat64(1.28))
            .addField("field2", Value.withString("string_value 2"))
            .build() // 完成第二个点的构建
        .build() // 完成所有point的构建
```

