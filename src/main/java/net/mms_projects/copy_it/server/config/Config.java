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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public final class Config {
    public final static class Keys {
        public static final String DBCONNECT = "dbconnect";
        public static final String HTTPAPI_PORT = "httpapi_port";
        public static final String MAXCONN_DATABASEPOOL = "maxconnections_databasepool";
        public static final String HTTP_FILES = "http_files";
        public static final String GCM_TOKEN = "gcm_token";
        public static final String PID_FILE = "pid_file";
        public static final String AUDIENCE = "audience";
        public static final String GOOGLE_CLIENT_ID = "google_client_id";
        public static final String GOOGLE_SECRET_KEY = "google_secret_key";
        public static final String MAX_CACHED_FILES = "max_cached_files";
        public static final String MAX_CACHED_CONSUMERS = "max_cached_consumers";
    }

    public Config(final File file) throws ConfigAlreadyLoadedException, IOException, MissingRequiredKey, NotADirectoryException, URISyntaxException {
        if (properties != null)
            throw new ConfigAlreadyLoadedException();
        properties = new Properties();
        properties.load(new FileReader(file));
        final String[] required = { Keys.DBCONNECT, Keys.HTTPAPI_PORT };
        for (int i = 0; i < required.length; i++) {
            if (!properties.containsKey(required[i]))
                throw new MissingRequiredKey(required[i]);
        }
        final String[] ints = { Keys.HTTPAPI_PORT, Keys.MAX_CACHED_FILES, Keys.MAX_CACHED_CONSUMERS };
        for (int i = 0; i < ints.length; i++) {
            if (properties.containsKey(ints[i]))
                Integer.valueOf(properties.getProperty(ints[i]));
        }
        final String[] dirs = { Keys.HTTP_FILES };
        for (int i = 0; i < dirs.length; i++) {
            if (properties.containsKey(dirs[i])) {
                File dir = new File(properties.getProperty(dirs[i]));
                if (!dir.exists() || !dir.isDirectory())
                    throw new NotADirectoryException(dir);
            }
        }
        final String[] uris = { Keys.AUDIENCE };
        for (int i = 0; i < uris.length; i++) {
            if (properties.containsKey(uris[i])) {
                try {
                    new URI(properties.getProperty(uris[i]));
                } catch (URISyntaxException e) {
                    throw e;
                }
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

    public static String getStringSafe(final String key, final String def) {
        if (properties.containsKey(key))
            return properties.getProperty(key);
        return def;
    }

    public static int getInt(final String key, final int def) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean hasString(final String key) {
        return properties.containsKey(key);
    }

    private static Properties properties;
}
