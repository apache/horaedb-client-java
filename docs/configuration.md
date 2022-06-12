## Global Options (System properties / Java -Dxxx)
| name | description |
| --- | --- |
| "CeresDBx.available_cpus" | CeresDB 可用 cpu 数量指定，默认使用当前环境的全部 cpu 数量 |
| "CeresDBx.grpc.conn.failures.reset_threshold" | gRPC reset connection 的阈值，默认 3 |
| "CeresDBx.client.read.write.rw_logging" | 写查的简洁日志输出开关，默认 true |
| "CeresDBx.client.write.collect_wrote_detail" | 写入成功后是否保留写入的 metric 列表，默认为 false |
| "CeresDBx.client.use_os_signal" | 是否使用 OS Signal，默认会使用 SIGUSR2 |
| "CeresDBx.signal.out_dir" | Signal 接收器输出内容到指定磁盘目录，默认为进程启动目录 |
| "CeresDBx.avro.name_validate" | 是否开启 Avro name 校验，默认 false |
| "CeresDBx.rpc.write.limit_percent" | RPC 层自适应限流器 `写` 操作占比（默认 0.7），剩下的留给 `读` 操作 |
| "CeresDBx.reporter.period_minutes" | Metrics reporter 定时输出周期，默认 30 分钟 |
| "CeresDBx.http.read_timeout_ms" | 基于 http 协议的管理类 API 的 http 读超时时间，默认 10000ms |
| "CeresDBx.http.write_timeout_ms" | 基于 http 协议的管理类 API 的 http 写超时时间，默认 10000ms |

## CeresDBxOptions
| name | description |
| --- | --- |
| clusterAddress | 集群地址（稳定可用的地址），路由表将从这个地址获取 |
| asyncWritePool | 作为一个纯异步的 client，需要一个写入调度线程池，用户可自行配置，默认将使用 SerializingExecutor，如果自行配置了 pool 请重点关注性能指标：async_read_pool.time 的数据，即使调整这个调度线程池的配置 |
| asyncReadPool | 作为一个纯异步的 client，需要一个查询调度线程池，用户可自行配置，默认将使用 SerializingExecutor，如果自行配置了 pool 请重点关注性能指标：async_read_pool.time 的数据，即使调整这个调度线程池的配置 |
| tenant | 租户信息 |
| rpcOptions | RPC 的配置选项，详情请参考 RpcOptions |
| routerOptions | 路由表更新组件的配置选项，详情请参考 RouterOptions |
| writeOptions | 写入相关的的配置选项，详情请参考 WriteOptions |
| managementOptions | 数据管理相关配置选项，详情参考 ManagementOptions |

## WriteOptions
写入相关的的配置选项

| name | description |
| --- | --- |
| maxRetries | 最大重试次数，sdk 会根据 server 返回的 error code 自行决定是否重试（通常是可能是路由表失效），重试过程对用户透明，完全异步上层无感知 |
| maxWriteSize | 每次写入请求最大的数据条目，超过会被分割成多个请求，默认值为 512 |
| maxInFlightWriteRows | 写入限流参数：最大的在途请求的数据行数，超过会被 block |
| limitedPolicy | 写入限流策略，提供几个实现分别为 blocking、discard 和 blocking-timeout，默认为 abort-blocking-timeout(3s)（阻塞到超时 3s 后失败并抛出异常），用户也可自行扩展 |

## QueryOptions
写入相关的的配置选项

| name | description |
| --- | --- |
| maxRetries | 最大重试次数，sdk 会在路由表失效时重试，重试过程对用户透明，完全异步上层无感知 |
| maxInFlightQueryRequests | 查询限流参数：最大的在途请求数，超过会被 block |
| limitedPolicy | 查询限流策略，提供几个实现分别为 blocking、discard 和 blocking-timeout，默认为 abort-blocking-timeout(10s)（阻塞到超时 10s 后失败并抛出异常），用户也可自行扩展 |

# ManagementOptions

| name | description |
| --- | --- |
| managementAddress | 管理服务地址，通常和几区地址 IP/Host 相同，但端口不同 |
| tenant | 租户信息 |
| checkSql | 是否在客户端提前检查 sql 有效性，默认 true |

## RpcOptions
RPC 相关的配置选项

| name | description |
| --- | --- |
| defaultRpcTimeout | 默认的远程调用超时时间，每个请求可单独指定 timeout，如果没有指定，那么将使用这个值，默认为 10s |
| rpcThreadPoolSize | 处理 RPC 调用的线程池最大线程数 |
| rpcThreadPoolQueueSize | 处理 RPC 调用的线程池最大线程数, 如果设置为 0，那么将使用 IO 线程处理 response，默认为 64，线程池的 core_size 要比这个值小，具体公式为：Math.min(Math.max(Cpus.cpus() << 1, 16), rpcThreadPoolQueueSize) |
| maxInboundMessageSize | 上行消息，可接受单个 message 最大的字节数，默认 64M |
| flowControlWindow | 基于 http2.0 的流量控制，默认 64M |
| idleTimeoutSeconds | 设置连接的最大空闲时间，超过时间连接可能失效，默认 5 分钟 |
| keepAliveTimeSeconds | 心跳保鲜，此参数控制在 transport 上发送 keep-alive ping 的时间间隔（以秒为单位默认无限秒，相当于关闭），具备指数避让策略。 |
| keepAliveTimeoutSeconds | 心跳保鲜，此参数控制 keep-alive ping 的发送方等待确认的时间（以秒为单位， 默认 3 秒）。如果在此时间内未收到确认，它将关闭连接。 |
| keepAliveWithoutCalls | 心跳保鲜，如果将此参数设置为 true（默认为 false），则即使没有请求进行，也可以发送 keep-alive ping。这会产生一定消耗，通常情况下，更建议使用 idleTimeoutSeconds 来代替次选项 |
| limitKind | 限流算法类型，支持 Vegas 和 Gradient， 默认为 Gradient |
| initialLimit | Limiter 初始 limit |
| maxLimit | Limiter 最大 limit |
| smoothing | Limiter 平滑因子，默认 0.2 |
| blockOnLimit | 当达到限流值时是否 block 住请求线程？否则快速失败，默认值 false |

## RouterOptions
路由表更新组件的配置选项

name | description |
| --- | --- |
| maxCachedSize | 本地最大缓存路由表条目数量，默认值为 10_000，超过会定期 GC |
| gcPeriodSeconds | 定期 GC 触发频率，默认 60 秒 |
| refreshPeriodSeconds | 路由表刷新频率，除了被服务端标记为 invalid 会在调用时主动刷新以外，后台还会定期刷新所有路由表，默认 30 秒执行一次全量刷新 |
