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

package net.mms_projects.copy_it.api.http;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import net.mms_projects.copy_it.api.oauth.HeaderVerifier;
import net.mms_projects.copy_it.server.database.Database;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AuthPage extends Page {
    public abstract FullHttpResponse onGetRequest(final HttpRequest request
                                                 ,final Database database
                                                 ,final HeaderVerifier headerVerifier) throws Exception;

    public abstract FullHttpResponse onPostRequest(final HttpRequest request
                                                  ,final HttpPostRequestDecoder postRequestDecoder
                                                  ,final Database database
                                                  ,final HeaderVerifier headerVerifier) throws Exception;

    public FullHttpResponse onGetRequest(final HttpRequest request
                                        ,final Database database) throws Exception {
        throw new RuntimeException();
    };

    public FullHttpResponse onPostRequest(final HttpRequest request
                                         ,final HttpPostRequestDecoder postRequestDecoder
                                         ,final Database database) throws Exception {
        throw new RuntimeException();
    };

    public String GetContentType() {
        return ContentTypes.JSON_TYPE;
    }

    protected void postProcess(final Runnable runnable) {
        if (runnable != null)
            EXECUTOR_SERVICE.submit(runnable);
    }

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
}
