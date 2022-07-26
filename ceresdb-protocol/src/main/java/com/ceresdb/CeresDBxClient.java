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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.common.Display;
import com.ceresdb.common.Endpoint;
import com.ceresdb.common.Lifecycle;
import com.ceresdb.common.signal.SignalHandlersLoader;
import com.ceresdb.common.util.MetricExecutor;
import com.ceresdb.common.util.MetricsUtil;
import com.ceresdb.models.Err;
import com.ceresdb.models.QueryOk;
import com.ceresdb.models.QueryRequest;
import com.ceresdb.models.Result;
import com.ceresdb.models.Rows;
import com.ceresdb.models.WriteOk;
import com.ceresdb.options.CeresDBxOptions;
import com.ceresdb.options.ManagementOptions;
import com.ceresdb.options.QueryOptions;
import com.ceresdb.options.RouterOptions;
import com.ceresdb.options.WriteOptions;
import com.ceresdb.rpc.Context;
import com.ceresdb.rpc.Observer;
import com.ceresdb.rpc.RpcClient;
import com.ceresdb.rpc.RpcFactoryProvider;
import com.ceresdb.rpc.RpcOptions;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;

/**
 * CeresDB client.
 *
 * @author jiachun.fjc
 */
public class CeresDBxClient implements Write, Query, Lifecycle<CeresDBxOptions>, Display {

    private static final Logger LOG = LoggerFactory.getLogger(CeresDBxClient.class);

    private static final Map<Integer, CeresDBxClient> INSTANCES   = new ConcurrentHashMap<>();
    private static final AtomicInteger                ID          = new AtomicInteger(0);
    private static final String                       ID_KEY      = "client.id";
    private static final String                       VERSION_KEY = "client.version";
    private static final String                       VERSION     = loadVersion();

    private final int           id;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private CeresDBxOptions opts;
    private RouterClient    routerClient;
    private WriteClient     writeClient;
    private QueryClient     queryClient;
    // CeresDBxClient is only intended to manage the instance and does not
    // intend to broker any of its behavior
    private Management management;
    // Note: We do not close it to free resources, as we view it as shared
    private Executor asyncWritePool;
    private Executor asyncReadPool;

    static {
        // load all signal handlers
        SignalHandlersLoader.load();
        // register all rpc service
        RpcServiceRegister.registerStorageService();
        // start scheduled metric reporter
        MetricsUtil.startScheduledReporter(Utils.autoReportPeriodMin(), TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(MetricsUtil::stopScheduledReporterAndDestroy));
    }

    public CeresDBxClient() {
        this.id = ID.incrementAndGet();
    }

    @Override
    public boolean init(final CeresDBxOptions opts) {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("CeresDBx client has started");
        }

        this.opts = CeresDBxOptions.check(opts).copy();

        final RpcClient rpcClient = initRpcClient(this.opts);
        this.routerClient = initRouteClient(this.opts, rpcClient);
        this.asyncWritePool = withMetricPool(this.opts.getAsyncWritePool(), "async_write_pool.time");
        this.asyncReadPool = withMetricPool(this.opts.getAsyncReadPool(), "async_read_pool.time");
        this.writeClient = initWriteClient(this.opts, this.routerClient, this.asyncWritePool);
        this.queryClient = initQueryClient(this.opts, this.routerClient, this.asyncReadPool);
        this.management = initManagementClient(this.opts, this.routerClient);

        INSTANCES.put(this.id, this);

        Utils.scheduleDisplaySelf(this, new LogPrinter(LOG));

        return true;
    }

    @Override
    public void shutdownGracefully() {
        if (!this.started.compareAndSet(true, false)) {
            return;
        }

        if (this.writeClient != null) {
            this.writeClient.shutdownGracefully();
        }

        if (this.queryClient != null) {
            this.queryClient.shutdownGracefully();
        }

        if (this.routerClient != null) {
            this.routerClient.shutdownGracefully();
        }

        if (this.management != null) {
            this.management.shutdownGracefully();
        }

        INSTANCES.remove(this.id);
    }

    @Override
    public void ensureInitialized() {
        if (this.started.get() && INSTANCES.containsKey(this.id)) {
            return;
        }
        throw new IllegalStateException(String.format("CeresDBxClient(%d) is not started", this.id));
    }

    @Override
    public CompletableFuture<Result<WriteOk, Err>> write(final Collection<Rows> data, final Context ctx) {
        ensureInitialized();
        return this.writeClient.write(data, attachCtx(ctx));
    }

    @Override
    public StreamWriteBuf<Rows, WriteOk> streamWrite(final String metric, final Context ctx) {
        ensureInitialized();
        return this.writeClient.streamWrite(metric, attachCtx(ctx));
    }

    @Override
    public CompletableFuture<Result<QueryOk, Err>> query(final QueryRequest req, final Context ctx) {
        ensureInitialized();
        return this.queryClient.query(req, attachCtx(ctx));
    }

    @Override
    public void streamQuery(final QueryRequest req, final Context ctx, final Observer<QueryOk> observer) {
        ensureInitialized();
        this.queryClient.streamQuery(req, attachCtx(ctx), observer);
    }

    public boolean hasManagement() {
        return this.management != null;
    }

    public Management management() {
        return this.management;
    }

    private Executor withMetricPool(final Executor pool, final String name) {
        return pool == null ? null : new MetricExecutor(pool, name);
    }

    private Context attachCtx(final Context ctx) {
        final Context c = ctx == null ? Context.newDefault() : ctx;
        return c.with(ID_KEY, id()).with(VERSION_KEY, version());
    }

    public int id() {
        return this.id;
    }

    public String version() {
        return VERSION;
    }

    public Executor asyncWritePool() {
        return this.asyncWritePool;
    }

    public Executor asyncReadPool() {
        return this.asyncReadPool;
    }

    public RouterClient routerClient() {
        return this.routerClient;
    }

    public WriteClient writeClient() {
        return this.writeClient;
    }

    public QueryClient queryClient() {
        return this.queryClient;
    }

    @Override
    public void display(final Printer out) {
        out.println("--- CeresDBxClient ---") //
                .print("id=") //
                .println(this.id) //
                .print("version=") //
                .println(version()) //
                .print("clusterAddress=") //
                .println(this.opts.getClusterAddress()) //
                .print("tenant=") //
                .println(this.opts.getTenant().getTenant()) //
                .print("userAsyncWritePool=") //
                .println(this.opts.getAsyncWritePool()) //
                .print("userAsyncReadPool=") //
                .println(this.opts.getAsyncReadPool());

        if (this.routerClient != null) {
            out.println("");
            this.routerClient.display(out);
        }

        if (this.writeClient != null) {
            out.println("");
            this.writeClient.display(out);
        }

        if (this.queryClient != null) {
            out.println("");
            this.queryClient.display(out);
        }

        if (this.management != null) {
            out.println("");
            this.management.display(out);
        }

        out.println("");
    }

    @Override
    public String toString() {
        return "CeresDBxClient{" + //
               "id=" + id + //
               "version=" + version() + //
               ", started=" + started + //
               ", opts=" + opts + //
               ", writeClient=" + writeClient + //
               ", asyncWritePool=" + asyncWritePool + //
               ", asyncReadPool=" + asyncReadPool + //
               '}';
    }

    public static List<CeresDBxClient> instances() {
        return new ArrayList<>(INSTANCES.values());
    }

    private static RpcClient initRpcClient(final CeresDBxOptions opts) {
        final RpcOptions rpcOpts = opts.getRpcOptions();
        rpcOpts.setTenant(opts.getTenant());
        final RpcClient rpcClient = RpcFactoryProvider.getRpcFactory().createRpcClient();
        if (!rpcClient.init(rpcOpts)) {
            throw new IllegalStateException("Fail to start RPC client");
        }
        rpcClient.registerConnectionObserver(new RpcConnectionObserver());
        return rpcClient;
    }

    private static RouterClient initRouteClient(final CeresDBxOptions opts, final RpcClient rpcClient) {
        final RouterOptions routerOpts = opts.getRouterOptions();
        routerOpts.setRpcClient(rpcClient);
        final RouterClient routerClient = new RouterClient();
        if (!routerClient.init(routerOpts)) {
            throw new IllegalStateException("Fail to start router client");
        }
        return routerClient;
    }

    private static WriteClient initWriteClient(final CeresDBxOptions opts, //
                                               final RouterClient routerClient, //
                                               final Executor asyncPool) {
        final WriteOptions writeOpts = opts.getWriteOptions();
        writeOpts.setRoutedClient(routerClient);
        writeOpts.setAsyncPool(asyncPool);
        final WriteClient writeClient = new WriteClient();
        if (!writeClient.init(writeOpts)) {
            throw new IllegalStateException("Fail to start write client");
        }
        return writeClient;
    }

    private static QueryClient initQueryClient(final CeresDBxOptions opts, //
                                               final RouterClient routerClient, //
                                               final Executor asyncPool) {
        final QueryOptions queryOpts = opts.getQueryOptions();
        queryOpts.setRouterClient(routerClient);
        queryOpts.setAsyncPool(asyncPool);
        final QueryClient queryClient = new QueryClient();
        if (!queryClient.init(queryOpts)) {
            throw new IllegalStateException("Fail to start query client");
        }
        return queryClient;
    }

    private static Management initManagementClient(final CeresDBxOptions opts, final RouterClient routerClient) {
        final ManagementOptions mOpts = opts.getManagementOptions();
        if (mOpts == null) {
            return null;
        }
        if (!CeresDBxManagementProvider.hasManagement()) {
            return null;
        }
        final Management management = CeresDBxManagementProvider.createManagement();
        mOpts.setRouterClient(routerClient);
        if (!management.init(mOpts)) {
            return null;
        }
        return management;
    }

    private static String loadVersion() {
        try {
            return Utils //
                    .loadProperties(CeresDBxClient.class.getClassLoader(), "client_version.properties") //
                    .getProperty(VERSION_KEY, "Unknown version");
        } catch (final Exception ignored) {
            return "Unknown version(err)";
        }
    }

    static final class RpcConnectionObserver implements RpcClient.ConnectionObserver {

        static final Counter CONN_COUNTER  = MetricsUtil.counter("connection_counter");
        static final Meter   CONN_FAILURES = MetricsUtil.meter("connection_failures");

        @Override
        public void onReady(final Endpoint ep) {
            CONN_COUNTER.inc();
            MetricsUtil.counter("connection_counter", ep).inc();
        }

        @Override
        public void onFailure(final Endpoint ep) {
            CONN_COUNTER.dec();
            CONN_FAILURES.mark();
            MetricsUtil.counter("connection_counter", ep).dec();
            MetricsUtil.meter("connection_failures", ep).mark();
        }

        @Override
        public void onShutdown(final Endpoint ep) {
            CONN_COUNTER.dec();
            MetricsUtil.counter("connection_counter", ep).dec();
        }
    }

    /**
     * A printer use logger, the {@link #print(Object)} writes data to
     * an inner buffer, the {@link #println(Object)} actually writes
     * data to the logger, so we must call {@link #println(Object)}
     * on the last writing.
     */
    static final class LogPrinter implements Display.Printer {

        private static final int MAX_BUF_SIZE = 1024 << 3;

        private final Logger logger;

        private StringBuilder buf = new StringBuilder();

        LogPrinter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public synchronized Printer print(final Object x) {
            this.buf.append(x);
            return this;
        }

        @Override
        public synchronized Printer println(final Object x) {
            this.buf.append(x);
            this.logger.info(this.buf.toString());
            truncateBuf();
            this.buf.setLength(0);
            return this;
        }

        private void truncateBuf() {
            if (this.buf.capacity() < MAX_BUF_SIZE) {
                this.buf.setLength(0); // reuse
            } else {
                this.buf = new StringBuilder();
            }
        }
    }
}
