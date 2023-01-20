/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ceresdb;

import io.ceresdb.models.*;
import io.ceresdb.options.CeresDBOptions;
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

    @Ignore
    @Test
    public void readmeTest() throws ExecutionException, InterruptedException {
        final CeresDBOptions opts = CeresDBOptions.newBuilder("127.0.0.1", 8831) // CeresDB default grpc port 8831
                .managementAddress("127.0.0.1", 5000) // CeresDB default http port 3307
                .tenant("public", "sub_test", "test_token") // tenant info
                // maximum retry times when write fails
                // (only some error codes will be retried, such as the routing table failure)
                .writeMaxRetries(1)
                // maximum retry times when read fails
                // (only some error codes will be retried, such as the routing table failure)
                .readMaxRetries(1).build();

        final CeresDBClient client = new CeresDBClient();
        if (!client.init(opts)) {
            throw new IllegalStateException("Fail to start CeresDBClient");
        }

        // Create table manually, creating table schema ahead of data ingestion is not required
        String createTableSql = "CREATE TABLE MY_FIRST_TABLE(" + "ts TIMESTAMP NOT NULL," + "c1 STRING TAG NOT NULL,"
                                + "c2 STRING TAG NOT NULL," + "c3 DOUBLE NULL," + "c4 STRING NULL," + "c5 INT64 NULL,"
                                + "c6 FLOAT NULL," + "c7 INT32 NULL," + "c8 INT16 NULL," + "c9 INT8 NULL,"
                                + "c10 BOOLEAN NULL," + "c11 UINT64 NULL," + "c12 UINT32 NULL," + "c13 UINT16 NULL,"
                                + "c14 UINT8 NULL," + "c15 TIMESTAMP NULL," + "c16 VARBINARY NULL,"
                                + "TIMESTAMP KEY(ts)" + ") ENGINE=Analytic";
        SqlResult result = client.management().executeSql(createTableSql);

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

        final SqlQueryRequest queryRequest = SqlQueryRequest.newBuilder().forMetrics("machine_metric") // table name is optional. If not provided, SQL parser will parse the `ql` to get the table name and do the routing automaticly
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
