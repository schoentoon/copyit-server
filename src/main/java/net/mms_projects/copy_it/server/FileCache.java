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

import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.config.MissingKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class FileCache {
    private static final FileCache FILE_CACHE = new FileCache();
    private static final int MAX_ITEMS = 100; //TODO Make this a config option

    private FileCache() {
        cache = new LinkedHashMap<String, String>() {
            public boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_ITEMS;
            }
        };
        not_files = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_ITEMS;
            }
        });
    }

    public static String get(final String filename) throws IOException, MissingKey {
        return FILE_CACHE.getFile(filename);
    }

    private String getFile(final String filename) throws IOException, MissingKey {
        String output = cache.get(filename);
        if (output != null)
            return output;
        if (not_files.contains(filename))
            throw new FileNotFoundException(filename);
        try {
            File file = new File(Config.getString(Config.Keys.HTTP_FILES) + File.separator + filename);
            FileInputStream inputStream = new FileInputStream(file);
            final StringBuilder builder = new StringBuilder(inputStream.available());
            for(int c = inputStream.read(); c != -1; c = inputStream.read())
                builder.append((char) c);
            output = builder.toString();
            cache.put(filename, output);
            return output;
        } catch (FileNotFoundException e) {
            not_files.add(filename);
            throw e;
        }
    }

    public static void clear() {
        FILE_CACHE.cache.clear();
        FILE_CACHE.not_files.clear();
    }

    private final LinkedHashMap<String, String> cache;
    private final Set<String> not_files;
}
