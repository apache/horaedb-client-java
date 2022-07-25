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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.SystemPropertyUtil;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Http utils.
 *
 * @author jiachun.fjc
 */
public class HttpUtil {

    public static final String PROTOCOL = "http";

    private static final long READ_TIMEOUT_MS  = SystemPropertyUtil.getLong(OptKeys.HTTP_READ_TIMEOUT_MS, 10000);
    private static final long WRITE_TIMEOUT_MS = SystemPropertyUtil.getLong(OptKeys.HTTP_WRITE_TIMEOUT_MS, 10000);

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // The singleton HTTP client.
    //
    // Shutdown isn’t necessary, the threads and connections that
    // are held will be released automatically if they remain idle.
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder() //
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS) //
            .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS) //
            .build();

    public static OkHttpClient httpClient() {
        return OK_HTTP_CLIENT;
    }

    public static Map<String, String> params(final String key, final String val) {
        return params(key, val, HashMap::new);
    }

    public static Map<String, String> params(final String key, final String val,
                                             final Supplier<Map<String, String>> ctx) {
        final Map<String, String> map = ctx.get();
        map.put(key, val);
        return map;
    }

    public static RequestBody requestBody(final Map<String, String> params) {
        return RequestBody.create(new Gson().toJson(params), JSON_MEDIA_TYPE);
    }
}
