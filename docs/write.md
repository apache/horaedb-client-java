
## Write process

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

## Description
- CeresDBClient
  - The java client implementation of CeresDB is oriented to users and provides APIs such as writing and querying
- WriteClient
    - The default implementation of writing data, pure asynchronous writing
    - Contains asynchronously fetching router, automatically splitting requests
    - Support asynchronous retry 
    - Asynchronously merge responses from multiple servers
- RouterClient
  - The router client will maintain the routing table information locally and refresh the routing table from the server
- RpcClient
  - A pure asynchronous high-performance RPC client, the default transport layer is implemented based on gRPC


## CreateTable
Create table example
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

## Write API

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

### Parameters
| name             | desc                                                                                                                                                                      |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WriteRuest req` | Write request is a written Point collection, where Point is a data point that supports multiple values, allowing data points from different tables to be written together |
| `Context ctx`    | Call context, to achieve some special requirements, the content in ctx will be written into the headers metadata of gRPC                                                  |

### Return

`CompletableFuture<Result<WriteOk, Err>>`
- Return a future, because write is a pure asynchronous API, and no part of the entire link is blocked

`Result<WriteOk, Err>`
- Result is inspired by Result in Rust, where QueryOk and Err can only have a value at the same time
- Since the Write API also splits the request according to the router, the `Result` must have the ability to merge

`WriteOk`
- The display of successful writing results, including success (number of successful points) and failed (number of failed points) and a list of written tables
- Multiple WriteOk supports merging

`Err`
- The result of the query failure is displayed, including the error status code, error text information, and the address of the server where the error was thrown

### How to build `Point`?
```java
final long time = System.currentTimeMillis() - 1;

// build single point once
final Point point = Point.newPointBuilder(table) // set table
        .setTimestamp(time) // set first point timestamp
        .addTag("tag1", "tag_v1") // add point tag
        .addTag("tag2", "tag_v2")
        .addField("field1", Value.withDouble(0.64)) // add point value
        .addField("field2", Value.withString("string_value"))
        .build() // complete the building and check
```

