## About LimitedPolicy
HoraeDBClient will limit the write/query by default (the default maximum number for write requests is 16384, and the default maximum number of requests for queries is 8),
The limiting parameters see the configuration in `WriteOptions` / `QueryOptions`.

`LimitedPolicy` is the processing policy after the flow is limited. It should be noted that if the number of data contained in one write request exceeds the limit, then the request will be allowed. 

| name                         | description                                                                                                                            |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| DiscardPolicy                | The latest written data is directly discarded when limiting                                                                            |
| AbortPolicy                  | Throw a LimitedException directly to the upper layer when limiting                                                                     |
| BlockingPolicy               | Blocking thread while limiting                                                                                                         |
| BlockingTimeoutPolicy        | Blocking thread util timeout，the error message while be reflected in `Result#Err` when timeout occurs, and no exception will be thrown |
| AbortOnBlockingTimeoutPolicy | Blocking thread util timeout，and throw a `LimitedException` to the upper when timeout occurs                                           |
| XXXPolicy                    | If none of the above meets your needs, please customize it based on `LimitedPolicy`. Happy coding : )                                  |

Note：default write limit policy is `AbortOnBlockingTimeoutPolicy(3s)`，query is `AbortOnBlockingTimeoutPolicy(10s)`

## Adaptive limiting
HoraeDBClient also provides an adaptive current limiter based on TCP `Vegas` and `Gradient` limit algorithms at the underlying communication layer.

### Vegas
`Vegas` is a congestion control algorithm that actively adjusts cwnd. The main idea is to set two thresholds, alpha and beta, and then calculate the target rate (Expected) and the actual rate (Actual), then compare the relationship between diff and alpha and beta, and adjust cwnd.

The core algorithm is
```
diff = cwnd*(1-baseRTT/RTT)
if (diff < alpha)
    set: cwnd = cwnd + 1
else if (diff >= beta)
    set: cwnd = cwnd - 1
else
    set: cwnd = cwnd
```

### Gradient Concurrency-limits
The `Gradient` concurrency-limit algorithm adjusts the limit according to the change gradient of the current average RTT and the long-term exponentially smoothed average RTT. Unlike traditional congestion control algorithms, average values are used instead of minimum values.

The core algorithm is
```java
// Compute the gradient limited to the range [0.5, 1.0] to filter outliers
gradient = max(0.5, min(1.0, longtermRtt / currentRtt));

// Calculate the new limit by applying the gradient and allowing some queuing
newLimit = gradient * currentLimit + queueSize;

// Update limits with smoothing factor (default is 0.2)
newLimit = currentLimit * (1 - smoothing) + newLimit * smoothing
```

- The default adaptive current limiting algorithm is `Gradient`，you can set in configuration `org.apache.horaedb.rpc.RpcOptions.limitKind = Vegas` by `Vegas`, or set `org.apache.horaedb.rpc.RpcOptions.limitKind = None` to close
- It will fail quickly when the limit is reached by default, and it can also be set by setting `org.apache.horaedb.rpc.RpcOptions.blockOnLimit = true`
- Write and read requests share this current limiter. By default, 70% and 30% of the access permissions can be obtained for write and read respectively. You can use the startup parameters `-DCeresDB.rpc.write.limit_percent=xxx`，readLimitPercent is `1 - writeLimitPercent`
