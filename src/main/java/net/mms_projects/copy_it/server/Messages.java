/*  copyit-server
 *  Copyright (C) 2013  Toon Schoenmakers
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mms_projects.copy_it.server;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {
    private static final String BUNDLE_NAME = "messages";

    private static ResourceBundle RESOURCE_BUNDLE;
    private static ResourceBundle FALLBACK_RESOURCE_BUNDLE;

    private Messages() {
    }

    private static final String ACCENT = "\033[1;34m";
    private static final String OK = "\033[1;32m";
    private static final String WARNING = "\033[1;33m";
    private static final String ERROR = "\033[1;31m";
    private static final String RESET = "\033[m";

    private static void ok() {
        final boolean colors = ansiColorsSupported();
        if (colors)
            System.out.print(ACCENT);
        System.out.print("[ ");
        if (colors)
            System.out.print(OK);
        System.out.print("OK");
        if (colors)
            System.out.print(ACCENT);
        System.out.print(" ] ");
        System.out.print(RESET);
    }

    private static void warning() {
        final boolean colors = ansiColorsSupported();
        if (colors)
            System.out.print(ACCENT);
        System.out.print("[ ");
        if (colors)
            System.out.print(WARNING);
        System.out.print("**");
        if (colors)
            System.out.print(ACCENT);
        System.out.print(" ] ");
        System.out.print(RESET);
    }

    private static void error() {
        final boolean colors = ansiColorsSupported();
        if (colors)
            System.out.print(ACCENT);
        System.out.print("[ ");
        if (colors)
            System.out.print(ERROR);
        System.out.print("!!");
        if (colors)
            System.out.print(ACCENT);
        System.out.print(" ] ");
        System.out.print(RESET);
    }

    private static boolean ansiColorsSupported() {
        if (System.getProperty("os.name").contains("windows"))
            return false;
        return System.console() != null;
    }

    public static void printOK(final String msg) {
        ok();
        System.out.println(msg);
    }

    public static void printError(final String msg) {
        error();
        System.out.println(msg);
    }

    public static void printWarning(final String msg) {
        warning();
        System.out.println(msg);
    }

    public static String getString(String key, Locale locale) {
        try {
            return getBundle(locale).getString(key);
        } catch (MissingResourceException e1) {
            try {
                return getFallbackBundle().getString(key);
            } catch (MissingResourceException e2) {
            }
        }
        return '!' + key + '!';
    }

    public static String getString(String key, Locale locale, Object... formatArgs) {
        String raw = getString(key, locale);
        return String.format(raw, formatArgs);
    }

    private static ResourceBundle getBundle(Locale locale) {
        if (RESOURCE_BUNDLE == null) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        }
        return RESOURCE_BUNDLE;
    }

    private static ResourceBundle getFallbackBundle() {
        if (FALLBACK_RESOURCE_BUNDLE == null) {
            FALLBACK_RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
        }
        return FALLBACK_RESOURCE_BUNDLE;
    }
}
