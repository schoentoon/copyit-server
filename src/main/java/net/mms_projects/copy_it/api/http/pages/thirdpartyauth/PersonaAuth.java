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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class PersonaAuth extends AbstractAuthorize {
    private static final String STATUS = "status";
    private static final String OKAY = "okay";
    private static final String PERSONA_AUTH_FAILED = "Persona auth failed.";
    private static final String MISSING_ASSERTION = "We're missing our assertion here..";
    private static final String ASSERTION = "assertion";
    private static final String EMAIL = "email";

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        final InterfaceHttpData assertion_raw = postRequestDecoder.getBodyHttpData(ASSERTION);
        if (assertion_raw == null || !(assertion_raw instanceof HttpData))
            throw new ErrorException(MISSING_ASSERTION);
        final String assertion = ((HttpData) assertion_raw).getString();
        JSONObject verified = verifyAssertion(assertion);
        if (verified.has(EMAIL) && verified.has(STATUS) && OKAY.equals(verified.getString(STATUS))) {
            JSONObject json = new JSONObject();
            json.put(SESSION, generateCookie(verified.getString(EMAIL), database));
            return new DefaultFullHttpResponse(request.getProtocolVersion(), OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
        } else
            throw new ErrorException(PERSONA_AUTH_FAILED);
    }


    private static final String VERIFY_ASSERTION = "https://verifier.login.persona.org/verify";
    private static final String AUDIENCE = "audience";
    private static final HttpClient CLIENT = HttpClients.createMinimal();

    private JSONObject verifyAssertion(final String assertion) throws IOException, JSONException {
        HttpPost post = new HttpPost(VERIFY_ASSERTION);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);
        parameters.add(new BasicNameValuePair(ASSERTION, assertion));
        parameters.add(new BasicNameValuePair(AUDIENCE, Config.getStringSafe(Config.Keys.AUDIENCE, null)));
        post.setEntity(new UrlEncodedFormEntity(parameters));
        HttpResponse response = CLIENT.execute(post);
        HttpEntity entity = response.getEntity();
        return new JSONObject(EntityUtils.toString(entity));
    }

    public boolean checkConfig() {
        return Config.hasString(Config.Keys.AUDIENCE);
    }
}
