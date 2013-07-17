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

import net.mms_projects.copy_it.api.oauth.exceptions.InvalidConsumerException;
import net.mms_projects.copy_it.server.database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class KeyStore {
    private static final KeyStore keyStore = new KeyStore();
    public KeyStore() {
        consumers = new HashMap<String, Consumer>();
    }

    public static final KeyStore getKeyStore() {
        return keyStore;
    }

    private static final String SELECT_QUERY = "SELECT public_key, secret_key, flags " +
                                               "FROM consumers " +
                                               "WHERE public_key = ? " +
                                               "LIMIT 1";

    public Consumer getConsumer(final String public_key, final Database database) throws SQLException, InvalidConsumerException {
        Consumer output = consumers.get(public_key);
        if (output != null)
            return output;
        PreparedStatement statement = database.getConnection().prepareStatement(SELECT_QUERY);
        statement.setString(1, public_key);
        ResultSet result = statement.executeQuery();
        if (result.first()) {
            output = new Consumer(result);
            consumers.put(output.getPublicKey(), output);
        }
        result.close();
        if (output == null)
            throw new InvalidConsumerException();
        return output;
    }

    private final HashMap<String, Consumer> consumers;
}
