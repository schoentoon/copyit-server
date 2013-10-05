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
import jlibs.core.util.regex.TemplateMatcher;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.LanguageHeader;
import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.database.Database;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.MOVED_PERMANENTLY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Authorize extends Page {
    private static final String ASSERTION = "assertion";
    private static final String OAUTH_TOKEN = "oauth_token";

    private static final String MISSING_OAUTH_TOKEN = "We're missing an OAuth token here..";
    private static final String MISSING_ASSERTION = "We're missing our assertion here..";

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
    private static final String PERSONA_AUTH_FAILED = "Persona auth failed.";

    private static final String SELECT_CALLBACK = "SELECT callback_uri, verifier " +
                                                  "FROM request_tokens " +
                                                  "WHERE public_key = ? " +
                                                  "AND (NOW() - INTERVAL 5 MINUTE) < timestamp";

    private static final String UPDATE_USER_ID = "UPDATE request_tokens " +
                                                 "SET user_id = (SELECT user_id " +
                                                                "FROM users WHERE user_email = ? " +
                                                                "LIMIT 1) " +
                                                 "WHERE public_key = ? ";

    private static final String CREATE_USER = "INSERT IGNORE INTO users (user_email) VALUES (?)";

    private static final String STATUS = "status";
    private static final String OKAY = "okay";
    private static final String EMAIL = "email";

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        final InterfaceHttpData oauth_token_raw = postRequestDecoder.getBodyHttpData(OAUTH_TOKEN);
        if (oauth_token_raw == null || !(oauth_token_raw instanceof HttpData))
            throw new ErrorException(MISSING_OAUTH_TOKEN);
        final String oauth_token = ((HttpData) oauth_token_raw).getString();
        final InterfaceHttpData assertion_raw = postRequestDecoder.getBodyHttpData(ASSERTION);
        if (assertion_raw == null || !(assertion_raw instanceof HttpData))
            throw new ErrorException(MISSING_ASSERTION);
        final String assertion = ((HttpData) assertion_raw).getString();
        try {
            JSONObject verified = verifyAssertion(assertion);
            if (verified.has(EMAIL) && verified.has(STATUS) && OKAY.equals(verified.getString(STATUS))) {
                PreparedStatement create_user = database.getConnection().prepareStatement(CREATE_USER);
                create_user.setString(1, verified.getString(EMAIL));
                create_user.executeUpdate();
            } else
                throw new ErrorException(PERSONA_AUTH_FAILED);
            PreparedStatement update = database.getConnection().prepareStatement(UPDATE_USER_ID);
            update.setString(1, verified.getString(EMAIL));
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
            throw new ErrorException(WE_MADE_A_BOO_BOO);
        } catch (NumberFormatException e) {
            throw new ErrorException(USER_ID_NOT_A_NUMBER);
        }
    }

    private static final String VERIFY_ASSERTION = "https://verifier.login.persona.org/verify";
    private static final String POST = "POST";
    private static final String ASSERTION_IS = "assertion=";
    private static final String AUDIENCE = "&audience=" + Config.getStringSafe(Config.Keys.AUDIENCE, "http://127.0.0.1:8080");

    private JSONObject verifyAssertion(final String assertion) throws IOException, JSONException {
        URL url = new URL(VERIFY_ASSERTION);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod(POST);
        final StringBuilder builder = new StringBuilder(assertion.length());
        builder.append(ASSERTION_IS);
        builder.append(assertion);
        builder.append(AUDIENCE);
        final String output = builder.toString();
        conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(output.length()));
        conn.setDoOutput(true);
        conn.setDoInput(true);
        DataOutputStream outputstream = new DataOutputStream(conn.getOutputStream());
        outputstream.writeBytes(output);
        outputstream.close();
        DataInputStream input = new DataInputStream(conn.getInputStream());
        builder.setLength(0);
        for(int c = input.read(); c != -1; c = input.read())
            builder.append((char) c);
        input.close();
        return new JSONObject(builder.toString());
    }

    public String GetContentType() {
        return ContentTypes.PLAIN_HTML;
    }
}
