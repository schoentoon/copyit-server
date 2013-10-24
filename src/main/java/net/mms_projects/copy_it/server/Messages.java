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

import jlibs.core.lang.Ansi;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BUNDLE_NAME = "messages";

    private static final Ansi ACCENT = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.BLUE, null);
    private static final Ansi OK = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.GREEN, null);
    private static final Ansi WARNING = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.YELLOW, null);
    private static final Ansi ERROR = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.RED, null);

    private static ResourceBundle RESOURCE_BUNDLE;
    private static ResourceBundle FALLBACK_RESOURCE_BUNDLE;

    private Messages() {
    }

    public static void printOK(final String msg) {
        ACCENT.print(System.out, "[ ");
        OK.print(System.out, "OK");
        ACCENT.print(System.out, " ] ");
        System.out.println(msg);
    }

    public static void printError(final String msg) {
        ACCENT.print(System.out, "[ ");
        ERROR.print(System.out, "!!");
        ACCENT.print(System.out, " ] ");
        System.out.println(msg);
    }

    public static void printWarning(final String msg) {
        ACCENT.print(System.out, "[ ");
        WARNING.print(System.out, "**");
        ACCENT.print(System.out, " ] ");
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
