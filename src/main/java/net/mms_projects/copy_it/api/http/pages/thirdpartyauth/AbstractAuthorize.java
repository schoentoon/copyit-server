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

package net.mms_projects.copy_it.api.http.pages.thirdpartyauth;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.UnsupportedMethodException;
import net.mms_projects.copy_it.server.database.Database;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractAuthorize extends Page {
    public FullHttpResponse onGetRequest(HttpRequest request, Database database) throws Exception {
        throw new UnsupportedMethodException(request.getMethod());
    }

    public static final String SESSION = "session";
    public abstract FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception;

    private static final String INSERT_SESSION = "INSERT INTO sessions (email, session_id) VALUES (?, ?)";

    private static final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    protected String generateCookie(final String email, Database database) throws SQLException {
        do {
            SecureRandom random = new SecureRandom();
            random.setSeed(email.hashCode() ^ System.currentTimeMillis());
            char[] rand = new char[1024];
            for (int i = 0; i < 1024; i++)
                rand[i] = characters.charAt(random.nextInt(characters.length()));
            PreparedStatement statement = database.getConnection().prepareStatement(INSERT_SESSION);
            statement.setString(1, email);
            final String cookie = new String(rand);
            statement.setString(2, cookie);
            if (statement.executeUpdate() == 1) {
                database.getConnection().commit();
                return cookie;
            }
        } while (true);
    }
}
