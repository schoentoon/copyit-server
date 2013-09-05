package net.mms_projects.copy_it.api.http.pages.v1;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.database.Database;

public class ClipboardUpdate extends Page {
    private static final String MISSING_DATA_PARAMETER = "Missing the data parameter.";
    @Override
    public FullHttpResponse onGetRequest(HttpRequest request, Database database, int user_id) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database
                                         ,int user_id) throws Exception {
        final InterfaceHttpData data = postRequestDecoder.getBodyHttpData("data");
        if (data == null)
            throw new ErrorException(MISSING_DATA_PARAMETER);
        return null;
    }
}
