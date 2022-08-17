## CeresDB software version definition：

### Format
X.Y.Z

| name | desc |
| --- | --- |
| X | major，Representative made incompatible modifications |
| Y | minor，Representative has added downward compatible functionality |
| Z | patch，Representative made a downward compatibility correction |

The label below（modifier）：
### Modifier
| name | desc |
| --- | --- |
| alpha | The internal test version is generally only used for internal testing, and there may be many bugs |
| beta | Public beta version |
| RC | Release candidate, release candidate, theoretically no new features will be added, mainly bug fixes |
| Final | Final version |

---
## 0.0.3.RC2
- Features
  - TageValue add withBooleanOrNull nullable API
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.RC
- Features
  - FieldValue&TageValue provide nullable API
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.beta2
- Features
  - CeresDBClient builder provide a simplified method for constructing parametric version
- Fixes
  -
- Breaking Changes
  -

## 0.0.3.beta1
- Features
  - Keyword check during writing (writing `timestamp` and `tsid` is not allowed)
- Fixes
  - Fix Union type missing timestamp LogicType
- Breaking Changes
  -

## 0.0.3.alpha
- Features
  - Support writing of unsigned Int64 values (through BigInteger)
  - `Record` support read unsigned int64
  - Improve the definition of version number rules
  - Optimization part document description
- Fixes
  -
- Breaking Changes
  - The return value of `Record#getUInt64` is changed from `Long` to `BigInteger` to avoid overflow

## 0.0.2.Final (2022-01-17)
- Features
  - Add statistics of total network request / response bytes (Metric)
  - Close grpc limiter based on sigusr2 signal
  - Occupied sigusr2 signal can be turned off and turned on by default
  - Add GRPC channel ID for troubleshooting
- Fixes
  - Remove SIG_TERM、SIGHUP signals，Only SIGUSR2 used to avoid incompatibility with some environments. At the same time, a small file is created to realize various signal processing. Please refer to the user's documentation for details
  - Update rpc-limiter， maxLimit change from 4096 to 1024，initialLimit change from 512 to 64
  - Add rpc-limiter unit test
  - Specify logger output in CeresDBClient for metrics reporter to avoid the burden of configuring logger conf for users
  - Fix Stream API NPE bug
- Breaking Changes
  - Add OptKeys，summarize all system properties configuration，unify naming rules，and some keys have changed
  - Query#blockingStreamQuery 返回值修改为 Iterator<Record> （原为 Iterator<Stream<Record>>）

## 0.0.2.RC15 (2022-01-04)
- Features
  - Change default limit algorithm of RPC from Vegas to Gradient Concurrency-limits
- Fixes
  - SerializingExecutor queue print warn log once when it exceeds a certain size(1024 by default)
  - RouteCache GC threads avoid print cache detail
- Breaking Changes
  - The parameters related to adaptive current limiting in RpcOptions have been redefined and are no longer compatible with the old version

## 0.0.2.RC14 (2021-12-28)
- Features
  - Add query current limiter. The default concurrency is 8
  - Record available permits metric when write/query
  - Default Value of maximum number of in transit write in lines change from 16384 to 8192
  - Vegas limiter initialLimit change from 20 to 40，and add vegasInitialLimit option
  - Various write parameters and configurations have been optimized
  - remove unnecessary metric collect
- Fixes
- Breaking Changes
  - QueryOptions#setRoutedClient 改为 QueryOptions#setRouterClient
  - QueryOptions#getRoutedClient 改为 QueryOptions#getRouterClient
  - CeresDBOptions.Builder.limitedPolicy 改为 CeresDBOptions.Builder.writeLimitedPolicy

## 0.0.2.RC13 (2021-12-27)
- Features
  - Prints table names when writing failed
  - RouterClient cleaner and refresher use shard pool to reduce resource used
  - CeresDBClient display to the log regularly (once every 30 minutes)
  - Default value of maximum single write batch change from 1024 to 512，and maximum in transit write in lines change from 4096 to 16384
  - RouteClient receive SIGUSR2 signal to route cache items to file
  - NamedThreadFactory add Factory ID to identify duplicate Factory Name
- Fixes
  - Update Log4j to 2.17.0 to fix security hole
- Breaking Changes

## 0.0.2.RC12 (2021-12-20)
- Features
- Fixes
  - Fix the problem that `Management` loaded based on SPI cannot be created repeatedly, resulting in the inability to support multi tenant
- Breaking Changes

## 0.0.2.RC11 (2021-12-17)
- Features
  - Query result `Record` add Type and LogicalType enum，and add an API to determine whether it is a timestamp type
- Fixes
- Breaking Changes

## 0.0.2.RC10 (2021-12-15)
- Features
  - Ignore null value written
- Fixes
- Breaking Changes

## 0.0.2.RC9 (2021-12-14)
- Features
  - TAG support more type. In addition to the original string support, the following types are added：
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
  - Update Log4j to 2.16.0 to fix security hole
  - Locking OkHttp to avoid jar dependency conflict
- Breaking Changes

## 0.0.2.RC8 (2021-12-07)
- Features
  - SQL Parser support Insert StatementType
  - SQL Parser Support `EXISTS [TABLE] xxx_table` 
  - In a single request，(tenant, token, childTenant) combination can be covered by change context，which is targeted at the user scenario of a single cluster but multiple tenants
- Fixes
  - fix 1.0.0.RC4 reference dependency client.version 1.0.0.RC3
  - fix tenant be ignored when token is null
- Breaking Changes
  - Remove SQL Parser of whereColumns

## 0.0.2.RC6 (2021-12-04)
- Features
- Fixes
- Breaking Changes
  - Authentication headers key unified to lowercase. Will be incompatible with the previous version of authentication

## 0.0.2.RC5 (2021-12-02)
- Features
  - Support parse SQL `SHOW CREATE TABLE` AND `DROP TABLE` 
  - ALTER TABLE ADD COLUMN Support adding
- Fixes
- Breaking Changes
  - TagValue#string rename to TagValue#withString. The naming style is consistent with FieldValue

## 0.0.2.RC4 (2021-11-24)
- Features
  - ceresdb-sql-javacc module support inner/left/right join SQL parse，Support to resolve multiple queries tables
- Fixes
  - Cancel the reuse of avro record when querying data to avoid data being accidentally overwritten
- Breaking Changes

## 0.0.2.RC3 (2021-11-10)
- Features
  - Add new SQL parser based javacc，It is mainly used for table routing of query and client SQL verification of Management API module
- Fixes
  - When the query result is returned, the utf8 type is converted to string
- Breaking Changes

## 0.0.2.RC2 (2021-11-02)
- Features
  - By default, avro schema's name validate is turned off to support `select count (*) from` queries without using aliases
  - Management API module checks the SQL validity on the client by default
- Fixes
  - Because the syntax of creating tables on the ceresdb server is changed, modify the parser of the client to support the latest syntax
- Breaking Changes

## 0.0.2.RC1 (2021-11-01)
- Features
  - Add Management API (support execute SQL)
  - @NotSupport Tag streaming API not supported at present
  - Add more types of support. All types currently supported:
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
  - Fix: the grpc connection cannot be refreshed when the IP under the domain name is changed
- Breaking Changes
  - FieldValue some API changes and are not forward compatible

## 0.0.1.RC6 (2021-09.28)
- Features
- Fixes
  - Modify version number format
  - Fix the problem that there is no source code in the mvn repository
- Breaking Changes

## 0.0.1-RC5 (2021-09-28)
- Features
  - The keep alive option of gprc is turned off by default.
  - Add time cost metrics for the rpc adaptive limiter is blocked in acquire (rpc_limiter_acquire_time_${method_name})
- Fixes
- Breaking Changes
  - The signal triggering clear-route-cache is changed from SIGINT to SIGHUP，and SIGINT signal is nno longer supported（because SIGINT is used for normal interactive exit of JVM）

## 0.0.1-RC4 (2021-09-13)
- Features
  - An adaptive flow restrictor based on TCP Vegas congestion control algorithm is added in the RPC layer. The traffic rates of write and read requests are 70% and 30% respectively. You can choose to configure fast failure or blocking until timeout. Default blocking
  - Add abort-on-blocking-timeout (throw exception after being blocked for timeout) write limit strategy. And set abort-on-blocking-timeout(3s) as default policy（the default policy before is blocking-timeout(1s) ）
  - Add abort (throw LimitedException directly) write limit strategy
  - Optimization of write limit strategy. When the number of data pieces written at one time exceeds the maximum permission of current limiter，if the in transit request is 0 ant this time, the maximum permission of the current limiter will be consumed and the request will be released, this strategy will avoid the direct failure of such large requests 
- Fixes
- Breaking Changes

## 0.0.1-RC3 (2021-09-06)
- Features
  - Add the client instance id and client code version to the call context and send them to the server
  - Add write flow policy and provide several default implementations, blocking、discard and blocking-timeout. The default is blocking-timeout(1s)
- Fixes
- Breaking Changes

## 0.0.1-RC2 (2021-09-03)
- Features
  - Add `SerializingExecutor` and `DirectExecutor` statistics of task execution time
  - Batch function optimization，add `SerializingExecutor`
  - Add write flow limiter，the maximum umber of data lines in transit requests is 4096. User configurable parameter `maxInFlightWriteRows`，less than 0 means no current limitation
  - `RpcOptions.rpcThreadPoolSize` （rpc max thread number）to avoid occupying too much resource. The default value is adjusted from 64 seconds to 0 second 
  - `RpcOptions.keepAliveTimeoutSeconds` （ping timeout）the default value is adjusted from 5 seconds to 3 seconds
  - Add CHANGELOG.md
- Fixes
- Breaking Changes

## 0.0.1-RC1 (2021-08-31)
- Features
  - The first version of CeresDB client ，contains basic query and write functions
- Fixes
- Breaking Changes
