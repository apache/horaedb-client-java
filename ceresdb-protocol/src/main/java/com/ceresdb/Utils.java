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
package com.ceresdb;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ceresdb.common.Display;
import com.ceresdb.common.Endpoint;
import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.ExecutorServiceHelper;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.common.util.NamedThreadFactory;
import com.ceresdb.common.util.ObjectPool;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.SharedScheduledPool;
import com.ceresdb.common.util.Spines;
import com.ceresdb.common.util.SystemPropertyUtil;
import com.ceresdb.common.util.ThreadPoolUtil;
import com.ceresdb.models.Err;
import com.ceresdb.models.FieldValue;
import com.ceresdb.models.Keyword;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.Schema;
import com.ceresdb.models.TagValue;
import com.ceresdb.models.Value;
import com.ceresdb.models.WriteOk;
import com.ceresdb.proto.Common;
import com.ceresdb.proto.Storage;
import com.ceresdb.rpc.Observer;
import com.google.protobuf.ByteStringHelper;

/**
 * Utils for CeresDBxClient.
 *
 * @author jiachun.fjc
 */
public final class Utils {

    public static final String DB_NAME = "CeresDB";

    private static final AtomicBoolean RW_LOGGING;

    private static final int                      REPORT_PERIOD_MIN;
    private static final ScheduledExecutorService DISPLAY;

    static {
        RW_LOGGING = new AtomicBoolean(SystemPropertyUtil.getBool(OptKeys.RW_LOGGING, true));
        REPORT_PERIOD_MIN = SystemPropertyUtil.getInt(OptKeys.REPORT_PERIOD, 30);
        DISPLAY = ThreadPoolUtil.newScheduledBuilder().poolName("display_self") //
                .coreThreads(1) //
                .enableMetric(true) //
                .threadFactory(new NamedThreadFactory("display_self", true)) //
                .rejectedHandler(new ThreadPoolExecutor.DiscardOldestPolicy()) //
                .build();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> ExecutorServiceHelper.shutdownAndAwaitTermination(DISPLAY)));
    }

    /**
     * Whether to output concise read/write logs.
     *
     * @return true or false
     */
    public static boolean isRwLogging() {
        return RW_LOGGING.get();
    }

    /**
     * See {@link #isRwLogging()}
     *
     * Reset `rwLogging`, set to the opposite of the old value.
     *
     * @return old value
     */
    public static boolean resetRwLogging() {
        return RW_LOGGING.getAndSet(!RW_LOGGING.get());
    }

    /**
     * Auto report self(CeresDBxClient) period.
     *
     * @return period with minutes
     */
    public static int autoReportPeriodMin() {
        return REPORT_PERIOD_MIN;
    }

    /**
     * Only used to schedule to display the self of client.
     *
     * @param display display
     * @param printer to print the display info
     */
    public static void scheduleDisplaySelf(final Display display, final Display.Printer printer) {
        DISPLAY.scheduleWithFixedDelay(() -> display.display(printer), 0, autoReportPeriodMin(), TimeUnit.MINUTES);
    }

    /**
     * Create a shared scheduler pool with the given name.
     *
     * @param name    scheduled pool's name
     * @param workers the num of workers
     * @return new scheduler poll instance
     */
    public static SharedScheduledPool getSharedScheduledPool(final String name, final int workers) {
        return new SharedScheduledPool(new ObjectPool.Resource<ScheduledExecutorService>() {

            @Override
            public ScheduledExecutorService create() {
                return ThreadPoolUtil.newScheduledBuilder() //
                        .poolName(name) //
                        .coreThreads(workers) //
                        .enableMetric(true) //
                        .threadFactory(new NamedThreadFactory(name, true)) //
                        .rejectedHandler(new ThreadPoolExecutor.DiscardOldestPolicy()) //
                        .build();
            }

            @Override
            public void close(final ScheduledExecutorService instance) {
                ExecutorServiceHelper.shutdownAndAwaitTermination(instance);
            }
        });
    }

    /**
     * Merge two given {@link Result} objects. If both Result objects are
     * {@link WriteOk} or {@link Err}, then merge their results. If one is
     * {@link Err} and the other is {@link WriteOk}, then we will discard
     * the {@link WriteOk}.
     *
     * @param r1 the result
     * @param r2 the other result
     * @return merged result
     */
    public static Result<WriteOk, Err> combineResult(final Result<WriteOk, Err> r1, final Result<WriteOk, Err> r2) {
        if (r1.isOk() && r2.isOk()) {
            return r1.getOk().combine(r2.getOk()).mapToResult();
        } else if (!r1.isOk() && !r2.isOk()) {
            return r1.getErr().combine(r2.getErr()).mapToResult();
        } else {
            if (r1.isOk()) {
                return r2.getErr().combine(r1.getOk()).mapToResult();
            } else {
                return r1.getErr().combine(r2.getOk()).mapToResult();
            }
        }
    }

    /**
     * Converts the given {@link Storage.WriteResponse} to {@link Result} that
     * upper-level readable.
     *
     * @param resp response of the write RPC
     * @param to   the server address wrote to
     * @param rows wrote date in this write
     * @return a {@link Result}
     */
    public static Result<WriteOk, Err> toResult(final Storage.WriteResponse resp, //
                                                final Endpoint to, //
                                                final Collection<Rows> rows) {
        final Common.ResponseHeader header = resp.getHeader();
        final int code = header.getCode();
        final String msg = header.getError();
        final int success = resp.getSuccess();
        final int failed = resp.getFailed();

        if (code == Result.SUCCESS) {
            final Set<String> metrics = rows != null && WriteOk.isCollectWroteDetail() ?
                    rows.stream().map(Rows::getMetric).collect(Collectors.toSet()) :
                    null;
            return WriteOk.ok(success, failed, metrics).mapToResult();
        } else {
            return Err.writeErr(code, msg, to, rows).mapToResult();
        }
    }

    /**
     * Converts the given {@link Storage.QueryResponse} to {@link Result} that
     * upper-level readable.
     *
     * @param resp       response of the write RPC
     * @param to         the server address wrote to
     * @param metrics    the metrics who query failed
     * @param errHandler the error handler
     * @return a {@link Result}
     */
    public static Result<QueryOk, Err> toResult(final Storage.QueryResponse resp, //
                                                final String ql, //
                                                final Endpoint to, //
                                                final Collection<String> metrics, final Runnable errHandler) {
        final Common.ResponseHeader header = resp.getHeader();
        final int code = header.getCode();
        final String msg = header.getError();

        if (code == Result.SUCCESS) {
            final int rowCount = resp.getRowsCount();
            final Stream<byte[]> rows = resp.getRowsList().stream().map(ByteStringHelper::sealByteArray);
            return QueryOk.ok(ql, toSchema(resp), rowCount, rows).mapToResult();
        } else {
            if (errHandler != null) {
                errHandler.run();
            }
            return Err.queryErr(code, msg, to, ql, metrics).mapToResult();
        }
    }

    private static Schema toSchema(final Storage.QueryResponse resp) {
        final Storage.QueryResponse.SchemaType type = resp.getSchemaType();
        final String content = resp.getSchemaContent();
        switch (type) {
            case AVRO:
                return Schema.schema(Schema.Type.Avro, content);
            case JSON:
                return Schema.schema(Schema.Type.Json, null);
            case UNRECOGNIZED:
            default:
                throw new IllegalArgumentException("Unrecognized schema type");
        }
    }

    /**
     * Determine whether the request was successful from the information in the
     * response header.
     *
     * @param header response header
     * @return true if response is success
     */
    public static boolean isSuccess(final Common.ResponseHeader header) {
        return header.getCode() == Result.SUCCESS;
    }

    /**
     * Returns a new CompletableFuture that is already completed with the given
     * value. Same as {@link CompletableFuture#completedFuture(Object)}, only
     * rename the method.
     *
     * @param value the given value
     * @param <U> the type of the value
     * @return the completed {@link CompletableFuture}
     */
    public static <U> CompletableFuture<U> completedCf(final U value) {
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Returns a new CompletableFuture that is already exceptionally with the given
     * error.
     *
     * @param t   the given exception
     * @param <U> the type of the value
     * @return the exceptionally {@link CompletableFuture}
     */
    public static <U> CompletableFuture<U> errorCf(final Throwable t) {
        final CompletableFuture<U> err = new CompletableFuture<>();
        err.completeExceptionally(t);
        return err;
    }

    /**
     * Break data stream into multiple requests based on the routing table
     * information given.
     *
     * @param data   the data to split
     * @param routes the route table info
     * @return multi data stream
     */
    public static Map<Endpoint, Collection<Rows>> splitDataByRoute(final Collection<Rows> data, //
                                                                   final Map<String /* metric */, Route> routes) {
        final Map<Endpoint, Collection<Rows>> splits = routes.values() //
                .stream() //
                .map(Route::getEndpoint) //
                .distinct() //
                .collect(Collectors.toMap(k -> k, k -> Spines.newBuf(), (v1, v2) -> v1));
        if (splits.size() == 1) {
            // fast path, zero copy
            splits.replaceAll((ep, empty) -> data);
        } else {
            data.forEach(rs -> {
                final Route route = routes.get(rs.getMetric());
                Requires.requireNonNull(route, "Null.route for " + rs);
                final Collection<Rows> partOf = splits.get(route.getEndpoint());
                Requires.requireNonNull(route, "Invalid.route " + route);
                partOf.add(rs);
            });
        }

        MetricsUtil.histogram("split_num_per_write").update(splits.size());

        return splits;
    }

    public static boolean shouldNotRetry(final Err err) {
        return !shouldRetry(err);
    }

    public static boolean shouldRetry(final Err err) {
        if (err == null) {
            return false;
        }
        final int errCode = err.getCode();
        return errCode == Result.INVALID_ROUTE || errCode == Result.SHOULD_RETRY;
    }

    public static boolean shouldRefreshRouteTable(final Err err) {
        return err.getCode() == Result.INVALID_ROUTE;
    }

    public static <V> Observer<V> toUnaryObserver(final CompletableFuture<V> future) {
        return new Observer<V>() {

            @Override
            public void onNext(final V value) {
                future.complete(value);
            }

            @Override
            public void onError(final Throwable err) {
                future.completeExceptionally(err);
            }
        };
    }

    public static Storage.Value toProtoValue(final FieldValue field) {
        final Storage.Value.Builder vb = Storage.Value.newBuilder();
        switch (field.getType()) {
            case Float64:
                return vb.setFloat64Value(field.getFloat64()).build();
            case String:
                return vb.setStringValue(field.getString()).build();
            case Int64:
                return vb.setInt64Value(field.getInt64()).build();
            case Float32:
                return vb.setFloat32Value(field.getFloat32()).build();
            case Int32:
                return vb.setInt32Value(field.getInt32()).build();
            case Int16:
                return vb.setInt16Value(field.getInt16()).build();
            case Int8:
                return vb.setInt8Value(field.getInt8()).build();
            case Boolean:
                return vb.setBoolValue(field.getBoolean()).build();
            case UInt64:
                return vb.setUint64Value(field.getUInt64()).build();
            case UInt32:
                return vb.setUint32Value(field.getUInt32()).build();
            case UInt16:
                return vb.setUint16Value(field.getUInt16()).build();
            case UInt8:
                return vb.setUint8Value(field.getUInt8()).build();
            case Timestamp:
                return vb.setTimestampValue(field.getTimestamp()).build();
            case Varbinary:
                return vb.setVarbinaryValue(ByteStringHelper.wrap(field.getVarbinary())).build();
            default:
                return invalidType(field);
        }
    }

    public static Storage.Value toProtoValue(final TagValue tag) {
        final Storage.Value.Builder vb = Storage.Value.newBuilder();
        switch (tag.getType()) {
            case String:
                return vb.setStringValue(tag.getString()).build();
            case Int64:
                return vb.setInt64Value(tag.getInt64()).build();
            case Int32:
                return vb.setInt32Value(tag.getInt32()).build();
            case Int16:
                return vb.setInt16Value(tag.getInt16()).build();
            case Int8:
                return vb.setInt8Value(tag.getInt8()).build();
            case Boolean:
                return vb.setBoolValue(tag.getBoolean()).build();
            case UInt64:
                return vb.setUint64Value(tag.getUInt64()).build();
            case UInt32:
                return vb.setUint32Value(tag.getUInt32()).build();
            case UInt16:
                return vb.setUint16Value(tag.getUInt16()).build();
            case UInt8:
                return vb.setUint8Value(tag.getUInt8()).build();
            case Timestamp:
                return vb.setTimestampValue(tag.getTimestamp()).build();
            case Varbinary:
                return vb.setVarbinaryValue(ByteStringHelper.wrap(tag.getVarbinary())).build();
            case Float32:
            case Float64:
            default:
                return invalidType(tag);
        }
    }

    public static long randomInitialDelay(final long delay) {
        return ThreadLocalRandom.current().nextLong(delay, delay << 1);
    }

    public static Properties loadProperties(final ClassLoader loader, final String name) throws IOException {
        final Properties prop = new Properties();
        prop.load(loader.getResourceAsStream(name));
        return prop;
    }

    public static <T> T unsupported(final String fmt, final Object... args) {
        throw new UnsupportedOperationException(String.format(fmt, args));
    }

    public static void checkKeywords(final Iterator<String> keys) {
        if (keys == null) {
            return;
        }

        while (keys.hasNext()) {
            ensureNotKeyword(keys.next());
        }
    }

    private static void ensureNotKeyword(final String name) {
        if (Keyword.isKeyword(name)) {
            throw new IllegalArgumentException("Invalid name, not allow keyword `" + name + '`');
        }
    }

    private static <T> T invalidType(final Value value) {
        throw new IllegalArgumentException("Invalid type " + value);
    }

    private Utils() {
    }
}
