## HoraeDB software version definition：

### Format
X.Y.Z

| name | desc                                                             |
| --- |------------------------------------------------------------------|
| X | major，Representative made incompatible modifications             |
| Y | minor，Representative has added downward compatible functionality |
| Z | patch，Representative made a downward compatibility correction    |

The label below（modifier）：
### Modifier
| name  | desc |
|-------| --- |
| alpha | The internal test version is generally only used for internal testing, and there may be many bugs |
| beta  | Public beta version |
| RC    | Release candidate, release candidate, theoretically no new features will be added, mainly bug fixes |

---
## 1.0.5 [2023-08-10]
- Features
  - `RpcOptions` add new field `connectionMaxAgeMs`, which controls max time a connection can live, default 0(forever). [65](https://github.com/CeresDB/horaedb-client-java/pull/65)

## 1.0.3 [2023-04-04]
- Fixes
  - Fix `Garbled characters on non-utf-8 encoded jdk` [57](https://github.com/CeresDB/horaedb-client-java/pull/57)
  - Fix `Multi-block(exp: group by) data not processed` [59](https://github.com/CeresDB/horaedb-client-java/pull/59)

## 1.0.2 [deprecated]

## 1.0.1 [2023-03-14]
- Fixes
  - Fix `Memory leak caused by metric req_rt` [51](https://github.com/CeresDB/horaedb-client-java/pull/51)
  - Fix `Query multi reuslt error with same value` [52](https://github.com/CeresDB/horaedb-client-java/pull/52)
  - Fix `Clear dirty route cache when error occurs` [53](https://github.com/CeresDB/horaedb-client-java/pull/53)

## 1.0.0 [2023-02-28]
- Features
  - Update proto to v1.0.0 [45](https://github.com/CeresDB/horaedb-client-java/pull/45)
  - Add API `List<Column> Row.getColumns()` [44](https://github.com/CeresDB/horaedb-client-java/pull/44)
- Fixes
  - Fix `NPE for table error while creating table` [43](https://github.com/CeresDB/horaedb-client-java/pull/43)
- Breaking Changes
  - Move `Value Row.getColumnValue(string)` to `Column Row.getColumn(string)`
  - Move `Object Value.getValue()` to `Object Value.getObject()`

## 1.0.0.alpha [2023-02-08]
- Features
  - The [HoraeDB](https://github.com/CeresDB/horaedb/tree/main) java client version initial release, allowing for the reading, writing, and managing of data tables.
