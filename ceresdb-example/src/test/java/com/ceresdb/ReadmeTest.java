package com.ceresdb;

import com.ceresdb.models.*;
import com.ceresdb.options.CeresDBxOptions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author kesheng
 * @date 2022/8/9 5:52 下午
 */
public class ReadmeTest {

    @Ignore
    @Test
    public void readmeTest() throws ExecutionException, InterruptedException {
        // CeresDBx options
        final CeresDBxOptions opts = CeresDBxOptions.newBuilder("127.0.0.1", 8831) // CeresDB 默认配置端口号为 8831，不需要修改
                .tenant("test", "sub_test", "test_token") // 租户信息
                .writeMaxRetries(1) // 写入失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
                .readMaxRetries(1) // 查询失败重试次数上限（只有部分错误 code 才会重试，比如路由表失效）
                .build();

        final CeresDBxClient client = new CeresDBxClient();
        if (!client.init(opts)) {
            throw new IllegalStateException("Fail to start CeresDBxClient");
        }

        final long t0 = System.currentTimeMillis();
        final long t1 = t0 + 1000;
        final long t2 = t1 + 1000;
        final Rows data = Series.newBuilder("machine_metric")
                .tag("city", "Singapore")
                .tag("ip", "127.0.0.1")
                .toRowsBuilder()
                // 下面针对 cpu、mem 两列，一次写入了三行数据（3 个时间戳），CeresDB 鼓励这种实践，SDK 可以通过高效的压缩来减少网络传输，并且对 server 端写入非常友好
                .field(t0, "cpu", FieldValue.withDouble(0.23)) // 第 1 行第 1 列
                .field(t0, "mem", FieldValue.withDouble(0.55)) // 第 1 行第 2 列
                .field(t1, "cpu", FieldValue.withDouble(0.25)) // 第 2 行第 1 列
                .field(t1, "mem", FieldValue.withDouble(0.56)) // 第 2 行第 2 列
                .field(t2, "cpu", FieldValue.withDouble(0.21)) // 第 3 行第 1 列
                .field(t2, "mem", FieldValue.withDouble(0.52)) // 第 3 行第 2 列
                .build();

        final CompletableFuture<Result<WriteOk, Err>> wf = client.write(data);
        // 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
        final Result<WriteOk, Err> wr = wf.get();

        Assert.assertTrue(wr.isOk());
        Assert.assertEquals(3, wr.getOk().getSuccess());
        // `Result` 类参考了 Rust 语言，提供了丰富的 mapXXX、andThen 类 function 方便对结果值进行转换，提高编程效率，欢迎参考 API 文档使用
        Assert.assertEquals(3, wr.mapOr(0, WriteOk::getSuccess).intValue());
        Assert.assertEquals(0, wr.getOk().getFailed());
        Assert.assertEquals(0, wr.mapOr(-1, WriteOk::getFailed).intValue());

        final QueryRequest queryRequest = QueryRequest.newBuilder()
                .forMetrics("machine_metric") // 表名可选填，不填的话 SQL Parser 会自动解析 ql 涉及到的表名并完成自动路由
                .ql("select timestamp, cpu, mem from machine_metric") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> qf = client.query(queryRequest);
        // 这里用 `future.get` 只是方便演示，推荐借助 CompletableFuture 强大的 API 实现异步编程
        final Result<QueryOk, Err> qr = qf.get();

        Assert.assertTrue(qr.isOk());

        final QueryOk queryOk = qr.getOk();

        final List<Record> records = queryOk.mapToRecord().collect(Collectors.toList());
    }

}
