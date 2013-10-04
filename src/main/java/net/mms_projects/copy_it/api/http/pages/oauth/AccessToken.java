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

package net.mms_projects.copy_it.api.http.pages.oauth;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.UnsupportedMethodException;
import net.mms_projects.copy_it.api.oauth.HeaderVerifier;
import net.mms_projects.copy_it.server.database.Database;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class AccessToken extends Page {
    private static final String USER_ID = "user_id";
    private static final String SELECT_USER_ID = "SELECT r.user_id user_id, r.secret_key secret_key, c.scopes scopes " +
                                                 "FROM request_tokens r, consumers c " +
                                                 "WHERE r.public_key = ? " +
                                                 "AND r.verifier = ? " +
                                                 "AND r.user_id IS NOT NULL " +
                                                 "AND c.application_id = ? " +
                                                 "AND (NOW() - INTERVAL 5 MINUTE) < r.timestamp";
    private static final String CREATE_TOKEN = "INSERT IGNORE INTO user_tokens (user_id, application_id, scopes) VALUES (?, ?, ?)";
    private static final String PUBLIC_KEY = "public_key";
    private static final String SECRET_KEY = "secret_key";
    private static final String SELECT_ACCESS_TOKEN = "SELECT public_key, secret_key " +
                                                      "FROM user_tokens " +
                                                      "WHERE user_id = ? " +
                                                      "AND application_id = ?";

    private static final String OUTPUT_TOKEN = "oauth_token=";
    private static final String OUTPUT_TOKEN_SECRET = "&oauth_token_secret=";

    private static final String CLEANUP = "DELETE FROM request_tokens " +
                                          "WHERE public_key = ?";

    public FullHttpResponse onGetRequest(HttpRequest request, Database database) throws Exception {
        URI uri = new URI(request.getUri());
        HeaderVerifier headerVerifier = new HeaderVerifier(request, uri, HeaderVerifier.Flags.REQUIRES_VERIFIER);
        headerVerifier.verifyConsumer(database);
        final String verifier = headerVerifier.getVerifier();
        final String token = headerVerifier.getPublicToken();
        PreparedStatement statement = database.getConnection().prepareStatement(SELECT_USER_ID);
        statement.setString(1, token);
        statement.setString(2, verifier);
        statement.setInt(3, headerVerifier.getConsumerId());
        ResultSet result = statement.executeQuery();
        if (result.first()) {
            int user_id = result.getInt(USER_ID);
            headerVerifier.setFakeUser(new HeaderVerifier.FakeUser(result));
            headerVerifier.checkSignature(null, false);
            result.close();
            PreparedStatement create_token = database.getConnection().prepareStatement(CREATE_TOKEN);
            create_token.setInt(1, user_id);
            create_token.setInt(2, headerVerifier.getConsumerId());
            create_token.setInt(3, headerVerifier.getConsumerScope().toInt());
            create_token.executeUpdate();
            PreparedStatement select_token = database.getConnection().prepareStatement(SELECT_ACCESS_TOKEN);
            select_token.setInt(1, user_id);
            select_token.setInt(2, headerVerifier.getConsumerId());
            ResultSet tokens = select_token.executeQuery();
            if (tokens.first()) {
                final StringBuilder output = new StringBuilder();
                output.append(OUTPUT_TOKEN);
                output.append(tokens.getString(PUBLIC_KEY));
                output.append(OUTPUT_TOKEN_SECRET);
                output.append(tokens.getString(SECRET_KEY));
                tokens.close();
                PreparedStatement cleanup = database.getConnection().prepareStatement(CLEANUP);
                cleanup.setString(1, token);
                cleanup.executeUpdate();
                return new DefaultFullHttpResponse(request.getProtocolVersion()
                        ,OK, Unpooled.copiedBuffer(output.toString(), CharsetUtil.UTF_8));
            }
        }
        result.close();
        return null;
    }

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        throw new UnsupportedMethodException(request.getMethod());
    }

    public String GetContentType() {
        return ContentTypes.PLAIN_TEXT;
    }
}
