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

import net.mms_projects.copy_it.server.config.Config;
import org.junit.Test;

import java.io.File;

public class DatabasePoolTest {
    @Test(expected=OutOfConnectionsException.class,timeout=30000)
    public void runTest() throws Exception {
        new Config(new File(System.getProperty("testConfigFile")));
        new DatabasePool(MySQL.class, Config.getMaxConnectionsDatabasePool());
        final int maxconnections = Config.getMaxConnectionsDatabasePool(); /* We want to claim more than we have */
        for (int i = 0; i < maxconnections; i++)
            DatabasePool.getConnection();
        DatabasePool.getConnection();
    }
}
