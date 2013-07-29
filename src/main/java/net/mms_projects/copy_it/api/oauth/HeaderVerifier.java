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

import io.netty.handler.codec.http.HttpRequest;
import net.mms_projects.copy_it.api.oauth.exceptions.InvalidConsumerException;
import net.mms_projects.copy_it.api.oauth.exceptions.OAuthException;
import net.mms_projects.copy_it.server.database.Database;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;

public class HeaderVerifier {
    private static final class ErrorMessages {
        private static final String NO_AUTH_HEADER = "No authorization header present";
        private static final String NO_REALM_PRESENT = "No OAuth realm present";
        private static final String MISSING_CONSUMER_KEY = "Missing oauth_consumer_key";
        private static final String MISSING_NONCE = "Missing oauth_nonce";
        private static final String MISSING_TIMESTAMP = "Missing oauth_timestamp";
        private static final String MISSING_SIGNATURE_METHOD = "Missing oauth_signature_method";
        private static final String MISSING_VERSION = "Missing oauth_version";
        private static final String MISSING_TOKEN = "Missing oauth_token";
        private static final String MISSING_SIGNATURE = "Missing oauth_signature";
        private static final String INVALID_CONSUMER_KEY = "Invalid consumer key";
        private static final String INVALID_TIMESTAMP = "Invalid timestamp";
        private static final String TIMESTAMP_OUT_OF_BOUNDS = "Timestamp is out of bounds (is your time correct?)";
        private static final String INVALID_VERSION = "Invalid OAuth version, only 1.0 is valid";
        private static final String INVALID_SIGNATURE_METHOD = "Invalid signature method, only HMAC-SHA1 is allowed";
        private static final String INVALID_OAUTH_TOKEN = "Invalid OAuth token";
        private static final String INVALID_FIELD_IN_AUTHHEADER = "There's an invalid parameter in the Authorization header";
    }

    private static final class OAuthParameters {
        private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
        private static final String OAUTH_NONCE = "oauth_nonce";
        private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
        private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
        private static final String OAUTH_VERSION = "oauth_version";
        private static final String OAUTH_TOKEN = "oauth_token";
        private static final String OAUTH_SIGNATURE = "oauth_signature";
    }

    private static final String OAUTH_REALM = "OAuth realm=\"\"";
    private static final String COMMA_REGEX = ", ";
    private static final String EQUALS_REGEX = "=";
    private static final String STRIP_QUOTES_REGEX = "^\"|\"$";
    private static final String EMPTY = "";
    private static final String VALID_OAUTH_VERSION = "1.0";
    private static final String VALID_SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String OAUTH_ = "oauth_";

    public HeaderVerifier(final HttpRequest request) throws OAuthException {
        if (!request.headers().contains(AUTHORIZATION))
            throw new OAuthException(ErrorMessages.NO_AUTH_HEADER);
        auth_header = request.headers().get(AUTHORIZATION);
        if (!auth_header.startsWith(OAUTH_REALM))
            throw new OAuthException(ErrorMessages.NO_REALM_PRESENT);
        String[] split = auth_header.split(COMMA_REGEX);
        oauth_params = new LinkedHashMap<String, String>();
        for (int i = 1; i < split.length; i++) {
            if (!split[i].startsWith(OAUTH_))
                throw new OAuthException(ErrorMessages.INVALID_FIELD_IN_AUTHHEADER);
            String[] split_header = split[i].split(EQUALS_REGEX, 2);
            oauth_params.put(split_header[0], split_header[1].replaceAll(STRIP_QUOTES_REGEX, EMPTY));
        }
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_CONSUMER_KEY))
            error(ErrorMessages.MISSING_CONSUMER_KEY);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_NONCE))
            error(ErrorMessages.MISSING_NONCE);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_TIMESTAMP))
            error(ErrorMessages.MISSING_TIMESTAMP);
        else {
            try {
                final long timestamp = Integer.valueOf(oauth_params.get(OAuthParameters.OAUTH_TIMESTAMP)).longValue();
                final long now = (System.currentTimeMillis() / 1000);
                if (timestamp < (now-300) || (now+300) < timestamp)
                    error(ErrorMessages.TIMESTAMP_OUT_OF_BOUNDS);
            } catch (NumberFormatException e) {
                error(ErrorMessages.INVALID_TIMESTAMP);
            }
        }
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_SIGNATURE_METHOD))
            error(ErrorMessages.MISSING_SIGNATURE_METHOD);
        else if (!oauth_params.get(OAuthParameters.OAUTH_SIGNATURE_METHOD).equals(VALID_SIGNATURE_METHOD))
            error(ErrorMessages.INVALID_SIGNATURE_METHOD);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_VERSION))
            error(ErrorMessages.MISSING_VERSION);
        else if (!oauth_params.get(OAuthParameters.OAUTH_VERSION).equals(VALID_OAUTH_VERSION))
            error(ErrorMessages.INVALID_VERSION);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_TOKEN))
            error(ErrorMessages.MISSING_TOKEN);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_SIGNATURE))
            error(ErrorMessages.MISSING_SIGNATURE);
        if (exception != null)
            throw exception;
        this.request = request;
    }

    public void verifyConsumer(Database database) throws SQLException, OAuthException {
        try {
            consumer = KeyStore.getKeyStore().getConsumer(oauth_params.get(OAuthParameters.OAUTH_CONSUMER_KEY), database);
        } catch (InvalidConsumerException e) {
            throw new OAuthException(ErrorMessages.INVALID_CONSUMER_KEY);
        }
    }

    private static final String SELECT_QUERY = "SELECT user_id, secret_key " +
                                               "FROM user_tokens " +
                                               "WHERE application_id = ? " +
                                               "AND public_key = ? " +
                                               "LIMIT 1";

    public void verifyOAuthToken(Database database) throws SQLException, OAuthException {
        final String oauth_token = oauth_params.get(OAuthParameters.OAUTH_TOKEN);
        PreparedStatement statement = database.getConnection().prepareStatement(SELECT_QUERY);
        statement.setInt(1, consumer.getId());
        statement.setString(2, oauth_token);
        ResultSet result = statement.executeQuery();
        if (result.first())
            user = new User(result);
        result.close();
        if (user == null)
            throw new OAuthException(ErrorMessages.INVALID_OAUTH_TOKEN);
    }

    private final class User {
        public static final String SECRET_KEY = "secret_key";
        public static final String USER_ID = "user_id";
        public User(ResultSet result) throws SQLException {
            secret = result.getString(SECRET_KEY);
            user_id = result.getInt(USER_ID);
        }

        public final String getPublicKey() { return oauth_params.get(OAuthParameters.OAUTH_TOKEN); }
        public final String getSecretKey() { return secret; }
        public final int getUserId() { return user_id; }
        private final String secret;
        private final int user_id;
    }

    public void checkSignature(boolean https) throws UnsupportedEncodingException {
        final String signature = oauth_params.get(OAuthParameters.OAUTH_SIGNATURE);
        final String raw = createRaw(https);
    }

    private static final String HTTP = "http";
    private static final String COLON_SLASH_SLASH = "%3A%2F%2F";
    private static final String UTF_8 = "UTF-8";
    private static final String EQUALS = "%3D";
    private static final String AND = "%26";

    private String createRaw(boolean https) throws UnsupportedEncodingException {
        final StringBuilder rawbuilder = new StringBuilder();
        rawbuilder.append(request.getMethod().toString());
        rawbuilder.append('&');
        rawbuilder.append(HTTP);
        if (https)
            rawbuilder.append('s');
        rawbuilder.append(COLON_SLASH_SLASH);
        rawbuilder.append(request.headers().get(HOST));
        rawbuilder.append(URLEncoder.encode(request.getUri(), UTF_8));
        rawbuilder.append('&');
        final String[] keys = new String[oauth_params.size()];
        oauth_params.keySet().toArray(keys);
        for (int i = 0; i < keys.length; i++) {
            rawbuilder.append(keys[i]);
            rawbuilder.append(EQUALS);
            rawbuilder.append(URLEncoder.encode(oauth_params.get(keys[i]), UTF_8));
            if (i != (keys.length - 1))
                rawbuilder.append(AND);
        }
        System.err.println(rawbuilder.toString());
        return rawbuilder.toString();
    }

    private void error(String message) {
        if (exception == null)
            exception = new OAuthException(message);
        else
            exception.addError(message);
    }

    private final String auth_header;
    private final HttpRequest request;
    private final LinkedHashMap<String, String> oauth_params;
    private OAuthException exception;
    private Consumer consumer;
    private User user;
}
