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
package io.ceresdb.common.signal;

import java.util.Optional;

/**
 * @author jiachun.fjc
 */
public enum FileSignal {
    ClearCache("clear_cache.sig", "How to clear route cache"), //
    RwLogging("rw_logging.sig", "How to open or close read/write log(The second execution means close)"), //
    RpcLimit("rpc_limit.sig", "How to open or close rpc limiter(The second execution means close)"), //
    ;

    private final String filename;
    private final String comment;

    FileSignal(String filename, String comment) {
        this.filename = filename;
        this.comment = comment;
    }

    public String getFilename() {
        return filename;
    }

    public String getComment() {
        return comment;
    }

    public static Optional<FileSignal> parse(final String name) {
        for (final FileSignal sig : values()) {
            if (sig.filename.equals(name)) {
                return Optional.of(sig);
            }
        }
        return Optional.empty();
    }
}
