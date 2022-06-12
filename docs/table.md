## 如何创建一张表
CeresDB 是一个 Schema-less 的时序数据引擎，你可以不必创建 schema 就立刻写入数据（CeresDB 会根据你的第一次写入帮你创建一个默认的 schema）。
当然你也可以自行创建一个 schema 来更精细化的管理的表（比如索引等）

### 表规范
#### CeresDB 的表要有以下约束：
- 必须包含主键
- 必须包含时间列
- 主键必须包含时间列，并且只能包含一个时间列，该时间列为主时间列（默认时间列）
- 主键列必须是非空列，因此，时间列也必须是非空列
- 可以指定若干 tag 列

#### 主时间列
每个表都必须要有一个主时间列，用来映射到时序数据里的时间戳，如 opentsdb/prometheus 里的 timestamp 。建表时，用户必须指定表中一个 Timestamp
类型的列为主时间列，该列会自动映射到 opentsdb/prometheus 等协议里的 timestamp 。需要注意的是，主时间列的名字可以自定义，不需要一定叫 timestamp

#### 约束和选项
建表语句可以
- 通过 PRIMARY KEY 约束指定主键
- 通过 TIMESTAMP KEY 约束指定主时间列（默认时间列）
- 通过 TAG 选项指定 tag 列

上述约束可以全大写或者小写，在建表后均不可修改


## 建表例子

1. 指定 primary key 为 (c1, ts) ，这是一个复合主键。同时，指定主时间列为 ts
```
CREATE TABLE with_primary_key(
    ts TIMESTAMP NOT NULL,
    c1 STRING NOT NULL,
    c2 STRING NULL,
    c3 DOUBLE NULL,
    c4 STRING NULL,
    c5 STRING NULL,
    TIMESTAMP KEY(ts),
    PRIMARY KEY(c1, ts)
) ENGINE=Analytic WITH (ttl='7d');
```

2. 指定 primary key 为 (c1, ts)，指定主时间列为 ts ，指定 tag 为 c1，c2，c2。另外，可以注意到允许有其他时间戳列，这个列作为普通的数据列存在。
```
CREATE TABLE with_primary_key_tag(
    ts TIMESTAMP NOT NULL,
    c1 STRING TAG NOT NULL,
    c2 STRING TAG NULL,
    c3 STRING TAG NULL,
    c4 DOUBLE NULL,
    c5 STRING NULL,
    c6 STRING NULL,
    c7 TIMESTAMP NULL,
    TIMESTAMP KEY(ts),
    PRIMARY KEY(c1, ts)
) ENGINE=Analytic WITH (ttl='7d');
```

3. 部分场景下，可能用户并没有指定主键，但是会指定 tags 。这种情况下，会隐式地为用户创建一个叫 tsid 的列，并自动地设置主键为 (tsid, timestamp)
   其中 timestamp 为用户的主时间列，而 tsid 则是通过 tags 哈希生成。本质上这也是一种自动生成 id 的机制
```
CREATE TABLE with_tag(
    ts TIMESTAMP NOT NULL,
    c1 STRING TAG NOT NULL,
    c2 STRING TAG NULL,
    c3 STRING TAG NULL,
    c4 DOUBLE NULL,
    c5 STRING NULL,
    c6 STRING NULL,
    TIMESTAMP KEY(ts)
) ENGINE=Analytic WITH (ttl='7d');
```

因此，实际逻辑上该语句会变成类似以下形式的语句（注意并不存在这种语法）
```
CREATE TABLE with_tag(
    tsid AUTO_HASH_BY_TAGS,
    ts TIMESTAMP NOT NULL,
    c1 STRING TAG NOT NULL,
    c2 STRING TAG NULL,
    c3 STRING TAG NULL,
    c4 DOUBLE NULL,
    c5 STRING NULL,
    c6 STRING NULL,
    TIMESTAMP KEY(ts),
    PRIMARY KEY(tsid, ts)
) ENGINE=Analytic;
```

## DESCRIBE TABLE
```
DESCRIBE TABLE MY_FIRST_TABLE
```

## ALTER TABLE
```
ALTER TABLE MY_FIRST_TABLE ADD COLUMN col_20 UINT64
```

# EXISTS TABLE
```
EXISTS [TABLE] xxx_table
```
