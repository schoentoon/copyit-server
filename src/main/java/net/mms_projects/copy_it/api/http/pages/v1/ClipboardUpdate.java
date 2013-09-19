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
import net.mms_projects.copy_it.api.oauth.HeaderVerifier;
import net.mms_projects.copy_it.server.config.Config;
import net.mms_projects.copy_it.server.database.Database;
import net.mms_projects.copy_it.server.database.DatabasePool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ClipboardUpdate extends AuthPage {
    private static final String MISSING_DATA_PARAMETER = "Missing the data parameter.";
    public FullHttpResponse onGetRequest(HttpRequest request, Database database, final HeaderVerifier headerVerifier) throws Exception {
        throw new UnsupportedOperationException();
    }

    private static final String INSERT_DATA = "REPLACE INTO clipboard_data (user_id, data) VALUES (?, ?)";

    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database
                                         ,final HeaderVerifier headerVerifier) throws Exception {
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
        postProcess(new PushNotification(headerVerifier.getUserId()));
        final JSONObject json = new JSONObject();
        return new DefaultFullHttpResponse(request.getProtocolVersion()
                ,OK, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
    }

    private static final class PushNotification implements Runnable {
        private PushNotification(int user_id) {
            this.user_id = user_id;
        }

        private static final String SELECT_GCM_TOKENS = "SELECT gcm_token " +
                                                        "FROM gcm_ids " +
                                                        "WHERE user_id = ?";
        private static final String GCM_TOKEN = "gcm_token";
        private static final String REGISTRATION_IDS = "registration_ids";
        private static final String DATA = "data";
        private static final String GCM_URL = "https://android.googleapis.com/gcm/send";
        private static final String POST = "POST";
        private static final String KEY_IS = "key=";

        public void run() {
            Database database = null;
            try {
                database = DatabasePool.getDBConnection();
                PreparedStatement statement = database.getConnection().prepareStatement(SELECT_GCM_TOKENS);
                statement.setInt(1, user_id);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.first()) {
                    JSONObject json = new JSONObject();
                    JSONArray ids_array = new JSONArray();
                    json.put(REGISTRATION_IDS, ids_array);
                    do {
                        ids_array.put(resultSet.getString(GCM_TOKEN));
                    } while (resultSet.next());
                    resultSet.close();
                    JSONObject data = new JSONObject();
                    json.put(DATA, data);
                    data.put("test", 123);
                    System.err.println("Input json: " + json.toString());
                    URL url = new URL(GCM_URL);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod(POST);
                    conn.setRequestProperty(CONTENT_TYPE, ContentTypes.JSON_TYPE);
                    conn.setRequestProperty(AUTHORIZATION, KEY_IS + Config.getString(GCM_TOKEN));
                    final String output = json.toString();
                    conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(output.length()));
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    DataOutputStream outputstream = new DataOutputStream(conn.getOutputStream());
                    outputstream.writeBytes(output);
                    outputstream.close();
                    DataInputStream input = new DataInputStream(conn.getInputStream());
                    StringBuilder builder = new StringBuilder(input.available());
                    for(int c = input.read(); c != -1; c = input.read())
                        builder.append((char) c);
                    input.close();
                    JSONObject outputjson = new JSONObject(builder.toString());
                    System.err.println("Output json: " + outputjson.toString());
                } else
                    resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (database != null)
                    database.free();
            }
        }

        private final int user_id;
    }
}
