## 一些运维工具

### 如何使用 `kill -s SIGUSR2 $pid`

第一次执行 `kill -s SIGUSR2 $pid` 会从日志输出上看到如下帮助信息，包括:
1. 清除本地路由表缓存
2. 开/关简洁版写查 response 日志的输出
3. 输出内存中的 metrics、路由表 cache 和重要对象的内存状态信息到本地文件中

按照帮助信息执行即可

```
 - -- CeresDBxClient Signal Help --
 -     Signal output dir: /Users/xxx/xxx
 -
 -     How to clear route cache:
 -       [1] `cd /Users/xxx/xxx`
 -       [2] `touch clear_cache.sig`
 -       [3] `kill -s SIGUSR2 $pid`
 -       [4] `rm clear_cache.sig`
 -
 -     How to open or close read/write log(The second execution means close):
 -       [1] `cd /Users/xxx/xxx`
 -       [2] `touch rw_logging.sig`
 -       [3] `kill -s SIGUSR2 $pid`
 -       [4] `rm rw_logging.sig`
 -
 -     How to open or close rpc limiter(The second execution means close):
 -       [1] `cd /Users/xxx/xxx`
 -       [2] `touch rpc_limit.sig`
 -       [3] `kill -s SIGUSR2 $pid`
 -       [4] `rm rpc_limit.sig`
 -
 -     How to get metrics、display info and route cache detail:
 -       [1] `cd /Users/xxx/xxx`
 -       [2] `rm *.sig`
 -       [3] `kill -s SIGUSR2 $pid`
 -
 -     The file signals that is currently open:
 -       rpc_limit.sig
 -
```
