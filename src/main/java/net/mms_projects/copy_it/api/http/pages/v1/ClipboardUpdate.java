package net.mms_projects.copy_it.api.http.pages.v1;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.database.Database;
import java.sql.PreparedStatement;

public class ClipboardUpdate extends Page {
    private static final String MISSING_DATA_PARAMETER = "Missing the data parameter.";
    public FullHttpResponse onGetRequest(HttpRequest request, Database database, int user_id) throws Exception {
        throw new UnsupportedOperationException();
    }

    private static final String INSERT_DATA = "REPLACE INTO clipboard_data (user_id, data) VALUES (?, ?)";

    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database
                                         ,final int user_id) throws Exception {
        final InterfaceHttpData data = postRequestDecoder.getBodyHttpData("data");
        if (data == null)
            throw new ErrorException(MISSING_DATA_PARAMETER);
        if (data instanceof HttpData) {
            PreparedStatement statement = database.getConnection().prepareStatement(INSERT_DATA);
            statement.setInt(1, user_id);
            statement.setString(2, ((HttpData) data).getString());
            statement.execute();
        } else
            throw new Exception("LOL NOPE");
        return null;
    }
}
