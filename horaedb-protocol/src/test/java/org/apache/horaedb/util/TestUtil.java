/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package org.apache.horaedb.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.ceresdb.common.util.Clock;
import org.apache.horaedb.models.Point;
import org.apache.horaedb.models.Result;
import org.apache.horaedb.models.Value;
import io.ceresdb.proto.internal.Common;
import io.ceresdb.proto.internal.Storage;

public class TestUtil {

    public static List<Point> newTableTwoPoints(final String table) {
        final long time = Clock.defaultClock().getTick() - 1;

        List<Point> data = new ArrayList<>();
        data.add(Point.newPointBuilder(table) //
                .setTimestamp(time).addTag("tag1", Value.withString("tag_v1")) //
                .addTag("tag2", Value.withString("tag_v2")) //
                .addField("field1", Value.withDouble(0.1)) //
                .addField("field2", Value.withString("string_value")) //
                .build());
        data.add(Point.newPointBuilder(table).setTimestamp(time + 1).addTag("tag1", Value.withString("tag_v1")) //
                .addTag("tag2", Value.withString("tag_v2")) //
                .addField("field1", Value.withDouble(0.2)) //
                .addField("field2", Value.withString("string_value_2")) //
                .build());
        return data;
    }

    public static List<Point> newMultiTablePoints(final String... tables) {
        final List<Point> pointsList = new ArrayList<>();
        for (final String table : tables) {
            pointsList.addAll(newTableTwoPoints(table));
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
