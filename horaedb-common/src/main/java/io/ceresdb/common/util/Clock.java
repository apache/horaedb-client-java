/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

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
