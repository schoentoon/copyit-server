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

import java.sql.SQLException;

/**
 * Easy access to the scope of consumers
 * @see net.mms_projects.copy_it.api.oauth.Consumer
 */
public enum Scope {
    READ, WRITE_AND_READ;
    public static Scope fromDatabase(int i) throws SQLException {
        switch (i) {
        case 1:
            return READ;
        case 2:
            return WRITE_AND_READ;
        default:
            throw new SQLException(i + " is an invalid scope");
        }
    }
    public boolean canRead() {
        return true;
    }
    public boolean canWrite() {
        return (this == WRITE_AND_READ);
    }
    public int toInt() {
        switch (this) {
        case READ:
            return 1;
        case WRITE_AND_READ:
            return 2;
        default:
            return -1;
        }
    }
}
