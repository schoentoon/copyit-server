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
import net.mms_projects.copy_it.api.oauth.exceptions.OAuthException;
import net.mms_projects.copy_it.server.database.Database;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;

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

    public HeaderVerifier(final HttpRequest request) throws OAuthException {
        if (!request.headers().contains(AUTHORIZATION))
            throw new OAuthException(ErrorMessages.NO_AUTH_HEADER);
        auth_header = request.headers().get(AUTHORIZATION);
        if (!auth_header.startsWith(OAUTH_REALM))
            throw new OAuthException(ErrorMessages.NO_REALM_PRESENT);
        String[] split = auth_header.split(COMMA_REGEX);
        oauth_params = new HashMap<String, String>();
        for (int i = 0; i < split.length; i++) {
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
                if ((now-300) > timestamp && timestamp < (now+300))
                    error(ErrorMessages.TIMESTAMP_OUT_OF_BOUNDS);
            } catch (NumberFormatException e) {
                error(ErrorMessages.INVALID_TIMESTAMP);
            }
        }
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_SIGNATURE_METHOD))
            error(ErrorMessages.MISSING_SIGNATURE_METHOD);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_VERSION))
            error(ErrorMessages.MISSING_VERSION);
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
        } catch (KeyStore.InvalidConsumerException e) {
            throw new OAuthException(ErrorMessages.INVALID_CONSUMER_KEY);
        }
    }

    private void error(String message) {
        if (exception == null)
            exception = new OAuthException(message);
        else
            exception.addError(message);
    }

    private final String auth_header;
    private final HttpRequest request;
    private final Map<String, String> oauth_params;
    private OAuthException exception;
    private Consumer consumer;
}
