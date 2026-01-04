/*
 * CraftBook Copyright (C) EngineHub and Contributors <https://enginehub.org/>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package org.enginehub.craftbook.util;

import java.util.regex.Pattern;

/**
 * Utility class for string operations.
 */
public final class StringUtil {

    private StringUtil() {
    }

    private static final Pattern STRIP_RESET_PATTERN = Pattern.compile("(?i)" + '\u00A7' + "[Rr]");

    /**
     * Strips reset color codes from a message.
     *
     * @param message The message to strip reset codes from
     * @return The message with reset codes removed, or null if the input was null
     */
    public static String stripResetChar(String message) {

        if (message == null)
            return null;

        return STRIP_RESET_PATTERN.matcher(message).replaceAll("");
    }
}

