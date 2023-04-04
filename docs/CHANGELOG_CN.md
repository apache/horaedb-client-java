## CeresDB 软件版本定义：

### 版本号格式
X.Y.Z

| name | desc                         |
|------|------------------------------|
| X    | 主版本号（major），一般代表做了不兼容的修改     |
| Y    | 此版本号（minor），一般代表做了向下兼容的功能性新增 |
| Z    | 修订版本号（patch），一般代表做了向下兼容的问题修正 |

再加上下边的标签（modifier）：
### Modifier
| name  | desc |
|-------| --- |
| alpha | 内测版本，一般只供内部测试用，bug 可能较多 |
| beta  | 公开测试版 |
| RC    | Release Candidate，发行候选版本，理论上不再加入新的功能，主要修复 bug |

---
## 1.0.3 [2023-04-04]
- Fixes
  - 修复 `在非utf-8编码的jdk上使用中文乱码问题` [57](https://github.com/CeresDB/ceresdb-client-java/pull/57)
  - 修复 `多batch数据(exp: group by)未被正确处理的问题` [59](https://github.com/CeresDB/ceresdb-client-java/pull/59)

## 1.0.2 [deprecated]

## 1.0.1 [2023-03-14]
- Fixes
  - 修复 `Metric req_rt导致的内存泄漏问题` [51](https://github.com/CeresDB/ceresdb-client-java/pull/51)
  - 修复 `查询数据返回多个结果时重复的问题` [52](https://github.com/CeresDB/ceresdb-client-java/pull/52)
  - 修复 `脏route缓存未被清理的问题` [53](https://github.com/CeresDB/ceresdb-client-java/pull/53)

## 1.0.0 [2023-02-28]
- Features
  - 更新 proto 版本到 v1.0.0 [45](https://github.com/CeresDB/ceresdb-client-java/pull/45)
  - 增加API `List<Column> Row.getColumns()` [44](https://github.com/CeresDB/ceresdb-client-java/pull/44)
- Fixes
  - 修复 `NPE for table error while creating table` [43](https://github.com/CeresDB/ceresdb-client-java/pull/43)
- Breaking Changes
  - 变更方法 `Value Row.getColumnValue(string)` 为 `Column Row.getColumn(string)`
  - 变更方法 `Object Value.getValue()` 为 `Object Value.getObject()`

## 1.0.0.alpha [2023-02-08]
- Features
  - [Ceresdb](https://github.com/CeresDB/ceresdb/tree/main) 稳定API的Java客户端版本，允许读写和管理数据表。
- Fixes
  -
- Breaking Changes
  -

