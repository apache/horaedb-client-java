## Stream API
CeresDB supports streaming query and writing which is suitable for large amounts of data.

### Stream Write API

```java
/**
 * Executes a streaming-write-call, returns a write request observer for streaming-write.
 *
 * @param table the table to write
 * @param ctx    the invoke context
 * @return a write request observer for streaming-write
 */
StreamWriteBuf<Point, WriteOk> streamWrite(final String table, final Context ctx);
```

#### Parameters

| name           | desc                                                                                                                                                                                                                |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `String table` | A table must be specified, and the data that can only be streamed into this table for efficiency. Supporting multiple table data streamed at the same time is not efficient and meaningless, so it is not supported |
| `Context ctx`  | Call context, to achieve some special requirements, the content in ctx will be written into the headers metadata of gRPC                                                                                            |

#### Return
`StreamWriteBuf<Point, WriteOk>`
```java
public interface StreamWriteBuf<V, R> {

    /**
     * Write data to buffer, but not flush to server.
     *
     * @param val data value
     * @return this
     */
    StreamWriteBuf<V, R> write(final V val);

    /**
     * Flush buffer data to server.
     *
     * @return this
     */
    StreamWriteBuf<V, R> flush();

    /**
     * Flush the given data to the server directly. The default
     * implementation write data to the buffer first and then
     * flush the data to the server immediately.
     *
     * @param c collection of data
     * @return this
     */
    StreamWriteBuf<V, R> writeAndFlush(final Collection<V> c);

    /**
     * Tell server that the streaming-write has completed.
     *
     * @return the streaming-wrote future result
     */
    CompletableFuture<R> completed();
}
```

You can continuously calls write multiple times to write data into the cache, and actually initiates a real streaming write call when flushing, which can be flushed multiple times. 
It is recommended to write batch of data and then flush once, and call again when the writing is completed

`completed` method tells the server to complete, and then the server will return a summarized result(`CompletableFuture<WriteOk>`)

Example:
```java
final StreamWriteBuf<Point, WriteOk> writer = this.writeClient.streamWrite("test_table");
final CompletableFuture<WriteOk> ret = writer
    .write(Util.generatePoints("test_table"))
    .write(Util.generatePoints("test_table"))
    .write(Util.generatePoints("test_table"))
    .flush() // flush, the background will send the data to the server
    .write(Util.generatePoints("test_table")) //
    .flush() // flush again
    .completed(); // completed will end the `stream`, and the server will return the overall write result
```

### Stream Query API

```java
/**
 * Executes a stream-query-call with a streaming response.
 *
 * @param req      the query request
 * @param observer receives data from an observable stream
 * @param ctx      the invoke context
 */
void streamSqlQuery(SqlQueryRequest req, Context ctx, Observer<SqlQueryOk> observer);

/**
 * Executes a stream-query-call with a streaming response.
 *
 * @param req     the query request
 * @param timeout how long to wait {@link Iterator#hasNext()} before giving up, in units of unit
 * @param unit    a TimeUnit determining how to interpret the timeout parameter
 * @param ctx     the invoke context
 * @return the iterator of record data
 */
Iterator<Row> blockingStreamSqlQuery(SqlQueryRequest req, long timeout, TimeUnit unit, Context ctx);
```

Stream query has two APIs.
- one is based on `Observer` callback, which is more flexible and suitable for non-blocking asynchronous scenarios
- the other is to return an `Iterator`, each element is a row of data (Row), the hasNext method may be blocked until the server returns the data stream or the data stream ends.

#### Parameters

| name                            | desc |
|---------------------------------| -- |
| `SqlQueryRequest req`           | Query request, which are no different from ordinary queries |
| `Context ctx`                   | Same as ordinary queries `Context` |
| `Observer<SqlQueryOk> observer` | The response callback observer can continuously receive the data returned by the server, and `onCompleted` will be called after all the data is sent on the server |
| `timeout`                       | The maximum waiting time for calling `Iterator#hasNext` (because it is a buffer-based lazy data pull, when the buffer is empty, it will pull data from the server now) |

Observer API
```java
public interface Observer<V> {

    /**
     * Receives a value from the stream.
     *
     * <p>Can be called many times but is never called after {@link #onError(Throwable)}
     * or {@link #onCompleted()} are called.
     *
     * @param value the value passed to the stream
     */
    void onNext(V value);

    /**
     * Receives a terminating error from the stream.
     *
     * <p>May only be called once and if called it must be the last method called. In
     * particular if an exception is thrown by an implementation of {@code onError}
     * no further calls to any method are allowed.
     *
     * @param err the error occurred on the stream
     */
    void onError(Throwable err);

    /**
     * Receives a notification of successful stream completion.
     *
     * <p>May only be called once and if called it must be the last method called. In
     * particular if an exception is thrown by an implementation of {@code onCompleted}
     * no further calls to any method are allowed.
     */
    default void onCompleted() {
        // NO-OP
    }
}
```
