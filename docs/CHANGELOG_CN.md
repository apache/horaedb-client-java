## CeresDB 软件版本定义：

### 版本号格式
X.Y.Z

| name | desc |
| --- | --- |
| X | 主版本号（major），一般代表做了不兼容的修改 |
| Y | 此版本号（minor），一般代表做了向下兼容的功能性新增 |
| Z | 修订版本号（patch），一般代表做了向下兼容的问题修正 |

再加上下边的标签（modifier）：
### Modifier
| name | desc |
| --- | --- |
| alpha | 内测版本，一般只供内部测试用，bug 可能较多 |
| beta | 公开测试版 |
| RC | Release Candidate，发行候选版本，理论上不再加入新的功能，主要修复 bug |
| Final | 最终版本 |

---
## 0.0.3.RC2
- Features
  - TageValue 追加 withBooleanOrNull 的 nullable API
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.RC
- Features
  - FieldValue&TageValue 提供 nullable API
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.beta2
- Features
  - CeresDBxClient builder 提供简化参数版构造方法
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.beta1
- Features
  - 写入期间关键字检查（不允许写入 `timestamp` 和 `tsid`）
- Fixes
  - 修复 Union 类型丢失 timestamp LogicType 的问题
- Breaking Changes
  -

## 0.0.3.alpha
- Features
  - 支持 unsigned int64 值的写入（通过 BigInteger）
  - `Record` 支持读取 unsigned int64 值
  - 完善版本号规则定义
  - 优化部分文档说明
- Fixes
  -
- Breaking Changes
  - `Record#getUInt64` 返回值由 `Long` 改为 `BigInteger` 以避免溢出

## 0.0.2.Final (2022-01-17)
- Features
  - 新增网络请求/响应 bytes 总量统计 metric
  - 基于 SIGUSR2 信号关闭 grpc limiter
  - 占用 SIGUSR2 信号可关，默认打开
  - GRPC channel 增加 ID 用于排查问题
- Fixes
  - 移除 SIG_TERM、SIGHUP 信号占用，仅使用 SIGUSR2 信号，避免和某些环境不兼容，同时利用创建一个小文件来实现各种不同的信号处理，详细请参考使用文档
  - 修改 rpc-limiter， maxLimit 调整为 1024（原 4096），initialLimit 调整为 64（原 512）
  - 增加 rpc-limiter 单元测试
  - 为 metrics reporter 指定 CeresDBClient 内的 logger output，避免给用户配置 logger conf 带来负担
  - 修复 Stream API NPE bug
- Breaking Changes
  - 增加 OptKeys，汇总所有 system properties 配置，统一命名规则，部分 key 已变化
  - Query#blockingStreamQuery 返回值修改为 Iterator<Record> （原为 Iterator<Stream<Record>>）

## 0.0.2.RC15 (2022-01-04)
- Features
  - 修改 RPC 层默认的限流算法，由 Vegas 改为 Gradient Concurrency-limits
- Fixes
  - SerializingExecutor queue 超过一定大小（默认 1024）时打印一次 warn 日志
  - RouteCache GC 线程避免打印 cache 明细
- Breaking Changes
  - RpcOptions 中与自适应限流相关的参数均进行了重新定义，不再兼容旧版本

## 0.0.2.RC14 (2021-12-28)
- Features
  - 增加查询限流器，默认并发度为 8
  - 每次 write/query 记录 available permits 到 metric
  - 最大在途写入行数从默认 16384 调整为 8192
  - Vegas limiter 调整 initialLimit 为 40（原 20），同时增加 vegasInitialLimit 选项
  - 调优了各种写入参数和配置
  - 精简掉不必要的 metric 采集
- Fixes
- Breaking Changes
  - QueryOptions#setRoutedClient 改为 QueryOptions#setRouterClient
  - QueryOptions#getRoutedClient 改为 QueryOptions#getRouterClient
  - CeresDBOptions.Builder.limitedPolicy 改为 CeresDBOptions.Builder.writeLimitedPolicy

## 0.0.2.RC13 (2021-12-27)
- Features
  - 写入失败时日志打印这一批失败的表名
  - RouterClient 的 cleaner 和 refresher 采用共享 Pool 以减少资源占用
  - CeresDBClient 定时 Display 到日志中（30 分钟执行一次）
  - 单次写入最大单批从默认 1024 调整为 512，最大在途写入行数从默认 4096 调整为 16384
  - RouteClient 接收 SIGUSR2 信号将 route cache items 输出到文件
  - NamedThreadFactory 增加 Factory ID 来识别重复的 Factory Name
- Fixes
  - 升级 Log4j 到 2.17.0 修复安全漏洞
- Breaking Changes

## 0.0.2.RC12 (2021-12-20)
- Features
- Fixes
  - 修复基于 SPI 加载的 `Management` 不能重复创建从而导致无法支持多租户的问题
- Breaking Changes

## 0.0.2.RC11 (2021-12-17)
- Features
  - 查询结果 `Record` 增加 Type 和 LogicalType 类型枚举，增加判断是否是 `timestamp` 类型 API
- Fixes
- Breaking Changes

## 0.0.2.RC10 (2021-12-15)
- Features
  - 忽略写入的 null 值
- Fixes
- Breaking Changes

## 0.0.2.RC9 (2021-12-14)
- Features
  - TAG 增加更多类型支持，除原有支持 String 外新增如下类型：
    ```
      Int64,
      Int(Int32),
      Int16,
      Int8,
      Boolean,
      Uint64,
      Uint32,
      Uint16,
      Uint8,
      Timestamp,
      Varbinary
    ```
- Fixes
  - 升级 Log4j 到 2.16.0 修复安全漏洞
  - 锁定 OkHttp 依赖项版本避免出现 jar 不兼容
- Breaking Changes

## 0.0.2.RC8 (2021-12-07)
- Features
  - SQL Parser 支持解析 Insert 的 StatementType
  - 支持 `EXISTS [TABLE] xxx_table` 语法
  - 在单次请求中，(tenant, token, childTenant) 组合是可以通过请求 Context 覆盖的，针对于单集群却多个租户的用户场景
- Fixes
  - 修复 1.0.0.RC4 依赖 client.version 一直为 1.0.0.RC3 的 bug
  - 修复 token 为空时不传租户的问题
- Breaking Changes
  - 移除 SQL Parser 的 whereColumns 解析，目前看没什么用处

## 0.0.2.RC6 (2021-12-04)
- Features
- Fixes
- Breaking Changes
  - 鉴权相关 Headers 的 Key 统一改为小写，将和前面版本的鉴权不兼容

## 0.0.2.RC5 (2021-12-02)
- Features
  - 支持 `SHOW CREATE TABLE` 和 `DROP TABLE` 的 SQL 语句解析
  - ALTER TABLE ADD COLUMN 支持添加多列
- Fixes
- Breaking Changes
  - TagValue#string 重命名为 TagValue#withString，与 FieldValue 风格保持一致

## 0.0.2.RC4 (2021-11-24)
- Features
  - ceresdb-sql-javacc 模块支持解析 inner/left/right join SQL 解析，支持解析多个查询 tables
- Fixes
  - 取消查询数据时 avro record 的复用，避免数据被不小心覆盖
- Breaking Changes

## 0.0.2.RC3 (2021-11-10)
- Features
  - 新增基于 javacc 的 SQL parser，主要用于查询的表路由以及 management API 模块的客户端 sql 校验
- Fixes
  - 在返回查询结果时将 Utf8 类型转换为 String
- Breaking Changes

## 0.0.2.RC2 (2021-11-02)
- Features
  - 默认关闭 avro schema 的 name validate 以支持 `select count(*) from` 这种不必使用别名查询
  - Management API 模块默认在客户端对 sql 有效性进行检查
- Fixes
  - 因为 CeresDB server 端建表语法变更，修改 client 的 parser 以支持最新语法
- Breaking Changes

## 0.0.2.RC1 (2021-11-01)
- Features
  - 新增 Management API (支持执行 SQL)
  - @NotSupport 标记暂不支持的流式 API
  - 新增更多类型支持，目前支持的全部类型：
    ```
      Double(Float64),
      String,
      Int64,
      Float(FLoat32),
      Int(Int32),
      Int16,
      Int8,
      Boolean,
      Uint64,
      Uint32,
      Uint16,
      Uint8,
      Timestamp,
      Varbinary
    ```
- Fixes
  - 修复当域名下的 ip 变更时 gRPC 连接无法刷新
- Breaking Changes
  - FieldValue 部分 API 变更并且不向前兼容

## 0.0.1.RC6 (2021-09.28)
- Features
- Fixes
  - 修改版本号格式
  - 修复发布到 mvn 仓库没有源码的问题新
- Breaking Changes

## 0.0.1-RC5 (2021-09-28)
- Features
  - 默认关闭 grpc 的 keep-alive 相关选项
  - 增加 rpc 自适应限流器在 acquire 时被 block 的时间统计指标 (rpc_limiter_acquire_time_${method_name})
- Fixes
- Breaking Changes
  - 触发 clear-route-cache 的信号由 SIGINT 改为 SIGHUP，不再支持 SIGINT 信号（因为 JVM 的正常交互式退出使用的是 SIGINT）

## 0.0.1-RC4 (2021-09-13)
- Features
  - 在 RPC 层增加了基于 TCP Vegas 拥塞控制算法的自适应通限流器，写、读请求的通行率占比分别为 70% 和 30%，可选择配置快速失败或者阻塞直到超时，默认阻塞
  - 增加 abort-on-blocking-timeout (被阻塞超时后抛出异常) 写入限流策略，并设置 abort-on-blocking-timeout(3s) 为默认策略（在这之前的默认策略为 blocking-timeout(1s) ）
  - 增加 abort (直接抛出 LimitedException) 写入限流策略
  - 写入限流策略优化，当一次性写入的数据条数就超过限流器最大许可的情况下，如果此时在途请求为 0，那么将消耗掉限流器的最大许可并对这次请求放行，该策略将避免这种大请求直接失败
- Fixes
- Breaking Changes

## 0.0.1-RC3 (2021-09-06)
- Features
  - 将 client 实例 id 和 client code version 加入到调用上下文并发送到服务端
  - 增加写入限流策略，提供几个默认实现分别为 blocking、discard 和 blocking-timeout，默认为 blocking-timeout(1s)
- Fixes
- Breaking Changes

## 0.0.1-RC2 (2021-09-03)
- Features
  - 增加 `SerializingExecutor` 和 `DirectExecutor` 执行任务耗时统计
  - 攒批执行任务优化，增加 `SerializingExecutor`
  - 增加写入限流器，默认限制最大在途请求的数据行数为 4096 行，用户可配置参数 `maxInFlightWriteRows`，小于 0 代表不做限流
  - `RpcOptions.rpcThreadPoolSize` （rpc 最大线程数量）默认值由原来的 64 调整为 0，默认不用线程池避免占用过多资源
  - `RpcOptions.keepAliveTimeoutSeconds` （ping timeout）默认值由原来的 5 秒调整为 3 秒
  - 新增 CHANGELOG.md
- Fixes
- Breaking Changes

## 0.0.1-RC1 (2021-08-31)
- Features
  - CeresDB client 的第一个版本，包含基本查询、写入能力
- Fixes
- Breaking Changes
