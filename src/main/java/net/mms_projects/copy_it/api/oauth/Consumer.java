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

package net.mms_projects.copy_it.api.oauth;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Consumer {
    public static final String PUBLIC_KEY = "public_key";
    public static final String SECRET_KEY = "secret_key";
    public static final String FLAGS = "flags";

    public Consumer(ResultSet cursor) throws SQLException {
        public_key = cursor.getString(PUBLIC_KEY);
        secret_key = cursor.getString(SECRET_KEY);
        flags = cursor.getInt(FLAGS);
    }

    public final String getPublicKey() { return public_key; }
    public final String getSecretKey() { return secret_key; }
    public final int getFlags() { return flags; }

    private final String public_key;
    private final String secret_key;
    private final int flags;
}
