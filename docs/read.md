## 查询流程

```
                   ┌─────────────────────┐
                   │   CeresDBxClient    │
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
│   CeresDBx Node1    │      │   CeresDBx Node2    │      │         ...         │
└─────────────────────┘      └─────────────────────┘      └─────────────────────┘
```

## 名词解释
- CeresDBxClient
  - CeresDBx 的 java client 实现，面向使用用户，提供写入、查询等 API
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
CompletableFuture<Result<QueryOk, Err>> query(QueryRequest req, Context ctx);
```

### 参数说明:
| name | desc |
| --- | --- |
| `QueryRequest req` | 查询条件，包含 metrics 和 ql 字段，metrics 为建议字段，填写话会有更高效的路由, 不填写的话会自动解析 ql 语句以便进行路由(需要引入 ceresdbx-sql 模块); ql 为查询语言的文本表示。 |
| `Context ctx` | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata |

### 返回值说明

```java
CompletableFuture<Result<QueryOk, Err>>: 返回结果一个 future，因为 query 是纯异步 API，整个链路上没有一处是阻塞的。

Result<QueryOk, Err>: Result 的灵感来自 Rust 中的 Result，其中 QueryOk 和 Err 同时只能一个有值。
QueryOk: 查询成功的结果展示，包含一个 rowCount 用于反映这次查询的数据条数；还有一个 Stream<byte[]> rows 表示此次查询的数据流。
可以调用 QueryOk#mapToRecord 将每一行转成 Record 或是 QueryOk#mapToArray 将每一行转成一个 Object[]。
实际上，也可以让服务端返回 json，在发起查询调用时在 Context
中设置数据协议，即 ctx.with("data_protocol", "json")，设置后服务端将返回 UTF8 的 byte[]，用户基于 QueryOk#map 自行将 byte[]
转成 String 再解析 json 即可。
代码示例：
    // Record
    final Stream<Record> records = queryOk.mapToRecord();
    // 其中 parseUser 先基于 bytes 构建 String 再 parseJson 到 pojo 对象
    final Stream<User> users = queryOk.map(bytes -> parseUser(bytes));

Err: 查询失败结果展示，包含错误状态码、错误文本信息、抛错的服务器地址。
```

Record:
```java
public interface Record extends IndexedRecord {

  /**
   * Return the value of a field given its name.
   */
  Object get(final String field);

  default <T> T get(final String field, final Class<T> expectType) {
    return expectType.cast(get(field));
  }

  /**
   * Get a boolean value for the given {@code field}.
   */
  default Boolean getBoolean(final String field) {
    return get(field, Boolean.class);
  }

  default Integer getUInt16(final String field) {
    return getInteger(field);
  }

  default Integer getUInt8(final String field) {
    return getInteger(field);
  }

  default Integer getInt16(final String field) {
    return getInteger(field);
  }

  default Integer getInt8(final String field) {
    return getInteger(field);
  }

  /**
   * Get a integer value for the given {@code field}.
   */
  default Integer getInteger(final String field) {
    return get(field, Integer.class);
  }

  default Long getTimestamp(final String field) {
    return getLong(field);
  }

  default Long getUInt64(final String field) {
    return getLong(field);
  }

  default Long getUInt32(final String field) {
    return getLong(field);
  }

  default Long getInt64(final String field) {
    return getLong(field);
  }

  /**
   * Get a long value for the given {@code field}.
   */
  default Long getLong(final String field) {
    return get(field, Long.class);
  }

  /**
   * Get a float value for the given {@code field}.
   */
  default Float getFloat(final String field) {
    return get(field, Float.class);
  }

  /**
   * Get a double value for the given {@code field}.
   */
  default Double getDouble(final String field) {
    return get(field, Double.class);
  }

  /**
   * Get a string value for the given {@code field}.
   */
  default String getString(final String field) {
    return get(field, String.class);
  }

  /**
   * Get a bytes value for the given {@code field}.
   */
  default byte[] getBytes(final String field) {
    return get(field, byte[].class);
  }

  /**
   * Return true if record has field with name.
   */
  boolean hasField(final String field);

  /**
   * Return the field count of this record.
   */
  int getFieldCount();

  /**
   * Return all field descriptors in the record.
   */
  List<FieldDescriptor> getFieldDescriptors();
}
```
