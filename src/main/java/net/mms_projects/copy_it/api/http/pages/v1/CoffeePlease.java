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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.api.http.pages.exceptions.ErrorException;
import net.mms_projects.copy_it.server.database.Database;

public class CoffeePlease extends Page {
    private static final HttpResponseStatus I_AM_A_TEAPOT = new HttpResponseStatus(418, "I'm a teapot");
    private static final String SORRY_ONLY_HAVE_TEA = "Sorry, I only have tea.";

    public FullHttpResponse onGetRequest(HttpRequest request, Database database) throws Exception {
        ErrorException errorException = new ErrorException(SORRY_ONLY_HAVE_TEA);
        return new DefaultFullHttpResponse(request.getProtocolVersion()
                ,I_AM_A_TEAPOT, Unpooled.copiedBuffer(errorException.toString(), CharsetUtil.UTF_8));
    }

    public FullHttpResponse onPostRequest(HttpRequest request, HttpPostRequestDecoder postRequestDecoder, Database database) throws Exception {
        return onGetRequest(request, database);
    }
}
