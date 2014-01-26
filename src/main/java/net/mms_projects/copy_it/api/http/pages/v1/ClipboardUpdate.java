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
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.AuthPage;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.api.http.pages.exceptions.UnsupportedMethodException;
import net.mms_projects.copy_it.api.oauth.HeaderVerifier;
import net.mms_projects.copy_it.server.database.Database;
import net.mms_projects.copy_it.server.push.android.GCMRunnable;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ClipboardUpdate extends AuthPage {
    private static final String MISSING_DATA_PARAMETER = "Missing the data parameter.";
    public FullHttpResponse onGetRequest(HttpRequest request, Database database, final HeaderVerifier headerVerifier) throws Exception {
        throw new UnsupportedMethodException(request.getMethod());
    }

    private static final String INSERT_DATA = "REPLACE INTO clipboard_data (user_id, data) VALUES (?, ?)";

    private static final String NO_WRITE_PERMISSION = "No write permissions.";

    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database
                                         ,final HeaderVerifier headerVerifier) throws Exception {
        if (!headerVerifier.getConsumerScope().canWrite() || !headerVerifier.getUserScope().canWrite())
            throw new ErrorException(NO_WRITE_PERMISSION);
        final InterfaceHttpData data = postRequestDecoder.getBodyHttpData("data");
        if (data == null)
            throw new ErrorException(MISSING_DATA_PARAMETER);
        if (data instanceof HttpData) {
            PreparedStatement statement = database.getConnection().prepareStatement(INSERT_DATA);
            statement.setInt(1, headerVerifier.getUserId());
            statement.setString(2, ((HttpData) data).getString());
            statement.execute();
        } else
            throw new ErrorException(MISSING_DATA_PARAMETER);
        dispatchNotification(database, headerVerifier.getUserId());
        final JSONObject json = new JSONObject();
        return new DefaultFullHttpResponse(request.getProtocolVersion()
                ,OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
    }

    private static final String SELECT_GCM_TOKENS = "SELECT gcm_token " +
            "FROM gcm_ids " +
            "WHERE user_id = ?";
    private static final String GCM_TOKEN = "gcm_token";

    public void dispatchNotification(Database database, int user_id) throws SQLException {
        PreparedStatement statement = database.getConnection().prepareStatement(SELECT_GCM_TOKENS);
        statement.setInt(1, user_id);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.first()) {
            GCMRunnable gcm = new GCMRunnable();
            do {
                gcm.addRegistrationId(resultSet.getString(GCM_TOKEN));
            } while (resultSet.next());
            gcm.setData("action", "content-updated");
            postProcess(gcm);
        }
        resultSet.close();
    }
}
