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

public final class Messages {
    private static final Ansi ACCENT = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.BLUE, null);
    private static final Ansi OK = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.GREEN, null);
    private static final Ansi ERROR = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.RED, null);

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
}
