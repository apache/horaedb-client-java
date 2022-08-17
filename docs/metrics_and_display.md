## 如何获取 metrics 和 display 数据？
在程序运行时，可以利用 Linux 平台的 SIGUSR2 信号输出节点的状态信息（display）以及 metric 数据

### 执行方式:
`kill -s SIGUSR2 pid`

相关信息会输出到指定目录，默认在程序工作目录（cwd: lsof -p $pid | grep cwd）生成 2 个文件
- CeresDB_client_metrics.log 为前缀的文件，记录了当前 client 节点的所有 metrics 信息
- CeresDB_client_display.log 为前缀的文件，记录的当前 client 节点重要的内存状态信息

### Metrics 输出到日志
- 默认每 10 分钟会输出 metrics 到日志中

### Metrics 列表（不断更新中）
|name|description|
| ---- | ---- |
| req_bytes | RPC request 总的 bytes 统计 |
| resp_bytes | RPC response 总的 bytes 统计 |
| req_qps_${rpc_method} | RPC request 的 QPS 统计，每个 RPC method 单独统计 |
| req_serialized_bytes_${rpc_method} | 每个 RPC request 的序列化后的大小统计，RPC method 统计粒度 |
| resp_serialized_bytes_${rpc_method} | 每个 RPC response 的序列化后的大小统计，RPC method 统计粒度 |
| req_rt_${rpc_method}_${tenant}| 请求 RT，方法 + 租户粒度 |
| req_rt_${rpc_method}_${tenant}_${address} | 请求 RT，方法 + 租户 + Server 地址粒度 |
| req_failed_${rpc_method}_${tenant}| 请求失败统计，方法 + 租户粒度 |
| req_failed_${rpc_method}_${tenant}_${address} | 请求失败统计，方法 + 租户 + Server 地址粒度 |
| thread_pool.${thread_pool_name} | 线程池执行任务耗时统计 |
| scheduled_thread_pool.${schedule_thread_pool_name} | Schedule 线程池执行任务耗时统计 |
| split_num_per_write | 根据路由表，每批写入会被分割成多个请求，这个指标统计的是写入被分割的情况 |
| route_for_metrics_refreshed_size_${address} | 每次向 server 请求路由表的 metric 数量 |
| route_for_metrics_cached_size_${address} | 本地缓存的路由表的 metric 数量 |
| route_for_metrics_gc_times_${address} | 路由表每次 GC 需要循环次数的统计 |
| route_for_metrics_gc_items_${address} | 路由表每次 GC 释放掉的 items 数量统计 |
| route_for_metrics_gc_timer_${address} | 路由表 GC 时间统计 |
| route_for_metrics_refresh_timer_${address} | 路由表远程刷新时间统计 |
| write_rows_success_num | 写入成功的数据条数统计 |
| write_rows_failed_num | 写入失败的数据条数统计 |
| write_failed | 写入失败的次数统计 |
| write_qps | 写入请求 QPS |
| metrics_num_per_write | 每个写入请求的 metrics 数量统计 |
| async_write_pool.time | SDK 中执行异步写入任务的 async pool 耗时统计，这个很重要，需要特别关注 |
| async_read_pool.time | SDK 中执行异步读取任务的 async pool 耗时统计，这个很重要，需要特别关注 |
| connection_counter | SDK 与 server 建立的连接个数 |
| connection_failures | SDK 与 server 失败连接的统计 |
| read_row_count | 每次查询数据条数的统计 |
| read_failed | 查询失败的次数统计 |
| read_qps | 查询 QPS |
| write_by_retries_${n} | 第 n 次重试写入的 qps，n == 0 表示是第一次写入（非重试），n > 3 将视为 n == 3 来统计 |
| read_by_retries_${n} | 第 n 次重试查询的 qps，n == 0 表示是第一次查询（非重试），n > 3 将视为 n == 3 来统计 |
| write_limiter_acquire_wait_time | 被写入限流器 block 的时间统计 |
| write_limiter_acquire_available_permits | 写入限流器 available_permits 统计 |
| query_limiter_acquire_wait_time | 被查询限流器 block 的时间统计 |
| query_limiter_acquire_available_permits | 查询限流器 available_permits 统计 |
| direct_executor_timer_${name} | direct executor 任务执行耗时统计 |
| serializing_executor_single_task_timer_${name} | serializing executor 单个任务执行耗时统计 |
| serializing_executor_drain_timer_${name} | serializing executor 排干全部任务耗时统计 |
| serializing_executor_drain_num_${name} | serializing executor 每次排干任务数量 histogram 统计 |
| rpc_limiter_${name} | 基于 TCP Vegas 算法的 rpc 限流器相关统计指标 |

### Metrics demo:
```
-- CeresDB 21-7-16 17:24:12 ===============================================================

-- CeresDB -- Counters --------------------------------------------------------------------
-- CeresDB 21-12-28 14:22:36 ==============================================================

-- CeresDB -- Counters --------------------------------------------------------------------
connection_counter
             count = 1
connection_counter_127.0.0.1:8831
             count = 1
rpc_limiter_call_id_grpc_call_status_success
             count = 3160

-- CeresDB -- Histograms ------------------------------------------------------------------
metrics_num_per_write
             count = 1585
               min = 1
               max = 1
              mean = 1.00
            stddev = 0.00
            median = 1.00
              75% <= 1.00
              95% <= 1.00
              98% <= 1.00
              99% <= 1.00
            99.9% <= 1.00
query_limiter_acquire_available_permits
             count = 0
               min = 0
               max = 0
              mean = 0.00
            stddev = 0.00
            median = 0.00
              75% <= 0.00
              95% <= 0.00
              98% <= 0.00
              99% <= 0.00
            99.9% <= 0.00
req_serialized_bytes_storage.StorageService/Route
             count = 1
               min = 35
               max = 35
              mean = 35.00
            stddev = 0.00
            median = 35.00
              75% <= 35.00
              95% <= 35.00
              98% <= 35.00
              99% <= 35.00
            99.9% <= 35.00
req_serialized_bytes_storage.StorageService/Write
             count = 3170
               min = 90222
               max = 90222
              mean = 90222.00
            stddev = 0.00
            median = 90222.00
              75% <= 90222.00
              95% <= 90222.00
              98% <= 90222.00
              99% <= 90222.00
            99.9% <= 90222.00
resp_serialized_bytes_storage.StorageService/Route
             count = 1
               min = 58
               max = 58
              mean = 58.00
            stddev = 0.00
            median = 58.00
              75% <= 58.00
              95% <= 58.00
              98% <= 58.00
              99% <= 58.00
            99.9% <= 58.00
resp_serialized_bytes_storage.StorageService/Write
             count = 3160
               min = 8
               max = 8
              mean = 8.00
            stddev = 0.00
            median = 8.00
              75% <= 8.00
              95% <= 8.00
              98% <= 8.00
              99% <= 8.00
            99.9% <= 8.00
route_for_metrics_cached_size_127.0.0.1:8831
             count = 1
               min = 1
               max = 1
              mean = 1.00
            stddev = 0.00
            median = 1.00
              75% <= 1.00
              95% <= 1.00
              98% <= 1.00
              99% <= 1.00
            99.9% <= 1.00
route_for_metrics_gc_items_127.0.0.1:8831
             count = 0
               min = 0
               max = 0
              mean = 0.00
            stddev = 0.00
            median = 0.00
              75% <= 0.00
              95% <= 0.00
              98% <= 0.00
              99% <= 0.00
            99.9% <= 0.00
route_for_metrics_gc_times_127.0.0.1:8831
             count = 0
               min = 0
               max = 0
              mean = 0.00
            stddev = 0.00
            median = 0.00
              75% <= 0.00
              95% <= 0.00
              98% <= 0.00
              99% <= 0.00
            99.9% <= 0.00
route_for_metrics_refreshed_size_127.0.0.1:8831
             count = 1
               min = 1
               max = 1
              mean = 1.00
            stddev = 0.00
            median = 1.00
              75% <= 1.00
              95% <= 1.00
              98% <= 1.00
              99% <= 1.00
            99.9% <= 1.00
rpc_limiter_inflight_partition_storage.StorageService/Write
             count = 3170
               min = 1
               max = 10
              mean = 5.63
            stddev = 2.91
            median = 6.00
              75% <= 8.00
              95% <= 10.00
              98% <= 10.00
              99% <= 10.00
            99.9% <= 10.00
serializing_executor_drain_num_query_client
             count = 0
               min = 0
               max = 0
              mean = 0.00
            stddev = 0.00
            median = 0.00
              75% <= 0.00
              95% <= 0.00
              98% <= 0.00
              99% <= 0.00
            99.9% <= 0.00
serializing_executor_drain_num_write_client
             count = 4745
               min = 1
               max = 4
              mean = 2.01
            stddev = 1.42
            median = 1.00
              75% <= 4.00
              95% <= 4.00
              98% <= 4.00
              99% <= 4.00
            99.9% <= 4.00
split_num_per_write
             count = 1585
               min = 1
               max = 1
              mean = 1.00
            stddev = 0.00
            median = 1.00
              75% <= 1.00
              95% <= 1.00
              98% <= 1.00
              99% <= 1.00
            99.9% <= 1.00
write_limiter_acquire_available_permits
             count = 1585
               min = 3072
               max = 7168
              mean = 5096.21
            stddev = 1428.11
            median = 5120.00
              75% <= 6144.00
              95% <= 7168.00
              98% <= 7168.00
              99% <= 7168.00
            99.9% <= 7168.00
write_rows_failed_num
             count = 1580
               min = 0
               max = 0
              mean = 0.00
            stddev = 0.00
            median = 0.00
              75% <= 0.00
              95% <= 0.00
              98% <= 0.00
              99% <= 0.00
            99.9% <= 0.00
write_rows_success_num
             count = 1580
               min = 1024
               max = 1024
              mean = 1024.00
            stddev = 0.00
            median = 1024.00
              75% <= 1024.00
              95% <= 1024.00
              98% <= 1024.00
              99% <= 1024.00
            99.9% <= 1024.00

-- CeresDB -- Meters ----------------------------------------------------------------------
connection_failures
             count = 0
         mean rate = 0.00 events/second
     1-minute rate = 0.00 events/second
     5-minute rate = 0.00 events/second
    15-minute rate = 0.00 events/second
req_qps_storage.StorageService/Route
             count = 1
         mean rate = 0.02 events/second
     1-minute rate = 0.07 events/second
     5-minute rate = 0.16 events/second
    15-minute rate = 0.19 events/second
req_qps_storage.StorageService/Write
             count = 3170
         mean rate = 48.97 events/second
     1-minute rate = 49.11 events/second
     5-minute rate = 49.76 events/second
    15-minute rate = 49.91 events/second
rpc_limiter_limit
             count = 50
         mean rate = 0.77 events/second
     1-minute rate = 3.68 events/second
     5-minute rate = 8.19 events/second
    15-minute rate = 9.36 events/second
rpc_limiter_limit.partition_partition_storage.StorageService/Query
             count = 0
         mean rate = 0.00 events/second
     1-minute rate = 0.00 events/second
     5-minute rate = 0.00 events/second
    15-minute rate = 0.00 events/second
rpc_limiter_limit.partition_partition_storage.StorageService/Write
             count = 0
         mean rate = 0.00 events/second
     1-minute rate = 0.00 events/second
     5-minute rate = 0.00 events/second
    15-minute rate = 0.00 events/second
rpc_limiter_limit.partition_partition_unknown
             count = 0
         mean rate = 0.00 events/second
     1-minute rate = 0.00 events/second
     5-minute rate = 0.00 events/second
    15-minute rate = 0.00 events/second
write_by_retries_0
             count = 1585
         mean rate = 24.47 events/second
     1-minute rate = 24.57 events/second
     5-minute rate = 24.88 events/second
    15-minute rate = 24.96 events/second
write_failed
             count = 0
         mean rate = 0.00 events/second
     1-minute rate = 0.00 events/second
     5-minute rate = 0.00 events/second
    15-minute rate = 0.00 events/second
write_qps
             count = 1580
         mean rate = 24.39 events/second
     1-minute rate = 24.17 events/second
     5-minute rate = 24.05 events/second
    15-minute rate = 24.02 events/second

-- CeresDB -- Timers ----------------------------------------------------------------------
direct_executor_timer_grpc_client
             count = 9486
         mean rate = 145.10 calls/second
     1-minute rate = 139.58 calls/second
     5-minute rate = 130.71 calls/second
    15-minute rate = 128.45 calls/second
               min = 0.00 milliseconds
               max = 1.22 milliseconds
              mean = 0.06 milliseconds
            stddev = 0.09 milliseconds
            median = 0.03 milliseconds
              75% <= 0.07 milliseconds
              95% <= 0.28 milliseconds
              98% <= 0.33 milliseconds
              99% <= 0.34 milliseconds
            99.9% <= 0.65 milliseconds
query_limiter_acquire_wait_time
             count = 0
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 0.00 milliseconds
               max = 0.00 milliseconds
              mean = 0.00 milliseconds
            stddev = 0.00 milliseconds
            median = 0.00 milliseconds
              75% <= 0.00 milliseconds
              95% <= 0.00 milliseconds
              98% <= 0.00 milliseconds
              99% <= 0.00 milliseconds
            99.9% <= 0.00 milliseconds
req_rt_storage.StorageService/Route_public
             count = 1
         mean rate = 0.02 calls/second
     1-minute rate = 0.07 calls/second
     5-minute rate = 0.16 calls/second
    15-minute rate = 0.19 calls/second
               min = 192.00 milliseconds
               max = 192.00 milliseconds
              mean = 192.00 milliseconds
            stddev = 0.00 milliseconds
            median = 192.00 milliseconds
              75% <= 192.00 milliseconds
              95% <= 192.00 milliseconds
              98% <= 192.00 milliseconds
              99% <= 192.00 milliseconds
            99.9% <= 192.00 milliseconds
req_rt_storage.StorageService/Route_public_127.0.0.1:8831
             count = 1
         mean rate = 0.02 calls/second
     1-minute rate = 0.07 calls/second
     5-minute rate = 0.16 calls/second
    15-minute rate = 0.19 calls/second
               min = 192.00 milliseconds
               max = 192.00 milliseconds
              mean = 192.00 milliseconds
            stddev = 0.00 milliseconds
            median = 192.00 milliseconds
              75% <= 192.00 milliseconds
              95% <= 192.00 milliseconds
              98% <= 192.00 milliseconds
              99% <= 192.00 milliseconds
            99.9% <= 192.00 milliseconds
req_rt_storage.StorageService/Write_public
             count = 3160
         mean rate = 48.91 calls/second
     1-minute rate = 49.12 calls/second
     5-minute rate = 49.76 calls/second
    15-minute rate = 49.91 calls/second
               min = 77.00 milliseconds
               max = 546.00 milliseconds
              mean = 142.22 milliseconds
            stddev = 46.96 milliseconds
            median = 133.00 milliseconds
              75% <= 161.00 milliseconds
              95% <= 198.00 milliseconds
              98% <= 265.00 milliseconds
              99% <= 395.00 milliseconds
            99.9% <= 474.00 milliseconds
req_rt_storage.StorageService/Write_public_127.0.0.1:8831
             count = 3160
         mean rate = 48.90 calls/second
     1-minute rate = 49.12 calls/second
     5-minute rate = 49.76 calls/second
    15-minute rate = 49.91 calls/second
               min = 68.00 milliseconds
               max = 546.00 milliseconds
              mean = 140.21 milliseconds
            stddev = 44.08 milliseconds
            median = 132.00 milliseconds
              75% <= 159.00 milliseconds
              95% <= 195.00 milliseconds
              98% <= 229.00 milliseconds
              99% <= 385.00 milliseconds
            99.9% <= 546.00 milliseconds
route_for_metrics_gc_timer_127.0.0.1:8831
             count = 0
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 0.00 milliseconds
               max = 0.00 milliseconds
              mean = 0.00 milliseconds
            stddev = 0.00 milliseconds
            median = 0.00 milliseconds
              75% <= 0.00 milliseconds
              95% <= 0.00 milliseconds
              98% <= 0.00 milliseconds
              99% <= 0.00 milliseconds
            99.9% <= 0.00 milliseconds
route_for_metrics_refresh_timer_127.0.0.1:8831
             count = 1
         mean rate = 0.02 calls/second
     1-minute rate = 0.07 calls/second
     5-minute rate = 0.16 calls/second
    15-minute rate = 0.19 calls/second
               min = 322.00 milliseconds
               max = 322.00 milliseconds
              mean = 322.00 milliseconds
            stddev = 0.00 milliseconds
            median = 322.00 milliseconds
              75% <= 322.00 milliseconds
              95% <= 322.00 milliseconds
              98% <= 322.00 milliseconds
              99% <= 322.00 milliseconds
            99.9% <= 322.00 milliseconds
rpc_limiter_acquire_time_storage.StorageService/Write
             count = 3170
         mean rate = 48.96 calls/second
     1-minute rate = 49.11 calls/second
     5-minute rate = 49.76 calls/second
    15-minute rate = 49.91 calls/second
               min = 0.00 milliseconds
               max = 0.06 milliseconds
              mean = 0.01 milliseconds
            stddev = 0.01 milliseconds
            median = 0.01 milliseconds
              75% <= 0.01 milliseconds
              95% <= 0.02 milliseconds
              98% <= 0.03 milliseconds
              99% <= 0.04 milliseconds
            99.9% <= 0.05 milliseconds
scheduledThreadPool.display_self
             count = 1
         mean rate = 0.02 calls/second
     1-minute rate = 0.07 calls/second
     5-minute rate = 0.16 calls/second
    15-minute rate = 0.19 calls/second
               min = 2.37 milliseconds
               max = 2.37 milliseconds
              mean = 2.37 milliseconds
            stddev = 0.00 milliseconds
            median = 2.37 milliseconds
              75% <= 2.37 milliseconds
              95% <= 2.37 milliseconds
              98% <= 2.37 milliseconds
              99% <= 2.37 milliseconds
            99.9% <= 2.37 milliseconds
serializing_executor_drain_timer_query_client
             count = 0
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 0.00 milliseconds
               max = 0.00 milliseconds
              mean = 0.00 milliseconds
            stddev = 0.00 milliseconds
            median = 0.00 milliseconds
              75% <= 0.00 milliseconds
              95% <= 0.00 milliseconds
              98% <= 0.00 milliseconds
              99% <= 0.00 milliseconds
            99.9% <= 0.00 milliseconds
serializing_executor_drain_timer_write_client
             count = 9485
         mean rate = 145.14 calls/second
     1-minute rate = 139.48 calls/second
     5-minute rate = 130.54 calls/second
    15-minute rate = 128.26 calls/second
               min = 0.00 milliseconds
               max = 58.63 milliseconds
              mean = 0.50 milliseconds
            stddev = 1.76 milliseconds
            median = 0.00 milliseconds
              75% <= 0.15 milliseconds
              95% <= 2.49 milliseconds
              98% <= 3.34 milliseconds
              99% <= 6.47 milliseconds
            99.9% <= 13.47 milliseconds
serializing_executor_single_task_timer_query_client
             count = 0
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 0.00 milliseconds
               max = 0.00 milliseconds
              mean = 0.00 milliseconds
            stddev = 0.00 milliseconds
            median = 0.00 milliseconds
              75% <= 0.00 milliseconds
              95% <= 0.00 milliseconds
              98% <= 0.00 milliseconds
              99% <= 0.00 milliseconds
            99.9% <= 0.00 milliseconds
serializing_executor_single_task_timer_write_client
             count = 9485
         mean rate = 145.13 calls/second
     1-minute rate = 139.48 calls/second
     5-minute rate = 130.54 calls/second
    15-minute rate = 128.26 calls/second
               min = 0.00 milliseconds
               max = 8.41 milliseconds
              mean = 0.50 milliseconds
            stddev = 1.06 milliseconds
            median = 0.01 milliseconds
              75% <= 0.18 milliseconds
              95% <= 2.53 milliseconds
              98% <= 3.16 milliseconds
              99% <= 4.71 milliseconds
            99.9% <= 7.03 milliseconds
write_limiter_acquire_wait_time
             count = 1585
         mean rate = 24.25 calls/second
     1-minute rate = 23.57 calls/second
     5-minute rate = 22.44 calls/second
    15-minute rate = 22.16 calls/second
               min = 1.99 milliseconds
               max = 61.91 milliseconds
              mean = 2.69 milliseconds
            stddev = 1.78 milliseconds
            median = 2.25 milliseconds
              75% <= 2.65 milliseconds
              95% <= 5.12 milliseconds
              98% <= 7.05 milliseconds
              99% <= 7.46 milliseconds
            99.9% <= 13.17 milliseconds

```

### Display demo:
```
--- CeresDBClient ---
id=1
version=1.0.0.Final
clusterAddress=127.0.0.1:8831
tenant=public
userAsyncWritePool=null
userAsyncReadPool=null

--- RouterClient ---
opts=RouterOptions{rpcClient=com.ceresdb.rpc.GrpcClient@35dd62b, clusterAddress=127.0.0.1:8831, maxCachedSize=10000, gcPeriodSeconds=60, refreshPeriodSeconds=30}
routeCache.size=1

--- GrpcClient ---
started=true
opts=RpcOptions{defaultRpcTimeout=10000, rpcThreadPoolSize=0, rpcThreadPoolQueueSize=16, maxInboundMessageSize=67108864, flowControlWindow=67108864, idleTimeoutSeconds=300, keepAliveTimeSeconds=3, keepAliveTimeoutSeconds=3, keepAliveWithoutCalls=true, openVegasLimiter=true, vegasInitialLimit=50, blockOnLimit=false, tenant=Tenant{tenant='public', childTenant='sub_test', token='test_token'}}
connectionObservers=[com.ceresdb.CeresDBClient$RpcConnectionObserver@465f5824]
asyncPool=DirectExecutor{name='grpc_executor'}
interceptors=[com.ceresdb.rpc.interceptors.MetricInterceptor@62b47ad1, com.ceresdb.rpc.interceptors.ClientRequestLimitInterceptor@77533e32, com.ceresdb.rpc.interceptors.ContextToHeadersInterceptor@1a52427d, com.ceresdb.rpc.interceptors.AuthHeadersInterceptor@2670a76b]
managedChannelPool={127.0.0.1:8831=ManagedChannelOrphanWrapper{delegate=ManagedChannelImpl{logId=1, target=127.0.0.1:8831}}}
transientFailures={}

--- WriteClient ---
maxRetries=0
maxWriteSize=512
asyncPool=SerializingExecutor{name='write_client'}

--- QueryClient ---
maxRetries=1
asyncPool=SerializingExecutor{name='query_client'}

--- HttpManagementClient ---
started=true
tenant=public


```
