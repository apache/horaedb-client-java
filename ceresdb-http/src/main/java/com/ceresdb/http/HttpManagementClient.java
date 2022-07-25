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
package com.ceresdb.http;

import java.util.Collection;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceresdb.Management;
import com.ceresdb.MetricParser;
import com.ceresdb.MetricParserFactory;
import com.ceresdb.MetricParserFactoryProvider;
import com.ceresdb.Route;
import com.ceresdb.RouterClient;
import com.ceresdb.common.Endpoint;
import com.ceresdb.common.Tenant;
import com.ceresdb.common.util.AuthUtil;
import com.ceresdb.common.util.Requires;
import com.ceresdb.common.util.Strings;
import com.ceresdb.common.util.internal.ThrowUtil;
import com.ceresdb.http.errors.ManagementException;
import com.ceresdb.models.SqlResult;
import com.ceresdb.options.ManagementOptions;
import com.ceresdb.rpc.Context;
import com.google.gson.Gson;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A management API client that communicates with the server using HTTP protocol.
 *
 * @author jiachun.fjc
 */
public class HttpManagementClient implements Management {

    private static final Logger LOG = LoggerFactory.getLogger(HttpManagementClient.class);

    private static final String AFFECTED_ROWS = "affected_rows";
    private static final String ROWS          = "rows";

    private final AtomicBoolean started = new AtomicBoolean(false);

    private ManagementOptions opts;

    private Tenant       tenant;
    private RouterClient routerClient;

    @Override
    public boolean init(final ManagementOptions opts) {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("Http management client has started");
        }
        this.opts = Requires.requireNonNull(opts, "Null.opts");
        this.tenant = Requires.requireNonNull(opts.getTenant(), "Null.Tenant");
        this.routerClient = Requires.requireNonNull(opts.getRouterClient(), "Null.RouterClient");
        return true;
    }

    @Override
    public void shutdownGracefully() {
        if (!this.started.compareAndSet(true, false)) {
            return;
        }
        this.opts = null;
    }

    @Override
    public void display(final Printer out) {
        out.println("--- HttpManagementClient ---") //
                .print("started=") //
                .println(this.started) //
                .print("tenant=") //
                .println(this.tenant.getTenant());
    }

    @Override
    public SqlResult executeSql(final boolean autoRouting, final Context ctx, final String fmtSql,
                                final Object... args) {
        final String sql = getSql(fmtSql, args);
        final Endpoint managementAddress = this.opts.getManagementAddress();

        if (!autoRouting && !this.opts.isCheckSql()) {
            return doExecuteSql(sql, managementAddress, ctx);
        }

        final MetricParser parser = parseSql(sql);

        if (!autoRouting) {
            return doExecuteSql(sql, managementAddress, ctx);
        }

        Requires.requireNonNull(parser, "Null.parser");

        final List<String> tables = parser.metricNames();

        Requires.requireNonNull(tables, "Null.tables");
        Requires.requireTrue(!tables.isEmpty(), "Empty.tables");

        try {
            final Map<String, Route> routes = this.routerClient.routeFor(tables).get();
            final Endpoint target = getTargetEndpoint(routes.values());

            return doExecuteSql(sql, target, ctx);
        } catch (final Exception e) {
            ThrowUtil.throwException(e);
        }

        return null;
    }

    private SqlResult doExecuteSql(final String sql, final Endpoint endpoint, final Context ctx) {
        final Request request = contextToHeaders(newBaseRequestBuilder(), ctx) //
                .url(getUrl(endpoint)) //
                .post(HttpUtil.requestBody(HttpUtil.params("query", sql))) //
                .build();

        LOG.info("Executing sql: {}, to: {}.", sql, endpoint);

        try (Response resp = HttpUtil.httpClient().newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new ManagementException(
                        String.format("Execute sql [%s] error from server %s, err_code=%d, err_msg=%s, detail_msg=%s", //
                                sql, endpoint, resp.code(), resp.message(), getRespBody(resp)));
            }
            return toSqlResult(resp);
        } catch (final Throwable t) {
            LOG.error("Fail to execute sql: {}.", sql, t);
            ThrowUtil.throwException(t);
        }
        return null; // never got here
    }

    private Endpoint getTargetEndpoint(final Collection<Route> routes) {
        final Endpoint managementAddress = this.opts.getManagementAddress();

        return routes == null ? managementAddress :
                routes.stream().findFirst().map(r -> Endpoint.of(r.getEndpoint().getIp(), managementAddress.getPort()))
                        .orElse(managementAddress);
    }

    private String getSql(final String fmtSql, final Object... args) {
        return String.format(fmtSql, args);
    }

    private MetricParser parseSql(final String sql) {
        final MetricParser sqlParser = getSqlParser(sql);

        if (sqlParser == null || !this.opts.isCheckSql()) {
            return sqlParser;
        }

        return checkStatementType(sqlParser);
    }

    private Request.Builder contextToHeaders(final Request.Builder builder, final Context ctx) {
        if (ctx == null) {
            return builder;
        }

        ctx.entrySet().forEach(e -> builder.addHeader(e.getKey(), String.valueOf(e.getValue())));
        return builder;
    }

    private Request.Builder newBaseRequestBuilder() {
        return authHeaders(new Request.Builder()) //
                .addHeader("Content-Type", "application/json");
    }

    private Request.Builder authHeaders(final Request.Builder builder) {
        AuthUtil.authHeaders(this.tenant).forEach(builder::addHeader);
        return builder;
    }

    private static String getUrl(final Endpoint endpoint) {
        Requires.requireNonNull(endpoint, "endpoint");
        return String.format("%s://%s:%d/sql", HttpUtil.PROTOCOL, endpoint.getIp(), endpoint.getPort());
    }

    @SuppressWarnings("unchecked")
    private static SqlResult toSqlResult(final Response resp) throws IOException {
        final String body = getRespBody(resp);
        if (Strings.isBlank(body)) {
            return SqlResult.EMPTY_RESULT;
        }

        final Map<String, Object> mapResult = new Gson().fromJson(body, Map.class);
        final Number affectedRows = (Number) mapResult.get(AFFECTED_ROWS);
        final List<Map<String, Object>> rows = (List<Map<String, Object>>) mapResult.get(ROWS);
        return new SqlResult(affectedRows == null ? 0L : affectedRows.longValue(), rows);
    }

    private static String getRespBody(final Response resp) throws IOException {
        final ResponseBody body = resp.body();
        return body == null ? "" : body.string();
    }

    private static MetricParser getSqlParser(final String sql) {
        final MetricParserFactory factory = MetricParserFactoryProvider.getMetricParserFactory();
        if (MetricParserFactory.DEFAULT == factory) {
            return null;
        }

        return factory.getParser(sql);
    }

    private static MetricParser checkStatementType(final MetricParser sqlParser) {
        final MetricParser.StatementType statementType = sqlParser.statementType();
        switch (statementType) {
            case Create:
            case Select:
            case Alter:
            case Describe:
            case Show:
            case Drop:
            case Exists:
                break;
            case Insert: // We must use the SDK to write data, insert statements are not allowed
            case Unknown:
            default:
                throw new UnsupportedOperationException("Unsupported statement: " + statementType);
        }

        return sqlParser;
    }
}
