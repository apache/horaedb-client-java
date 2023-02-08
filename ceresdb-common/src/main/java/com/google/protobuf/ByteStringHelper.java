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
// Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
package com.google.protobuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A {@code ByteString} helper.
 *
 */
public class ByteStringHelper {

    /**
     * Wrap a byte array into a ByteString.
     */
    public static ByteString wrap(final byte[] bs) {
        return ByteString.wrap(bs);
    }

    /**
     * Wrap a byte array into a ByteString.
     * @param bs     the byte array
     * @param offset read start offset in array
     * @param len    read data length
     * @return the result byte string.
     */
    public static ByteString wrap(final byte[] bs, final int offset, final int len) {
        return ByteString.wrap(bs, offset, len);
    }

    /**
     * Wrap a byte buffer into a ByteString.
     */
    public static ByteString wrap(final ByteBuffer buf) {
        return ByteString.wrap(buf);
    }

    /**
     * Steal the byte[] from {@link ByteString}, if failed,
     * then call {@link ByteString#toByteArray()}.
     *
     * @param byteString the byteString source data
     * @return carried bytes
     */
    public static byte[] sealByteArray(final ByteString byteString) {
        final BytesStealer stealer = new BytesStealer();
        try {
            byteString.writeTo(stealer);
            if (stealer.isValid()) {
                return stealer.value();
            }
        } catch (final IOException ignored) {
            // ignored
        }
        return byteString.toByteArray();
    }

    /**
     * Concatenate the given strings while performing various optimizations to
     * slow the growth rate of tree depth and tree node count. The result is
     * either a {@link ByteString.LeafByteString} or a
     * {@link RopeByteString} depending on which optimizations, if any, were
     * applied.
     *
     * <p>Small pieces of length less than {@link
     * ByteString#CONCATENATE_BY_COPY_SIZE} may be copied by value here, as in
     * BAP95.  Large pieces are referenced without copy.
     *
     * <p>Most of the operation here is inspired by the now-famous paper <a
     *  * href="https://web.archive.org/web/20060202015456/http://www.cs.ubc.ca/local/reading/proceedings/spe91-95/spe/vol25/issue12/spe986.pdf">
     *
     * @param left  string on the left
     * @param right string on the right
     * @return concatenation representing the same sequence as the given strings
     */
    public static ByteString concatenate(final ByteString left, final ByteString right) {
        return RopeByteString.concatenate(left, right);
    }

    /**
     * @see #concatenate(ByteString, ByteString)
     */
    public static ByteString concatenate(final List<ByteBuffer> byteBuffers) {
        final int size = byteBuffers.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return wrap(byteBuffers.get(0));
        }

        ByteString left = null;
        for (final ByteBuffer buf : byteBuffers) {
            if (buf.remaining() > 0) {
                if (left == null) {
                    left = wrap(buf);
                } else {
                    left = concatenate(left, wrap(buf));
                }
            }
        }
        return left;
    }
}
