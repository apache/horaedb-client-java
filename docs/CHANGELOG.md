## CeresDB software version definition：

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
## 1.0.0 [2023-02-28]
- Features
  - Update proto to v1.0.0 [45](https://github.com/CeresDB/ceresdb-client-java/pull/45)
  - Add API `List<Column> Row.getColumns()` [44](https://github.com/CeresDB/ceresdb-client-java/pull/44)
- Fixes
  - Fix `NPE for table error while creating table` [43](https://github.com/CeresDB/ceresdb-client-java/pull/43)
- Breaking Changes
  - Move `Value Row.getColumnValue(string)` to `Column Row.getColumn(string)`
  - Move `Object Value.getValue()` to `Object Value.getObject()`

## 1.0.0.alpha [2023-02-08]
- Features
  - The [Ceresdb](https://github.com/CeresDB/ceresdb/tree/main) java client version initial release, allowing for the reading, writing, and managing of data tables.
- Fixes
  -
- Breaking Changes
  -
