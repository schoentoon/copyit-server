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

package net.mms_projects.copy_it.server.database;

import net.mms_projects.copy_it.server.Messages;
import net.mms_projects.copy_it.server.config.Config;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class DatabasePool {
    public static DatabasePool singleton = null;
    public DatabasePool(Class<? extends Database> driver, int maxConnections) throws Exception {
        if (singleton != null)
            throw new Exception("A databasepool was already created.");
        this.maxConnections = maxConnections;
        connections = new ArrayList<Database>(maxConnections);
        claimed_connections = new ArrayList<Database>(maxConnections);
        final String dbconnect = Config.getDBConnect();
        final Constructor<?> constructor = driver.getConstructors()[0];
        for (int i = 0; i < maxConnections; i++)
            connections.add((Database) constructor.newInstance(dbconnect));
        singleton = this;
        Messages.printOK("Created a " + driver.getSimpleName() + " database pool with " + maxConnections + " connections");
    }

    public static final Database getDBConnection() throws OutOfConnectionsException {
        for (int i = 0; i < singleton.maxConnections; i++) {
            final Database connection = singleton.connections.get(i);
            if (!singleton.claimed_connections.contains(connection)) {
                singleton.claimed_connections.add(connection);
                return connection;
            }
        }
        throw new OutOfConnectionsException();
    }

    public static void freeDBConnection(final Database connection) {
        singleton.claimed_connections.remove(connection);
    }

    public static final Connection getConnection() throws OutOfConnectionsException {
        return getDBConnection().getConnection();
    }

    public static void freeConnection(final Connection connection) {
        for (final Database db : singleton.claimed_connections) {
            if (db.getConnection() == connection) {
                singleton.claimed_connections.remove(db);
                return;
            }
        }
    }

    private final List<Database> connections;
    private final List<Database> claimed_connections;
    private final int maxConnections;
}
