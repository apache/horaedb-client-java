## 关于 LimitedPolicy
CeresDBClient 默认会对写入/查询进行限流（默认写请求最大在途数据条目 16384，查询最大在途请求数目 8），
限流方式为对在途请求的数据条数/查询请求数进行限制，可参见 WriteOptions/QueryOptions 中的配置，
其中 LimitedPolicy 为被限流后的处理策略，需要注意的是，如果用户一个写入请求中包含的数据条数就超过设置的最大许可数，
那么当在途请求为 0 的情况下会放行这次请求，下面为所有策略进行一个简单介绍

| name | description |
| --- | ---|
| DiscardPolicy | 达到限流值后直接丢弃最新的写入数据 |
| AbortPolicy | 达到限流值后直接抛出 LimitedException 异常到上层 |
| BlockingPolicy | 达到限流值后阻塞当前调用线程直到拿到写入许可（等待在途请求被消化） |
| BlockingTimeoutPolicy | 达到限流值后阻塞当前调用线程直到拿到写入许可（等待在途请求被消化）或者一直等到超时，如果超时，错误信息会体现在返回值的 Result#Err 上，不会抛出异常 |
| AbortOnBlockingTimeoutPolicy | 达到限流值后阻塞当前调用线程直到拿到写入许可（等待在途请求被消化）或者一直等到超时，如果超时会抛出 LimitedException 异常到上层 |
| XXXPolicy | 如果以上均不满足您的需求，请基于 LimitedPolicy 定制吧，祝 coding 的开心 |

注意：AbortOnBlockingTimeoutPolicy(3s) 为写入限流的默认策略，AbortOnBlockingTimeoutPolicy(10s) 为查询限流的默认策略

## 自适应限流
除了上面介绍的上层限流器，CeresDBClient 还在底层通信层提供了基于 TCP Vegas 和 Gradient Concurrency-limits 算法的自适应限流器。

### Vegas
Vegas 是一种主动调整 cwnd 的拥塞控制算法，主要思想是设置两个阈值，alpha 和 beta，然后通过计算目标速率 (Expected)
和实际速率 (Actual) 差 (diff)，再比较 diff 与 alpha 和 beta 的关系，对 cwnd 进行调节。

核心算法逻辑如下：
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
并发限制算法，根据当前平均 RTT 的变化梯度和长期指数平滑平均 RTT 调整限制。与传统的拥塞控制算法不同，使用平均值而不是最小值。

核心算法逻辑如下：
```java
// 计算限制范围 [0.5, 1.0] 的梯度以过滤异常值
gradient = max(0.5, min(1.0, longtermRtt / currentRtt));

// 通过应用渐变并允许某些排队来计算新的限制
newLimit = gradient * currentLimit + queueSize;

// 使用平滑因子更新限制（平滑因子默认值为 0.2）
newLimit = currentLimit * (1 - smoothing) + newLimit * smoothing
```

- 默认自适应限流算法是 Gradient，可以通过设置 `io.ceresdb.rpc.RpcOptions.limitKind = Vegas` 改为 Vegas
  或者是设置为 设置 `io.ceresdb.rpc.RpcOptions.limitKind = None` 进行关闭
- 默认达到限流值会快速失败，也可以通过设置 `io.ceresdb.rpc.RpcOptions.blockOnLimit = true` 将策略改为阻塞直至超时
- 默认情况下写、读请求共享这个限流器，默认写、读分别可以获得 70% 和 30% 的通行许可，可以通过启动参数 -DCeresDB.rpc.write.limit_percent=xxx 来设置写的比例，读的比例为 1 - writeLimitPercent
