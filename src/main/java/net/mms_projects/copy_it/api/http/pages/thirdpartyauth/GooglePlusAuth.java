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

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.database.Database;
import org.json.JSONObject;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class GooglePlusAuth extends AbstractAuthorize {
    private static final String CODE = "code";
    private static final String MISSING_CODE_PARAMETER = "Missing code parameter";
    private static final String GOOGLE_AUTH_FAILED = "Google+ signin failed";

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        final InterfaceHttpData code_raw = postRequestDecoder.getBodyHttpData(CODE);
        if (code_raw == null || !(code_raw instanceof HttpData))
            throw new ErrorException(MISSING_CODE_PARAMETER);
        final String code = ((HttpData) code_raw).getString();
        Tokeninfo verified = checkCode(code);
        if (verified.getVerifiedEmail() && verified.getEmail() != null) {
            JSONObject json = new JSONObject();
            json.put(SESSION, generateCookie(verified.getEmail(), database));
            return new DefaultFullHttpResponse(request.getProtocolVersion(), OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
        } else
            throw new ErrorException(GOOGLE_AUTH_FAILED);
    }

    private static final HttpTransport TRANSPORT = new NetHttpTransport();
    private static final JacksonFactory JSON_FACTORY = new JacksonFactory();
    private static final String CLIENT_ID = Config.getStringSafe(Config.Keys.GOOGLE_CLIENT_ID, null);
    private static final String CLIENT_SECRET = Config.getStringSafe(Config.Keys.GOOGLE_SECRET_KEY, null);
    private static final String POSTMESSAGE = "postmessage";

    private Tokeninfo checkCode(final String code) throws IOException {
        GoogleTokenResponse tokenResponse =
                new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY,
                        CLIENT_ID, CLIENT_SECRET, code, POSTMESSAGE).execute();
        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(JSON_FACTORY)
                .setTransport(TRANSPORT)
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET).build()
                .setFromTokenResponse(tokenResponse);

        // Check that the token is valid.
        Oauth2 oauth2 = new Oauth2.Builder(
                TRANSPORT, JSON_FACTORY, credential).build();
        Tokeninfo tokenInfo = oauth2.tokeninfo()
                .setAccessToken(credential.getAccessToken()).execute();
        return tokenInfo;
    }

    public boolean checkConfig() {
        return Config.hasString(Config.Keys.GOOGLE_CLIENT_ID) && Config.hasString(Config.Keys.GOOGLE_SECRET_KEY);
    }
}
