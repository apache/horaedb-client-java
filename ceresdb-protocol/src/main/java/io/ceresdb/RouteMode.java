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

/**
 *  define route mode
 * @author lee
 * @version : RouteMode.java, v 0.1 2023��01��17�� 14:23 lee Exp $
 */
public enum RouteMode {
    /**
     * In this mode, client request to a server  directly, and the server proxy the request to the correct server.
     */
    STANDALONE,

    /**
     * In this mode, the client will find the correct server first, and then request to the server.
     */
    CLUSTER
}