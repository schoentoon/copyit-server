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

package net.mms_projects.copy_it.api.http.pages.v1;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.AuthPage;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.api.oauth.HeaderVerifier;
import net.mms_projects.copy_it.server.database.Database;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ClipboardGet extends AuthPage {
    private static final String NO_CONTENT_POSTED = "No content posted yet.";
    private static final String CONTENT = "content";
    private static final String DATA = "data";
    private static final String LAST_UPDATED = "last_updated";
    private static final String SELECT_CONTENT = "SELECT data, UNIX_TIMESTAMP(last_updated) last_updated " +
                                                 "FROM clipboard_data " +
                                                 "WHERE user_id = ?";

    private static final String NO_READ_PERMISSION = "No read permissions";

    public FullHttpResponse onGetRequest(HttpRequest request, Database database, HeaderVerifier headerVerifier) throws Exception {
        if (!headerVerifier.getConsumerScope().canRead())
            throw new ErrorException(NO_READ_PERMISSION);
        PreparedStatement statement = database.getConnection().prepareStatement(SELECT_CONTENT);
        statement.setInt(1, headerVerifier.getUserId());
        ResultSet result = statement.executeQuery();
        if (result.first()) {
            final JSONObject json = new JSONObject();
            json.put(CONTENT, result.getString(DATA));
            json.put(LAST_UPDATED, result.getInt(LAST_UPDATED));
            result.close();
            return new DefaultFullHttpResponse(request.getProtocolVersion()
                    ,OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
        }
        result.close();
        throw new ErrorException(NO_CONTENT_POSTED);
    }

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database, HeaderVerifier headerVerifier) throws Exception {
        throw new UnsupportedOperationException();
    }
}
