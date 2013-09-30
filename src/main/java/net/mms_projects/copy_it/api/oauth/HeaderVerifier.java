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

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import net.mms_projects.copy_it.api.oauth.exceptions.InvalidConsumerException;
import net.mms_projects.copy_it.api.oauth.exceptions.OAuthException;
import net.mms_projects.copy_it.server.database.Database;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        private static final String MISSING_VERIFIER = "Missing oauth_verifier";
        private static final String INVALID_CONSUMER_KEY = "Invalid consumer key";
        private static final String INVALID_TIMESTAMP = "Invalid timestamp";
        private static final String TIMESTAMP_OUT_OF_BOUNDS = "Timestamp is out of bounds (is your time correct?)";
        private static final String INVALID_VERSION = "Invalid OAuth version, only 1.0 is valid";
        private static final String INVALID_SIGNATURE_METHOD = "Invalid signature method, only HMAC-SHA1 is allowed";
        private static final String INVALID_OAUTH_TOKEN = "Invalid OAuth token";
        private static final String INVALID_FIELD_IN_AUTHHEADER = "There's an invalid parameter in the Authorization header";
        private static final String INVALID_PARAMETER = "Invalid parameter";
        private static final String USED_NONCE = "This nonce was used earlier already.";
        private static final String NONCE_TOO_LONG = "Your nonce is too long, maximum length is 8.";
        private static final String INVALID_SIGNATURE = "Invalid signature.";
    }

    private static final class OAuthParameters {
        private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
        private static final String OAUTH_NONCE = "oauth_nonce";
        private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
        private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
        private static final String OAUTH_VERSION = "oauth_version";
        private static final String OAUTH_TOKEN = "oauth_token";
        private static final String OAUTH_SIGNATURE = "oauth_signature";
        private static final String OAUTH_CALLBACK = "oauth_callback";
        private static final String OAUTH_VERIFIER = "oauth_verifier";
        private static final String VERIFIER_KEYS[] = { OAUTH_CONSUMER_KEY, OAUTH_NONCE, OAUTH_SIGNATURE_METHOD, OAUTH_TIMESTAMP, OAUTH_TOKEN, OAUTH_VERIFIER, OAUTH_VERSION };
        private static final String KEYS[] = { OAUTH_CONSUMER_KEY, OAUTH_NONCE, OAUTH_SIGNATURE_METHOD, OAUTH_TIMESTAMP, OAUTH_TOKEN, OAUTH_VERSION };
    }

    public static final class Flags {
        public static final int MAY_MISS_TOKEN    = 0x01;
        public static final int REQUIRES_VERIFIER = 0x02;
    }

    private static final String OAUTH_REALM = "OAuth realm=\"\"";
    private static final String COMMA_REGEX = ", ";
    private static final String EQUALS_REGEX = "=";
    private static final String STRIP_QUOTES_REGEX = "^\"|\"$";
    private static final String EMPTY = "";
    private static final String VALID_OAUTH_VERSION = "1.0";
    private static final String VALID_SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String OAUTH_ = "oauth_";

    public HeaderVerifier(final HttpRequest request, final URI uri) throws OAuthException {
        this(request, uri, 0);
    }

    public HeaderVerifier(final HttpRequest request, final URI uri, final int flags) throws OAuthException {
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
        else if (oauth_params.get(OAuthParameters.OAUTH_NONCE).length() > 8)
            error(ErrorMessages.NONCE_TOO_LONG);
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
        if (((flags & Flags.MAY_MISS_TOKEN) != Flags.MAY_MISS_TOKEN) && !oauth_params.containsKey(OAuthParameters.OAUTH_TOKEN))
            error(ErrorMessages.MISSING_TOKEN);
        if (((flags & Flags.REQUIRES_VERIFIER) == Flags.REQUIRES_VERIFIER) && !oauth_params.containsKey(OAuthParameters.OAUTH_VERIFIER))
            error(ErrorMessages.MISSING_VERIFIER);
        if (!oauth_params.containsKey(OAuthParameters.OAUTH_SIGNATURE))
            error(ErrorMessages.MISSING_SIGNATURE);
        final QueryStringDecoder querydecoder = new QueryStringDecoder(request.getUri());
        final Map<String, List<String>> parameters = querydecoder.parameters();
        final Set<String> keyset = parameters.keySet();
        final Iterator<String> iter = keyset.iterator();
        while (iter.hasNext()) {
            if (iter.next().startsWith(OAUTH_))
                error(ErrorMessages.INVALID_PARAMETER);
        }
        if (exception != null)
            throw exception;
        this.request = request;
        this.uri = uri;
        this.flags = flags;
    }

    public void verifyConsumer(Database database) throws SQLException, OAuthException {
        try {
            consumer = KeyStore.getKeyStore().getConsumer(oauth_params.get(OAuthParameters.OAUTH_CONSUMER_KEY), database);
        } catch (InvalidConsumerException e) {
            throw new OAuthException(ErrorMessages.INVALID_CONSUMER_KEY);
        }
    }

    private static final String SELECT_QUERY = "SELECT user_id, secret_key, scopes " +
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

    private static class User {
        public static final String SECRET_KEY = "secret_key";
        public static final String USER_ID = "user_id";
        public static final String SCOPES = "scopes";
        public User(ResultSet result) throws SQLException {
            secret = result.getString(SECRET_KEY);
            user_id = result.getInt(USER_ID);
            scope = Scope.fromDatabase(result.getInt(SCOPES));
        }

        public final String getSecretKey() { return secret; }
        public final int getUserId() { return user_id; }
        public final Scope getScope() { return scope; }
        private final String secret;
        private final int user_id;
        private final Scope scope;
    }

    public static class FakeUser extends User {
        public FakeUser(ResultSet resultSet) throws SQLException {
            super(resultSet);
        }
    }

    public void setFakeUser(FakeUser fakeUser) {
        if (user == null)
            user = fakeUser;
    }

    private static final String NONCE_CHECKING_QUERY = "SELECT 1 " +
                                                       "FROM nonces " +
                                                       "WHERE nonce = ? " +
                                                       "AND _id = ? " +
                                                       "AND (NOW() - INTERVAL 5 MINUTE) < timestamp";
    private static final String NONCE_INSERT_QUERY = "INSERT INTO nonces " +
                                                     "(_id, nonce) " +
                                                     "VALUES (?, ?);";


    public void verifyOAuthNonce(Database database) throws SQLException, OAuthException {
        if (user == null) /* Should NEVER happen! */
            throw new OAuthException("user is null!");
        final String oauth_nonce = oauth_params.get(OAuthParameters.OAUTH_NONCE);
        PreparedStatement statement = database.getConnection().prepareStatement(NONCE_CHECKING_QUERY);
        statement.setString(1, oauth_nonce);
        statement.setInt(2, user.getUserId());
        ResultSet result = statement.executeQuery();
        if (result.first()) {
            result.close();
            throw new OAuthException(ErrorMessages.USED_NONCE);
        }
        result.close();
        PreparedStatement insertStatement = database.getConnection().prepareStatement(NONCE_INSERT_QUERY);
        insertStatement.setInt(1, user.getUserId());
        insertStatement.setString(2, oauth_nonce);
        insertStatement.execute();
        database.getConnection().commit();
    }

    private static final String HMAC_SHA1 = "HmacSHA1";

    public void checkSignature(HttpPostRequestDecoder postRequestDecoder, boolean https) throws UnsupportedEncodingException, URISyntaxException, OAuthException {
        final String signed_with = oauth_params.get(OAuthParameters.OAUTH_SIGNATURE);
        final String raw = createRaw(postRequestDecoder, https);
        final String secretkey = consumer.getSecretKey() + "&" + user.getSecretKey();
        try {
            final Key signingKey = new SecretKeySpec(secretkey.getBytes(), HMAC_SHA1);
            final Mac mac = Mac.getInstance(HMAC_SHA1);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(raw.getBytes());
            final String signature = new String(Base64.encodeBase64(rawHmac));
            System.err.println("Signed with: " + URLDecoder.decode(signed_with, UTF_8));
            System.err.println("Should be::: " + signature);
            if (!URLDecoder.decode(signed_with, UTF_8).equals(signature))
                throw new OAuthException(ErrorMessages.INVALID_SIGNATURE);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private static final String HTTP = "http";
    private static final String COLON_SLASH_SLASH = "%3A%2F%2F";
    private static final String UTF_8 = "UTF-8";
    private static final String EQUALS = "%3D";
    private static final String AND = "%26";
    private static final String PLUS = "+";
    private static final String PLUS_ENCODED = "%20";

    private String createRaw(HttpPostRequestDecoder post, boolean https) throws UnsupportedEncodingException, URISyntaxException {
        final StringBuilder rawbuilder = new StringBuilder();
        rawbuilder.append(request.getMethod().toString());
        rawbuilder.append('&');
        rawbuilder.append(HTTP);
        if (https)
            rawbuilder.append('s');
        rawbuilder.append(COLON_SLASH_SLASH);
        rawbuilder.append(URLEncoder.encode(request.headers().get(HOST), UTF_8));
        rawbuilder.append(URLEncoder.encode(uri.getPath(), UTF_8));
        rawbuilder.append('&');
        if (uri.getQuery() == null && request.getMethod() == HttpMethod.GET) {
            String[] loop_through = OAuthParameters.KEYS;
            if ((flags & Flags.REQUIRES_VERIFIER) == Flags.REQUIRES_VERIFIER)
                loop_through = OAuthParameters.VERIFIER_KEYS;
            for (int i = 0; i < loop_through.length; i++) {
                rawbuilder.append(loop_through[i]);
                rawbuilder.append(EQUALS);
                rawbuilder.append(URLEncoder.encode(oauth_params.get(loop_through[i]), UTF_8));
                if (i != (loop_through.length - 1))
                    rawbuilder.append(AND);
            }
        } else {
            final List<String> keys = new ArrayList<String>();
            final Map<String, String> parameters = new HashMap<String, String>();
            if (request.getMethod() == HttpMethod.GET) {
                final QueryStringDecoder querydecoder = new QueryStringDecoder(uri);
                final Map<String, List<String>> get_parameters = querydecoder.parameters();
                final Set<String> keyset = parameters.keySet();
                final Iterator<String> iter = keyset.iterator();
                while (iter.hasNext()) {
                    final String key = iter.next();
                    keys.add(key);
                    parameters.put(key, get_parameters.get(key).get(0));
                }
            } else if (request.getMethod() == HttpMethod.POST) {
                final List<InterfaceHttpData> post_parameters = post.getBodyHttpDatas();
                final Iterator<InterfaceHttpData> iter = post_parameters.iterator();
                while (iter.hasNext()) {
                    final InterfaceHttpData data = iter.next();
                    try {
                        final HttpData httpData = (HttpData) data;
                        parameters.put(httpData.getName(), URLEncoder.encode(httpData.getString(), UTF_8).replace(PLUS, PLUS_ENCODED));
                        keys.add(httpData.getName());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            for (int i = 0; i < OAuthParameters.KEYS.length; i++)
                keys.add(OAuthParameters.KEYS[i]);
            if ((flags & Flags.REQUIRES_VERIFIER) == Flags.REQUIRES_VERIFIER)
                keys.add(OAuthParameters.OAUTH_VERIFIER);
            Collections.sort(keys);
            final int length = keys.size();
            for (int i = 0; i < length; i++) {
                final String key = keys.get(i);
                rawbuilder.append(key);
                rawbuilder.append(EQUALS);
                if (key.startsWith(OAUTH_))
                    rawbuilder.append(URLEncoder.encode(oauth_params.get(key), UTF_8));
                else
                    rawbuilder.append(URLEncoder.encode(parameters.get(key), UTF_8));
                if (i != (length - 1))
                    rawbuilder.append(AND);
            }
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

    public URI getUri() { return uri; }
    public int getUserId() { return user.getUserId(); }
    public int getConsumerId() { return consumer.getId(); }
    public int getConsumerFlags() { return consumer.getFlags(); }
    public Scope getConsumerScope() { return consumer.getScope(); }
    public Scope getUserScope() { return user.getScope(); }
    public String getCallbackUri() throws UnsupportedEncodingException {
        String callback = oauth_params.get(OAuthParameters.OAUTH_CALLBACK);
        if (callback == null)
            return null;
        return URLDecoder.decode(callback, UTF_8);
    }
    public String getVerifier() { return oauth_params.get(OAuthParameters.OAUTH_VERIFIER); }
    public String getPublicToken() { return oauth_params.get(OAuthParameters.OAUTH_TOKEN); }

    private final String auth_header;
    private final HttpRequest request;
    private final LinkedHashMap<String, String> oauth_params;
    private OAuthException exception;
    private Consumer consumer;
    private User user;
    private final URI uri;
    private final int flags;
}
