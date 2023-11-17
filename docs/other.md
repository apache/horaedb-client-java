## Some maintenance tools

### How to use `kill -s SIGUSR2 $pid`

The first time you execute `kill -s SIGUSR2 $pid`, will see the following help information from the log output, including:
1. Clear the local router cache
2. Turn on/off simple output of the response log
3. Output in-memory metrics, router cache, and memory state information of important objects to local files

Follow the help information to execute

```
 - -- HoraeDBClient Signal Help --
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
