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

package net.mms_projects.copy_it.api.http.pages.android;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.database.Database;
import org.json.JSONObject;

import java.sql.PreparedStatement;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class RegisterGCM extends Page {
    public FullHttpResponse onGetRequest(final HttpRequest request, final Database database, int user_id) throws Exception {
        throw new UnsupportedOperationException();
    };

    private static final String MISSING_GCM_TOKEN = "Missing \"gcm_token\"";
    private static final String GCM_TOKEN_TOO_LONG = "Gcm token is too long, are you sure this is a gcm token?";

    private static final String GCM_TOKEN = "gcm_token";
    private static final String INSERT_STATEMENT = "INSERT IGNORE INTO gcm_ids (user_id, gcm_token) VALUES (?, ?)";

    public FullHttpResponse onPostRequest(final HttpRequest request, final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database, int user_id) throws Exception {
        InterfaceHttpData gcm_token = postRequestDecoder.getBodyHttpData(GCM_TOKEN);
        if (gcm_token != null && gcm_token instanceof HttpData) {
            final String gcm_id = ((HttpData) gcm_token).getString();
            if (gcm_id.length() < 256) {
                PreparedStatement statement = database.getConnection().prepareStatement(INSERT_STATEMENT);
                statement.setInt(1, user_id);
                statement.setString(2, gcm_id);
                if (statement.executeUpdate() > 0)
                    database.getConnection().commit();
                JSONObject json = new JSONObject();
                return new DefaultFullHttpResponse(request.getProtocolVersion()
                        ,OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
            } else
                throw new ErrorException(GCM_TOKEN_TOO_LONG);
        } else
            throw new ErrorException(MISSING_GCM_TOKEN);
    };
}
