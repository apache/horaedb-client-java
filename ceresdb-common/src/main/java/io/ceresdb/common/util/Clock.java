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
package io.ceresdb.common.util;

/**
 *
 * @author jiachun.fjc
 */
public abstract class Clock {

    /**
     * Returns the current time tick.
     *
     * @return time tick in millis.
     */
    public abstract long getTick();

    public long duration(final long startTick) {
        return getTick() - startTick;
    }

    /**
     * The default clock to use.
     *
     * @return the default {@link Clock} instance
     * @see UserTimeClock
     */
    public static Clock defaultClock() {
        return UserTimeClockHolder.DEFAULT;
    }

    /**
     * A clock implementation which returns the current millis.
     */
    public static class UserTimeClock extends Clock {

        @Override
        public long getTick() {
            return System.currentTimeMillis();
        }
    }

    private static class UserTimeClockHolder {
        private static final Clock DEFAULT = new UserTimeClock();
    }
}
