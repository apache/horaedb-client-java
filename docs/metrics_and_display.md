## How to get metrics and display data?
When the program is running, the SIGUSR2 signal of the Linux platform can be used to output the status information (display) and metric data of the node

### How to use
`kill -s SIGUSR2 pid`

Information will be output to the specified directory，by default, 2 files will be generated in the program working directory (cwd: lsof -p $pid | grep cwd)
- The file prefixed with CeresDB_client_metrics.log records all the metrics information of the current client node
- The file prefixed with CeresDB_client_display.log records important memory status information of the current client node

### Metrics output to log
- Metrics will be output to the log every 10 minutes by default

### Metrics description （updating）
|name| description                                                                                                              |
| ---- |--------------------------------------------------------------------------------------------------------------------------|
| req_bytes | RPC request total bytes statistics                                                                                       |
| resp_bytes | RPC response total bytes statistics                                                                                      |
| req_qps_${rpc_method} | QPS statistics of RPC request, separate for each RPC method                                                              |
| req_serialized_bytes_${rpc_method} | The serialized size statistics of each RPC request, separate for each method                                             |
| resp_serialized_bytes_${rpc_method} | The serialized size statistics of each RPC response, separate for each method                                            |
| req_rt_${rpc_method}_${tenant}| Request RT, method + tenant                                                                                              |
| req_rt_${rpc_method}_${tenant}_${address} | Rquest RT, method + tenant + server                                                                                      |
| req_failed_${rpc_method}_${tenant}| Request failed, method + tenant                                                                                          |
| req_failed_${rpc_method}_${tenant}_${address} | Request failed, method + tenant + server                                                                                 |
| thread_pool.${thread_pool_name} | Time of thread pool execution tasks                                                                                      |
| scheduled_thread_pool.${schedule_thread_pool_name} | Time of thread schedule pool execution tasks                                                                             |
| split_num_per_write | Each batch of writes will be divided into multiple requests by router, and this indicator counts the splitting of writes |
| route_for_metrics_refreshed_size_${address} | The count of metrics requested for the router from the server each time                                                  |
| route_for_metrics_cached_size_${address} | The count of metrics in the locally cached router                                                                        |
| route_for_metrics_gc_times_${address} | The count of cycles required for each GC of the router                                                                   |
| route_for_metrics_gc_items_${address} | The count of items released by each GC of the router                                                                     |
| route_for_metrics_gc_timer_${address} | The time of GC time                                                                                                      |
| route_for_metrics_refresh_timer_${address} | The time of remote refresh                                                                                               |
| write_rows_success_num | The count of successfully written point                                                                                  |
| write_rows_failed_num | The count of failed written points                                                                                       |
| write_failed | The count of failed write requests                                                                                       |
| write_qps | Write QPS                                                                                                                |
| metrics_num_per_write | The count of metrics for each write request                                                                              |
| async_write_pool.time | The async pool time of executing asynchronous write tasks in the SDK is very important and requires special attention    |
| async_read_pool.time | Same as `async_write_pool.time` for reading                                                                              |
| connection_counter | The count of connections established between the SDK and the server                                                      |
| connection_failures | The count of failed connections established between the SDK and the server                                               |
| read_row_count | The count of point per query                                                                                             |
| read_failed | The count of failed queried requests                                                                                     |
| read_qps | Query QPS                                                                                                                |
| write_by_retries_${n} | The QPS of the nth retry write, n == 0 means it is the first write (not retry), n > 3 will be counted as n == 3          |
| read_by_retries_${n} | Same as `write_by_retries_${n}` for reading                                                                              |
| write_limiter_acquire_wait_time | Time written to the current limiter block                                                                                |
| write_limiter_acquire_available_permits | Write limiter available_permits                                                                                          |
| query_limiter_acquire_wait_time | The time of the queried limiter block                                                                                    |
| query_limiter_acquire_available_permits | Query limiter available_permits statistics                                                                               |
| direct_executor_timer_${name} | The task execution time of direct executor                                                                               |
| serializing_executor_single_task_timer_${name} | The task exeution time for serializing executor                                                                          |
| serializing_executor_drain_timer_${name} | Drain all task time statistics                                                                                           |
| serializing_executor_drain_num_${name} | Serializing executor histogram statistics on the number of tasks drained each time                                       |
| rpc_limiter_${name} | The rpc metrics on TCP Vegas limiter                                                                                     |

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
opts=RouterOptions{rpcClient=ioceresdb.rpc.GrpcClient@35dd62b, clusterAddress=127.0.0.1:8831, maxCachedSize=10000, gcPeriodSeconds=60, refreshPeriodSeconds=30}
routeCache.size=1

--- GrpcClient ---
started=true
opts=RpcOptions{defaultRpcTimeout=10000, rpcThreadPoolSize=0, rpcThreadPoolQueueSize=16, maxInboundMessageSize=67108864, flowControlWindow=67108864, idleTimeoutSeconds=300, keepAliveTimeSeconds=3, keepAliveTimeoutSeconds=3, keepAliveWithoutCalls=true, openVegasLimiter=true, vegasInitialLimit=50, blockOnLimit=false, tenant=Tenant{tenant='public', childTenant='sub_test', token='test_token'}}
connectionObservers=[io.ceresdb.CeresDBClient$RpcConnectionObserver@465f5824]
asyncPool=DirectExecutor{name='grpc_executor'}
interceptors=[io.ceresdb.rpc.interceptors.MetricInterceptor@62b47ad1, io.ceresdb.rpc.interceptors.ClientRequestLimitInterceptor@77533e32, io.ceresdb.rpc.interceptors.ContextToHeadersInterceptor@1a52427d, io.ceresdb.rpc.interceptors.AuthHeadersInterceptor@2670a76b]
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
