## Query process

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

## Description
- CeresDBClient
  - The java client implementation of CeresDB is oriented to users and provides APIs such as writing and querying
- QueryClient
  - The default implementation of queries, purely asynchronous
  - Including asynchronous acquisition of routing table, automatic retry when routing table fails
- RouterClient
  - The router client will maintain the routing table information locally and refresh the routing table from the server
- RpcClient
  - A pure asynchronous high-performance RPC client, the default transport layer is implemented based on gRPC


## How to use

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

### Parameters
| name                  | desc                                                                                                                                                                                                                                                          |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SqlQueryRequest req` | Query request, including `tables` and `sql` fields, `tables` is a suggested field, if filled in, there will be more efficient routing, if not filled, the sql statement will be automatically parsed for routing (need to introduce the ceresdb-sql module) |
| `Context ctx`         | Call context, to achieve some special requirements, the content in ctx will be written into the headers metadata of gRPC                                                                                                                                                                                                           |

### Return

`CompletableFuture<Result<SqlQueryOk, Err>>`
-  Return a future, because query is a pure asynchronous API, and no part of the entire link is blocked.

`Result<SqlQueryOk, Err>`
- Result is inspired by Result in Rust, where QueryOk and Err can only have a value at the same time

`SqlQueryOk`
 - The result of a successful query, contains
   - sql in SqlQueryRequest
   - affectedRows is the number of affected data rows returned when updating or deleting data
   - rows is the statement returned by the query, a class of Sql will only return affectedRows Or List<Rows>, will not return both values

`Row`
- When processing the results returned by the query, the user can directly obtain `List<Row>`, or process it through stream.
- Row is a collection of Value, which is a very simple data structure
- Note: When Value gets the java primitive value, you need to pass in the type method that matches the table creation, otherwise an error will be reported
- Example to use `Row`: `row.getColumnValue("cpu_util").getDouble()
  `

`Err`
- The result of the query failure is displayed, including the error status code, error text information, and the address of the server where the error was thrown.


