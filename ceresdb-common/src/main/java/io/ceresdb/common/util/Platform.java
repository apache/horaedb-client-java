/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.common.util;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ceresdb.common.OptKeys;

public class Platform {

    private static final Logger LOG = LoggerFactory.getLogger(Platform.class);

    private static final String WIN_KEY = "win";
    private static final String MAC_KEY = "mac os x";

    private static final boolean IS_WINDOWS = isWindows0();
    private static final boolean IS_MAC     = isMac0();

    /**
     * Return {@code true} if the JVM is running on Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * Return {@code true} if the JVM is running on Mac OSX
     */
    public static boolean isMac() {
        return IS_MAC;
    }

    private static boolean isMac0() {
        final boolean mac = checkOS(MAC_KEY);
        if (mac) {
            LOG.debug("Platform: Mac OS X");
        }
        return mac;
    }

    private static boolean isWindows0() {
        final boolean windows = checkOS(WIN_KEY);
        if (windows) {
            LOG.debug("Platform: Windows");
        }
        return windows;
    }

    private static boolean checkOS(final String osKey) {
        return SystemPropertyUtil.get(OptKeys.OS_NAME, "") //
                .toLowerCase(Locale.US) //
                .contains(osKey);
    }
}
