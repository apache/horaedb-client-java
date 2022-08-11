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
 */
public class ReadmeTest {

    @Ignore @Test public void readmeTest() throws ExecutionException, InterruptedException {
        final CeresDBxOptions opts = CeresDBxOptions.newBuilder("127.0.0.1", 8831) // ceresdb default grpc port 8831
                .tenant("test", "sub_test", "test_token") // tenant info
                // maximum retry times when write fails
                // (only some error codes will be retried, such as the routing table failure)
                .writeMaxRetries(1)
                // maximum retry times when read fails
                // (only some error codes will be retried, such as the routing table failure)
                .readMaxRetries(1).build();

        final CeresDBxClient client = new CeresDBxClient();
        if (!client.init(opts)) {
            throw new IllegalStateException("Fail to start CeresDBxClient");
        }

        final long t0 = System.currentTimeMillis();
        final long t1 = t0 + 1000;
        final long t2 = t1 + 1000;
        final Rows data = Series.newBuilder("machine_metric").tag("city", "Singapore").tag("ip", "127.0.0.1")
                .toRowsBuilder()
                // codes below organizes 3 lines data (3 timestamps) for the `cpu` and `mem` column, this will just transport once through network. CeresDB encourage practices like this, because the SDK could use efficient compression algorithm to reduce network traffic and also be friendly to the sever side.
                .field(t0, "cpu", FieldValue.withDouble(0.23)) // first row, first column
                .field(t0, "mem", FieldValue.withDouble(0.55)) // first row, second column
                .field(t1, "cpu", FieldValue.withDouble(0.25)) // second row, first column
                .field(t1, "mem", FieldValue.withDouble(0.56)) // second row, second column
                .field(t2, "cpu", FieldValue.withDouble(0.21)) // third row, first column
                .field(t2, "mem", FieldValue.withDouble(0.52)) // third row, second column
                .build();

        final CompletableFuture<Result<WriteOk, Err>> wf = client.write(data);
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<WriteOk, Err> wr = wf.get();

        Assert.assertTrue(wr.isOk());
        Assert.assertEquals(3, wr.getOk().getSuccess());
        // `Result` class referenced the Rust language practice, provides rich functions (such as mapXXX, andThen) transforming the result value to improve programming efficiency. You can refer to the API docs for detail usage.
        Assert.assertEquals(3, wr.mapOr(0, WriteOk::getSuccess).intValue());
        Assert.assertEquals(0, wr.getOk().getFailed());
        Assert.assertEquals(0, wr.mapOr(-1, WriteOk::getFailed).intValue());

        final QueryRequest queryRequest = QueryRequest.newBuilder().forMetrics(
                "machine_metric") // table name is optional. If not provided, SQL parser will parse the `ql` to get the table name and do the routing automaticly
                .ql("select timestamp, cpu, mem from machine_metric") //
                .build();
        final CompletableFuture<Result<QueryOk, Err>> qf = client.query(queryRequest);
        // here the `future.get` is just for demonstration, a better async programming practice would be using the CompletableFuture API
        final Result<QueryOk, Err> qr = qf.get();

        Assert.assertTrue(qr.isOk());

        final QueryOk queryOk = qr.getOk();

        final List<Record> records = queryOk.mapToRecord().collect(Collectors.toList());
    }

}
