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
package io.ceresdb.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.ceresdb.common.util.Clock;
import io.ceresdb.models.Point;
import io.ceresdb.models.Result;
import io.ceresdb.models.Value;
import io.ceresdb.proto.internal.Common;
import io.ceresdb.proto.internal.Storage;

public class TestUtil {

    public static List<Point> newTablePoints(final String table) {
        final long time = Clock.defaultClock().getTick() - 1;
        return Point.newPointsBuilder(table) //
                .addPoint().setTimestamp(time).addTag("tag1", Value.withString("tag_v1")) //
                .addTag("tag2", Value.withString("tag_v2")) //
                .addField("field1", Value.withDouble(0.1)) //
                .addField("field2", Value.withString("string_value")) //
                .build().addPoint().setTimestamp(time + 1).addTag("tag1", Value.withString("tag_v1")) //
                .addTag("tag2", Value.withString("tag_v2")) //
                .addField("field1", Value.withDouble(0.2)) //
                .addField("field2", Value.withString("string_value_2")) //
                .build().build();
    }

    public static List<Point> newMultiTablePoints(final String... tables) {
        final List<Point> pointsList = new ArrayList<>();
        for (final String table : tables) {
            pointsList.addAll(newTablePoints(table));
        }
        return pointsList;
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
