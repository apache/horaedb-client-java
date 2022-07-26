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
package com.ceresdb.models;

import java.util.function.Function;

import com.ceresdb.common.util.Requires;

/**
 * `Result` is a type that represents either success ([`Ok`]) or failure ([`Err`]).
 *
 * @author jiachun.fjc
 */
public final class Result<Ok, Err> {

    public static final int SUCCESS       = 200;
    public static final int INVALID_ROUTE = 302;
    public static final int SHOULD_RETRY  = 310;
    public static final int FLOW_CONTROL  = 503;

    private final Ok  ok;
    private final Err err;

    public static <Ok, Err> Result<Ok, Err> ok(final Ok ok) {
        Requires.requireNonNull(ok, "Null.ok");
        return new Result<>(ok, null);
    }

    public static <Ok, Err> Result<Ok, Err> err(final Err err) {
        Requires.requireNonNull(err, "Null.err");
        return new Result<>(null, err);
    }

    private Result(Ok ok, Err err) {
        this.ok = ok;
        this.err = err;
    }

    public boolean isOk() {
        return this.ok != null && this.err == null;
    }

    public Ok getOk() {
        return Requires.requireNonNull(this.ok, "Null.ok");
    }

    public Err getErr() {
        return Requires.requireNonNull(this.err, "Null.err");
    }

    /**
     * Maps a `Result<Ok, Err>` to `Result<U, Err>` by applying a function to
     * a contained [`Ok`] value, leaving an [`Err`] value untouched.
     *
     * This function can be used to compose the results of two functions.
     *
     * @param mapper a function to a contained [`Ok`] value
     * @param <U>    the [`Ok`] value type to map to
     * @return `Result<U, Err>`
     */
    public <U> Result<U, Err> map(final Function<Ok, U> mapper) {
        return isOk() ? Result.ok(mapper.apply(getOk())) : Result.err(getErr());
    }

    /**
     * Returns the provided default (if [`Err`]), or applies a function to
     * the contained value (if [`Ok`]).
     *
     * Arguments passed to `mapOr` are eagerly evaluated; if you are passing
     * the result of a function call, it is recommended to use
     * {@link #mapOrElse(Function, Function)}, which is lazily evaluated.
     *
     * @param defaultVal default value (if [`Err`])
     * @param mapper     a function to a contained [`Ok`] value
     * @param <U>        the value type to map to
     * @return the provided default (if [`Err`]), or applies a function to
     * the contained value (if [`Ok`])
     */
    public <U> U mapOr(final U defaultVal, final Function<Ok, U> mapper) {
        return isOk() ? mapper.apply(getOk()) : defaultVal;
    }

    /**
     * Maps a `Result<OK, Err>` to `U` by applying a fallback function to a
     * contained [`Err`] value, or a default function to a contained [`Ok`]
     * value.
     *
     * This function can be used to unpack a successful result while
     * handling an error.
     *
     * @param fallbackMapper a fallback function to a contained [`Err`] value
     * @param mapper         a function to a contained [`Ok`] value
     * @param <U>            the value type to map to
     * @return `U` by applying a fallback function to a contained [`Err`] value,
     * or a default function to a contained [`Ok`] value.
     */
    public <U> U mapOrElse(final Function<Err, U> fallbackMapper, final Function<Ok, U> mapper) {
        return isOk() ? mapper.apply(getOk()) : fallbackMapper.apply(getErr());
    }

    /**
     * Maps a `Result<Ok, Err>` to `Result<Ok, F>` by applying a function to a
     * contained [`Err`] value, leaving an [`Ok`] value untouched.
     *
     * @param mapper a function to a contained [`Err`] value
     * @param <F>    the err type to map to
     * @return `Result<Ok, F>`
     */
    public <F> Result<Ok, F> mapErr(final Function<Err, F> mapper) {
        return isOk() ? Result.ok(getOk()) : Result.err(mapper.apply(getErr()));
    }

    /**
     * Calls `mapper` if the result is [`Ok`], otherwise returns the [`Err`] value.
     *
     * @param mapper a function to a contained [`Ok`] value
     * @param <U> the value type witch mapped to
     * @return `Result<U, Err>`
     */
    public <U> Result<U, Err> andThen(final Function<Ok, Result<U, Err>> mapper) {
        return isOk() ? mapper.apply(getOk()) : Result.err(getErr());
    }

    /**
     * Calls `mapper` if the result is [`Err`], otherwise returns the [`Ok`] value.
     *
     * @param mapper a function to a contained [`Err`] value
     * @param <F>    the err type to map to
     * @return `Result<Ok, Err>`
     */
    public <F> Result<Ok, F> orElse(final Function<Err, Result<Ok, F>> mapper) {
        return isOk() ? Result.ok(getOk()) : mapper.apply(getErr());
    }

    /**
     * Returns the contained [`Ok`] value or a provided default.
     *
     * @param defaultVal a provided default value
     * @return the contained [`Ok`] value or a provided default
     */
    public Ok unwrapOr(final Ok defaultVal) {
        return isOk() ? getOk() : defaultVal;
    }

    /**
     * Returns the contained [`Ok`] value or computes it from a function.
     *
     * @param mapper computes function
     * @return the contained [`Ok`] value or computes it from a function
     */
    public Ok unwrapOrElse(final Function<Err, Ok> mapper) {
        return isOk() ? getOk() : mapper.apply(getErr());
    }

    @Override
    public String toString() {
        return "Result{" + "ok=" + ok + ", err=" + err + '}';
    }
}
