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

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import net.mms_projects.copy_it.api.oauth.exceptions.OAuthException;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;

public class HeaderVerifierTest {
    private final SecureRandom random = new SecureRandom();

    @Test(expected=OAuthException.class,timeout=500)
    public void noAuthHeader() throws OAuthException {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void noRealmPresent() throws OAuthException {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, "A totally invalid authorization header.");
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthConsumer() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthNonce() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthTimestamp() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void testTimestampTooLate() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString((System.currentTimeMillis()/1000)+9001) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void testTimestampTooEarly() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString((System.currentTimeMillis()/1000)-9001) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthSignatureMethod() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void invalidOAuthSignatureMethod() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"MD5\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_version=\"1.0\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthVersion() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void invalidOAuthVersion() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_version=\"2.0\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthToken() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void missingOAuthSignature() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(expected=OAuthException.class,timeout=500)
    public void invalidParameter() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\"" +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"" +
                "i_am_invalid=\"I'm totally fake\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }

    @Test(timeout=500)
    public void validRequest() throws OAuthException {
        String header = "OAuth realm=\"\", " +
                "oauth_consumer_key=\"401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d\", " +
                "oauth_nonce=\"" + (new BigInteger(130, random).toString(32)) + "\", " +
                "oauth_timestamp=\"" + Long.toString(System.currentTimeMillis()/1000) + "\", " +
                "oauth_signature_method=\"HMAC-SHA1\", " +
                "oauth_version=\"1.0\", " +
                "oauth_token=\"oauth_token\", " +
                "oauth_signature=\"CBTk%2FvzxEqqr0AvhnVgdWNHuKfw%3D\"";
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://127.0.0.1:8080/");
        request.headers().add(AUTHORIZATION, header);
        new HeaderVerifier(request);
    }
}
