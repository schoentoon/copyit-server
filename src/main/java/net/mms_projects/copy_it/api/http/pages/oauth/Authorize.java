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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.LanguageHeader;
import net.mms_projects.copy_it.server.TemplateMatcher;
import net.mms_projects.copy_it.server.database.Database;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.MOVED_PERMANENTLY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Authorize extends Page {
    private static final String OAUTH_TOKEN = "oauth_token";

    private static final String MISSING_OAUTH_TOKEN = "We're missing an OAuth token here..";

    private static final TemplateMatcher TEMPLATE_MATCHER = new TemplateMatcher("${", "}");

    private static final String SELECT_APPLICATION = "SELECT a.name application " +
                                                     "FROM request_tokens r, applications a " +
                                                     "WHERE r.public_key = ? " +
                                                     "AND r.application_id = a._id";

    private static final String APPLICATION = "application";
    private static final String AUTHORIZE_DOT_HTML = "authorize.html";

    public FullHttpResponse onGetRequest(HttpRequest request, Database database) throws Exception {
        final QueryStringDecoder querydecoder = new QueryStringDecoder(new URI(request.getUri()));
        Map<String, List<String>> parameters = querydecoder.parameters();
        if (!parameters.containsKey(OAUTH_TOKEN))
            throw new ErrorException(MISSING_OAUTH_TOKEN);
        PreparedStatement get_app = database.getConnection().prepareStatement(SELECT_APPLICATION);
        get_app.setString(1, parameters.get(OAUTH_TOKEN).get(0));
        ResultSet result = get_app.executeQuery();
        if (result.first()) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put(APPLICATION, result.getString(APPLICATION));
            result.close();
            return new DefaultFullHttpResponse(request.getProtocolVersion(), OK
                    ,Unpooled.copiedBuffer(TEMPLATE_MATCHER.replace(LanguageHeader.fromRequest(request)
                                                                                  .getPageInLocale(AUTHORIZE_DOT_HTML)
                                                                   ,vars)
                                          ,CharsetUtil.UTF_8));
        }
        result.close();
        throw new ErrorException(INVALID_TOKEN);
    }

    private static final String CALLBACK_URI = "callback_uri";
    private static final String VERIFIER = "verifier";
    private static final String CALLBACK_URI_PARAMETER = "?oauth_verifier=";

    private static final String INVALID_TOKEN = "Invalid token..";
    private static final String USER_ID_NOT_A_NUMBER = "The specified user_id is not a number";
    private static final String WE_MADE_A_BOO_BOO = "We made a boo boo.";

    private static final String SELECT_CALLBACK = "SELECT callback_uri, verifier " +
                                                  "FROM request_tokens " +
                                                  "WHERE public_key = ? " +
                                                  "AND (NOW() - INTERVAL 5 MINUTE) < timestamp";

    private static final String UPDATE_USER_ID = "UPDATE request_tokens " +
                                                 "SET user_id = (SELECT user_id " +
                                                                "FROM users WHERE user_email = ? " +
                                                                "LIMIT 1) " +
                                                 "WHERE public_key = ? ";
    private static final String GET_EMAIL_FROM_SESSION = "SELECT email FROM sessions WHERE session_id = ?";
    private static final String DELETE_SESSION = "DELETE FROM sessions WHERE session_id = ?";

    private static final String CREATE_USER = "INSERT IGNORE INTO users (user_email) VALUES (?)";
    private static final String SESSION = "session";
    private static final String MISSING_SESSION = "We're missing a session here..";
    private static final String EMAIL = "email";

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        final QueryStringDecoder querydecoder = new QueryStringDecoder(new URI(request.getUri()));
        Map<String, List<String>> parameters = querydecoder.parameters();
        if (!parameters.containsKey(OAUTH_TOKEN))
            throw new ErrorException(MISSING_OAUTH_TOKEN);
        final String oauth_token = parameters.get(OAUTH_TOKEN).get(0);
        final InterfaceHttpData session_raw = postRequestDecoder.getBodyHttpData(SESSION);
        if (session_raw == null || !(session_raw instanceof HttpData))
            throw new ErrorException(MISSING_SESSION);
        final String session = ((HttpData) session_raw).getString();
        try {
            PreparedStatement get_email = database.getConnection().prepareStatement(GET_EMAIL_FROM_SESSION);
            get_email.setString(1, session);
            ResultSet email = get_email.executeQuery();
            if (email.first()) {
                final String email_address = email.getString(EMAIL);
                email.close();
                PreparedStatement cleanup_session = database.getConnection().prepareStatement(DELETE_SESSION);
                cleanup_session.setString(1, session);
                cleanup_session.executeUpdate();
                PreparedStatement create_user = database.getConnection().prepareStatement(CREATE_USER);
                create_user.setString(1, email_address);
                create_user.executeUpdate();
                PreparedStatement update = database.getConnection().prepareStatement(UPDATE_USER_ID);
                update.setString(1, email_address);
                update.setString(2, oauth_token);
                if (update.executeUpdate() == 1) {
                    database.getConnection().commit();
                    PreparedStatement statement = database.getConnection().prepareStatement(SELECT_CALLBACK);
                    statement.setString(1, oauth_token);
                    ResultSet result = statement.executeQuery();
                    if (result.first()) {
                        final String callback = result.getString(CALLBACK_URI);
                        final String verifier = result.getString(VERIFIER);
                        result.close();
                        final StringBuilder output = new StringBuilder();
                        output.append(callback);
                        output.append(CALLBACK_URI_PARAMETER);
                        output.append(verifier);
                        DefaultFullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), MOVED_PERMANENTLY);
                        HttpHeaders.addHeader(response, HttpHeaders.Names.LOCATION, output.toString());
                        return response;
                    } else {
                        result.close();
                        throw new ErrorException(INVALID_TOKEN);
                    }
                }
            }
            email.close();
            throw new ErrorException(WE_MADE_A_BOO_BOO);
        } catch (NumberFormatException e) {
            throw new ErrorException(USER_ID_NOT_A_NUMBER);
        }
    }

    public String GetContentType() {
        return ContentTypes.PLAIN_HTML;
    }
}
