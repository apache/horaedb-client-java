## 流式 API
CeresDB 支持流式查询和写入，其中流式查询为 response streaming，适合用来大规模拉取数据；流式写入为 request streaming，适合大批量导入数据使用。

### 流式写入 API 说明

```java
/**
 * Executes a streaming-write-call, returns a write request observer for streaming-write.
 *
 * @param metric the metric to write
 * @param ctx    the invoke context
 * @return a write request observer for streaming-write
 */
StreamWriteBuf<Rows, WriteOk> streamWrite(final String metric, final Context ctx);
```

#### 参数说明

| name | desc |
| --- | --- |
| `String metric` | 必须要指定一个 metric，并且限制只能流式写入这个 metric 的数据，这样做是为了高效，支持多个 metric 数据同时流式写入并不能做到高效，意义不大，所以不支持 |
| `Context ctx` | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata |

#### 返回值说明
返回值:
`StreamWriteBuf<Rows, WriteOk>`: 一个支持流式写入的 buffer 如下：
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

用户不断的多次调用 write 将数据写入缓存，在 flush 时真正发起一次真正的流式写入调用，可以多次 flush，建议 write 一定条数再 flush 一次，当写入完成时再调用
completed 告知 server 完成，接下来 server 端会返回一个汇总过的 result(`CompletableFuture<WriteOk>`)

Example:
```java
final StreamWriteBuf<Rows, WriteOk> writer = this.writeClient.streamWrite("test_metric");
final CompletableFuture<WriteOk> ret = writer
    .write(Util.generateRow("test_metric")) // 写入随机生成的数据，这里只作为示例
    .write(Util.generateRow("test_metric")) // 可以链式调用
    .write(Util.generateRow("test_metric")) //
    .flush() // flush 一次，后台会将数据发送到 server
    .write(Util.generateRow("test_metric")) //
    .flush() // 再一次 flush，整个流式调用可以多次 flush，每次 flush 数据的大小可根据业务场景定夺
    .completed(); // 调用 completed 会结束这个`流`，server 会返回总体的写入结果
```

### 流式查询 API 说明

```java
/**
 * Executes a stream-query-call with a streaming response.
 *
 * @param req      the query request
 * @param observer receives data from an observable stream
 * @param ctx      the invoke context
 */
void streamQuery(QueryRequest req, Context ctx, Observer<QueryOk> observer);

/**
 * Executes a stream-query-call with a streaming response.
 *
 * @param req     the query request
 * @param timeout how long to wait {@link Iterator#hasNext()} before giving up, in units of unit
 * @param unit    a TimeUnit determining how to interpret the timeout parameter
 * @param ctx     the invoke context
 * @return the iterator of record data
 */
Iterator<Record> blockingStreamQuery(QueryRequest req, long timeout, TimeUnit unit, Context ctx);
```

流式查询有两种 API，一种是基于 Observer 回调，灵活性更高，适合非阻塞的异步场景；
另一种是返回一个 Iterator，每个 element 即是一行数据(Record)，hasNext 方法有可能被阻塞直到服务端返回数据流或者数据流结束。

#### 参数说明

| name | desc |
| --- | --- |
| `QueryRequest req` | 查询条件，这一点和普通查询没有任何区别 |
| `Context ctx` | 调用上下文，实现一些特殊需求，ctx 中的内容会写入 gRPC 的 headers metadata |
| `Observer<QueryOk> observer` | response 回调观察者，可以不断的接收 server 端返回的数据，在 server 端把数据吐完后 onCompleted 会被调用 |
| `timeout` | 调用 `Iterator#hasNext` 的最大等待时间（因为是基于 buffer 的惰性拉取数据，在 buffer 为空时会从 server 现拉取数据） |

Observer 的 API 如下：
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
