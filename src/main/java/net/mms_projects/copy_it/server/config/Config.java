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

package net.mms_projects.copy_it.server.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public final class Config {
    public final static class Keys {
        public static final String DBCONNECT = "dbconnect";
        public static final String HTTPAPI_PORT = "httpapi_port";
        public static final String MAXCONN_DATABASEPOOL = "maxconnections_databasepool";
        public static final String HTTP_FILES = "http_files";
        private static final String[] required = { DBCONNECT, HTTPAPI_PORT };
        private static final String[] ints = { HTTPAPI_PORT };
        private static final String[] dirs = { HTTP_FILES };
    }

    public Config(final File file) throws ConfigAlreadyLoadedException, IOException, MissingRequiredKey, NotADirectoryException {
        if (properties != null)
            throw new ConfigAlreadyLoadedException();
        properties = new Properties();
        properties.load(new FileReader(file));
        for (int i = 0; i < Keys.required.length; i++) {
            if (!properties.containsKey(Keys.required[i]))
                throw new MissingRequiredKey(Keys.required[i]);
        }
        for (int i = 0; i < Keys.ints.length; i++) {
            if (properties.containsKey(Keys.ints[i]))
                Integer.valueOf(properties.getProperty(Keys.ints[i]));
        }
        for (int i = 0; i < Keys.dirs.length; i++) {
            if (properties.containsKey(Keys.dirs[i])) {
                File dir = new File(properties.getProperty(Keys.dirs[i]));
                if (!dir.exists() || !dir.isDirectory())
                    throw new NotADirectoryException(dir);
            }
        }
    }

    public static int getHTTPAPIPort() throws NoConfigException {
        if (properties != null)
            return Integer.valueOf(properties.getProperty(Keys.HTTPAPI_PORT));
        throw new NoConfigException();
    }

    public static int getMaxConnectionsDatabasePool() {
        try {
            return Integer.valueOf(properties.getProperty(Keys.MAXCONN_DATABASEPOOL));
        } catch (Exception e) {
            return 10;
        }
    }

    public static String getDBConnect() throws NoConfigException {
        if (properties != null)
            return properties.getProperty(Keys.DBCONNECT);
        throw new NoConfigException();
    }

    public static String getString(final String key) throws MissingKey {
        if (properties.containsKey(key))
            return properties.getProperty(key);
        throw new MissingKey(key);
    }

    private static Properties properties;
}
