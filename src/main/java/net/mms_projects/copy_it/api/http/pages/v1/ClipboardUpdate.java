package net.mms_projects.copy_it.api.http.pages.v1;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.server.database.Database;

public class ClipboardUpdate extends Page {
    @Override
    public FullHttpResponse onGetRequest(HttpRequest request, Database database, int user_id) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database
                                         ,int user_id) throws Exception {
        return null;
    }
}
