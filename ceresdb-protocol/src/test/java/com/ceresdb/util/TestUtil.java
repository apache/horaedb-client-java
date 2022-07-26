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
package com.ceresdb.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ceresdb.common.util.Clock;
import com.ceresdb.models.FieldValue;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.Series;
import com.ceresdb.proto.Common;
import com.ceresdb.proto.Storage;

/**
 * @author jiachun.fjc
 */
public class TestUtil {

    public static Rows newRow(final String metric) {
        final long time = Clock.defaultClock().getTick() - 1;
        return Series.newBuilder(metric) //
                .tag("tag1", "tag_v1") //
                .tag("tag2", "tag_v2") //
                .toRowsBuilder() //
                .field(time, "field1", FieldValue.withDouble(0.1)) //
                .field(time, "field2", FieldValue.withString("string_value")) //
                .field(time + 1, "field1", FieldValue.withDouble(0.2)) //
                .field(time + 1, "field2", FieldValue.withString("string_value_2")) //
                .build();
    }

    public static List<Rows> newListOfRows(final String... metrics) {
        final List<Rows> rowsList = new ArrayList<>();
        for (final String metric : metrics) {
            rowsList.add(newRow(metric));
        }
        return rowsList;
    }

    public static Storage.WriteResponse newSuccessWriteResp(final int success) {
        final Common.ResponseHeader header = Common.ResponseHeader.newBuilder() //
                .setCode(Result.SUCCESS) //
                .build();
        return Storage.WriteResponse.newBuilder() //
                .setHeader(header) //
                .setSuccess(success) //
                .build();
    }

    public static Storage.WriteResponse newFailedWriteResp(final int errCode, final int failed) {
        return newFailedWriteResp(errCode, "test err", failed);
    }

    public static Storage.WriteResponse newFailedWriteResp(final int errCode, final String err, final int failed) {
        final Common.ResponseHeader errHeader = Common.ResponseHeader.newBuilder() //
                .setCode(errCode) //
                .setError(err) //
                .build();
        return Storage.WriteResponse.newBuilder() //
                .setHeader(errHeader) //
                .setFailed(failed) //
                .build();
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> asSet(final T... values) {
        final Set<T> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }
}
