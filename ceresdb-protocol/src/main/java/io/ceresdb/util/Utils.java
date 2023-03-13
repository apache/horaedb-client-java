/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

import io.ceresdb.Route;
import io.ceresdb.common.Display;
import io.ceresdb.common.Endpoint;
import io.ceresdb.common.OptKeys;
import io.ceresdb.common.util.ExecutorServiceHelper;
import io.ceresdb.common.util.MetricsUtil;
import io.ceresdb.common.util.NamedThreadFactory;
import io.ceresdb.common.util.ObjectPool;
import io.ceresdb.common.util.Requires;
import io.ceresdb.common.util.SharedScheduledPool;
import io.ceresdb.common.util.Spines;
import io.ceresdb.common.util.SystemPropertyUtil;
import io.ceresdb.common.util.ThreadPoolUtil;
import io.ceresdb.models.Err;
import io.ceresdb.models.Keyword;
import io.ceresdb.models.Point;
import io.ceresdb.models.Row;
import io.ceresdb.models.SqlQueryOk;
import io.ceresdb.models.Result;
import io.ceresdb.models.Value;
import io.ceresdb.models.WriteOk;
import io.ceresdb.proto.internal.Common;
import io.ceresdb.proto.internal.Storage;
import io.ceresdb.rpc.Observer;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TimeStampMilliVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarBinaryVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.google.protobuf.ByteString;
import com.google.protobuf.ByteStringHelper;

/**
 * Utils for CeresDBClient.
 *
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
     * Auto report self(CeresDBClient) period.
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
     * @param points wrote date in this write
     * @return a {@link Result}
     */
    public static Result<WriteOk, Err> toResult(final Storage.WriteResponse resp, //
                                                final Endpoint to, //
                                                final List<Point> points) {
        final Common.ResponseHeader header = resp.getHeader();
        final int code = header.getCode();
        final String msg = header.getError();
        final int success = resp.getSuccess();
        final int failed = resp.getFailed();

        if (code == Result.SUCCESS) {
            final Set<String> tables = points != null && WriteOk.isCollectWroteDetail() ?
                    points.stream().map(Point::getTable).collect(Collectors.toSet()) :
                    null;
            return WriteOk.ok(success, failed, tables).mapToResult();
        } else {
            return Err.writeErr(code, msg, to, points).mapToResult();
        }
    }

    /**
     * Converts the given {@link Storage.SqlQueryResponse} to {@link Result} that
     * upper-level readable.
     *
     * @param resp       response of the write RPC
     * @param to         the server address wrote to
     * @param tables    the metrics who query failed
     * @param errHandler the error handler
     * @return a {@link Result}
     */
    public static Result<SqlQueryOk, Err> toResult(final Storage.SqlQueryResponse resp, //
                                                   final String sql, //
                                                   final Endpoint to, //
                                                   final Collection<String> tables, final Runnable errHandler) {
        final Common.ResponseHeader header = resp.getHeader();
        final int code = header.getCode();
        final String msg = header.getError();

        if (code != Result.SUCCESS) {
            if (errHandler != null) {
                errHandler.run();
            }
            return Err.queryErr(code, msg, to, sql, tables).mapToResult();
        }

        if (resp.getArrow().getRecordBatchesCount() == 0) {
            return SqlQueryOk.ok(sql, resp.getAffectedRows(), null).mapToResult();
        }

        List<Row> rows = resp.getArrow().getRecordBatchesList().stream()
                .flatMap(batch -> parseArrowBatch(batch, resp.getArrow().getCompression()).stream())
                .collect(Collectors.toList());

        return SqlQueryOk.ok(sql, resp.getAffectedRows(), rows).mapToResult();
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
    public static Map<Endpoint, List<Point>> splitDataByRoute(final List<Point> data, //
                                                              final Map<String /* table */, Route> routes) {
        final Map<Endpoint, List<Point>> splits = routes.values() //
                .stream() //
                .map(Route::getEndpoint) //
                .distinct() //
                .collect(Collectors.toMap(k -> k, k -> Spines.newBuf(), (v1, v2) -> v1));
        if (splits.size() == 1) {
            // fast path, zero copy
            splits.replaceAll((ep, empty) -> data);
        } else {
            data.forEach(rs -> {
                final Route route = routes.get(rs.getTable());
                Requires.requireNonNull(route, "Null.route for " + rs);
                final Collection<Point> partOf = splits.get(route.getEndpoint());
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

    public static Storage.Value toProtoValue(final Value value) {
        final Storage.Value.Builder vb = Storage.Value.newBuilder();
        switch (value.getDataType()) {
            case Double:
                return vb.setFloat64Value(value.getDouble()).build();
            case String:
                return vb.setStringValue(value.getString()).build();
            case Int64:
                return vb.setInt64Value(value.getInt64()).build();
            case Float:
                return vb.setFloat32Value(value.getFloat()).build();
            case Int32:
                return vb.setInt32Value(value.getInt32()).build();
            case Int16:
                return vb.setInt16Value(value.getInt16()).build();
            case Int8:
                return vb.setInt8Value(value.getInt8()).build();
            case Boolean:
                return vb.setBoolValue(value.getBoolean()).build();
            case UInt64:
                return vb.setUint64Value(value.getUInt64()).build();
            case UInt32:
                return vb.setUint32Value(value.getUInt32()).build();
            case UInt16:
                return vb.setUint16Value(value.getUInt16()).build();
            case UInt8:
                return vb.setUint8Value(value.getUInt8()).build();
            case Timestamp:
                return vb.setTimestampValue(value.getTimestamp()).build();
            case Varbinary:
                return vb.setVarbinaryValue(ByteStringHelper.wrap(value.getVarbinary())).build();
            default:
                return invalidType(value);
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

    private static List<Row> parseArrowBatch(ByteString batch, Storage.ArrowPayload.Compression compression) {
        try {
            InputStream arrowStream = batch.newInput();
            if (compression == Storage.ArrowPayload.Compression.ZSTD) {
                byte[] batchBuffer = batch.toByteArray();
                long decompressedSize = Zstd.decompressedSize(batchBuffer);
                if (decompressedSize > 0) {
                    // batch compress mode
                    byte[] decompressedByteBuffer = Zstd.decompress(batchBuffer, (int) decompressedSize);
                    arrowStream = new ByteArrayInputStream(decompressedByteBuffer);
                } else {
                    // stream compress mode
                    ZstdInputStream zstdInputStream = new ZstdInputStream(batch.newInput());
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(batch.size());
                    byte[] block = new byte[128 * 1024];
                    int size = zstdInputStream.read(block);
                    byteArrayOutputStream.write(block);
                    while (size > 0) {
                        size = zstdInputStream.read(block);
                        byteArrayOutputStream.write(block);
                    }
                    arrowStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                }
            }

            ArrowStreamReader arrowStreamReader = new ArrowStreamReader(arrowStream, new RootAllocator());

            VectorSchemaRoot readRoot = arrowStreamReader.getVectorSchemaRoot();
            Schema schema = readRoot.getSchema();

            while (arrowStreamReader.loadNextBatch()) {
                return parseArrowRecord(schema, readRoot);
            }
            return Collections.EMPTY_LIST;
        } catch (IOException e) {
            return Collections.EMPTY_LIST;
        }
    }

    private static List<Row> parseArrowRecord(Schema schema, VectorSchemaRoot root) {
        // init row builders
        List<Row.RowBuilder> builders = new ArrayList<>(root.getRowCount());
        for (int i = 0; i < root.getRowCount(); i++) {
            builders.add(Row.newRowBuilder(schema.getFields().size()));
        }

        String[] fields = new String[schema.getFields().size()];
        for (int fieldIdx = 0; fieldIdx < schema.getFields().size(); fieldIdx++) {
            Field field = schema.getFields().get(fieldIdx);
            fields[fieldIdx] = field.getName();

            FieldVector vector = root.getVector(fieldIdx);
            switch (Types.getMinorTypeForArrowType(field.getType())) {
                case VARCHAR:
                    VarCharVector varCharVector = (VarCharVector) vector;
                    for (int rowIdx = 0; rowIdx < varCharVector.getValueCount(); rowIdx++) {
                        if (varCharVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withStringOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx,
                                    Value.withString(new String(varCharVector.get(rowIdx))));
                        }
                    }
                    break;
                case BIT:
                    BitVector bitVector = (BitVector) vector;
                    for (int rowIdx = 0; rowIdx < bitVector.getValueCount(); rowIdx++) {
                        if (bitVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withBooleanOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withBoolean(bitVector.get(rowIdx) > 0));
                        }
                    }
                    break;
                case FLOAT8:
                    Float8Vector float8Vector = (Float8Vector) vector;
                    for (int rowIdx = 0; rowIdx < float8Vector.getValueCount(); rowIdx++) {
                        if (float8Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withDoubleOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withDouble(float8Vector.get(rowIdx)));
                        }
                    }
                    break;
                case FLOAT4:
                    Float4Vector float4Vector = (Float4Vector) vector;
                    for (int rowIdx = 0; rowIdx < float4Vector.getValueCount(); rowIdx++) {
                        if (float4Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withFloatOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withFloat(float4Vector.get(rowIdx)));
                        }
                    }
                    break;
                case BIGINT:
                    BigIntVector bigIntVector = (BigIntVector) vector;
                    for (int rowIdx = 0; rowIdx < bigIntVector.getValueCount(); rowIdx++) {
                        if (bigIntVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt64OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt64(bigIntVector.get(rowIdx)));
                        }
                    }
                    break;
                case INT:
                    IntVector intVector = (IntVector) vector;
                    for (int rowIdx = 0; rowIdx < intVector.getValueCount(); rowIdx++) {
                        if (intVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt32OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt32(intVector.get(rowIdx)));
                        }
                    }
                    break;
                case SMALLINT:
                    SmallIntVector smallIntVector = (SmallIntVector) vector;
                    for (int rowIdx = 0; rowIdx < smallIntVector.getValueCount(); rowIdx++) {
                        if (smallIntVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt16OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt16(smallIntVector.get(rowIdx)));
                        }
                    }
                    break;
                case TINYINT:
                    TinyIntVector tinyIntVector = (TinyIntVector) vector;
                    for (int rowIdx = 0; rowIdx < tinyIntVector.getValueCount(); rowIdx++) {
                        if (tinyIntVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt8OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withInt8(tinyIntVector.get(rowIdx)));
                        }
                    }
                    break;
                case UINT8:
                    UInt8Vector uInt8Vector = (UInt8Vector) vector;
                    for (int rowIdx = 0; rowIdx < uInt8Vector.getValueCount(); rowIdx++) {
                        if (uInt8Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt64OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt64(uInt8Vector.get(rowIdx)));
                        }
                    }
                    break;
                case UINT4:
                    UInt4Vector uInt4Vector = (UInt4Vector) vector;
                    for (int rowIdx = 0; rowIdx < uInt4Vector.getValueCount(); rowIdx++) {
                        if (uInt4Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt32OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt32(uInt4Vector.get(rowIdx)));
                        }
                    }
                    break;
                case UINT2:
                    UInt2Vector uInt2Vector = (UInt2Vector) vector;
                    for (int rowIdx = 0; rowIdx < uInt2Vector.getValueCount(); rowIdx++) {
                        if (uInt2Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt16OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt16(uInt2Vector.get(rowIdx)));
                        }
                    }
                    break;
                case UINT1:
                    UInt1Vector uInt1Vector = (UInt1Vector) vector;
                    for (int rowIdx = 0; rowIdx < uInt1Vector.getValueCount(); rowIdx++) {
                        if (uInt1Vector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt8OrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withUInt8(uInt1Vector.get(rowIdx)));
                        }
                    }
                    break;
                case TIMESTAMPMILLI:
                    TimeStampMilliVector timeStampMilliVector = (TimeStampMilliVector) vector;
                    for (int rowIdx = 0; rowIdx < timeStampMilliVector.getValueCount(); rowIdx++) {
                        if (timeStampMilliVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withTimestampOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx,
                                    Value.withTimestamp(timeStampMilliVector.get(rowIdx)));
                        }
                    }
                    break;
                case VARBINARY:
                    VarBinaryVector varBinaryVector = (VarBinaryVector) vector;
                    for (int rowIdx = 0; rowIdx < varBinaryVector.getValueCount(); rowIdx++) {
                        if (varBinaryVector.isNull(rowIdx)) {
                            builders.get(rowIdx).setValue(fieldIdx, Value.withVarbinaryOrNull(null));
                        } else {
                            builders.get(rowIdx).setValue(fieldIdx,
                                    Value.withVarbinaryOrNull(varBinaryVector.get(rowIdx)));
                        }
                    }
                    break;
                default:
                    // paas

            }
        }

        builders.stream().forEach(builder -> builder.setFields(fields));
        return builders.stream().map(builder -> builder.build()).collect(Collectors.toList());
    }

    private Utils() {
    }
}
