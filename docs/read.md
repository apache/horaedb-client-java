## 查询流程

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
                    │  Default GRPC impl  │
                    └─────────────────────┘
                               ▲
                               │
           ┌───────────────────┴ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
           │                            │
           ▼                            ▼                            ▼
┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
│   CeresDB  Node1    │      │   CeresDB  Node2    │      │         ...         │
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘
```

## 名词解释
- CeresDBClient
  - CeresDB 的 java client 实现，面向使用用户，提供写入、查询等 API
- QueryClient
  - 查询的默认实现，纯异步
  - 包含异步获取路由表，路由表失效自动重试
- RouterClient
  - 路由表客户端，会在本地维护路由表信息，并从服务端刷新路由表
- RpcClient
  - 一个纯异步的高性能 RPC 客户端，默认传输层基于 gRPC 实现


## 查询 API 说明

```java
/**
 * According to the conditions, query data from the database.
 *
 * @param req the query request
 * @param ctx the invoke context
 * @return query result
 */
CompletableFuture<Result<SqlQueryOk, Err>> sqlQuery(SqlQueryRequest req, Context ctx);
```

### 参数说明:
| name                  | desc                                                                                                       |
|-----------------------|------------------------------------------------------------------------------------------------------------|
| `SqlQueryRequest req` | 查询条件，包含 tables 和 sql 字段，tables 为建议字段，填写话会有更高效的路由, 不填写的话会自动解析 sql 语句以便进行路由(需要引入 ceresdb-sql 模块); sql 为查询语言。 |
| `Context ctx`         | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata                                                         |

### 返回值说明

```java
CompletableFuture<Result<SqlQueryOk, Err>>: 返回结果一个 future，因为 query 是纯异步 API，整个链路上没有一处是阻塞的。

Result<SqlQueryOk, Err>: Result 的灵感来自 Rust 中的 Result，其中 QueryOk 和 Err 同时只能一个有值。
SqlQueryOk: 查询成功的结果，其中 sql 是查询时的语句，affectedRows 是在更新或删除数据时返回的受影响数据行数，List<Row> 是查询返回的语句，一般来说，一类Sql只会返回 affectedRows 或者 List<Rows>，不会两个值都返回
        
在处理查询返回结果时，用户可以直接获取List<Row>，也可以通过 stream 的方式处理，Row是一个 Value 的集合，是很简单的数据结构。
需要注意的是，在 Value 获取 java primitive 值的时候，需要传入和建表匹配的数值类型方法，否则将会报错。        
代码示例：
        // column cpu_util 是一个 FLOAT64 类型的field
        row.getColumnValue("cpu_util").getFloat64()

Err: 查询失败结果展示，包含错误状态码、错误文本信息、抛错的服务器地址。
```

