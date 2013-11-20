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
import net.mms_projects.copy_it.api.http.pages.android.RegisterGCM;
import net.mms_projects.copy_it.api.http.pages.android.UnRegisterGCM;
import net.mms_projects.copy_it.api.http.pages.oauth.AccessToken;
import net.mms_projects.copy_it.api.http.pages.oauth.Authorize;
import net.mms_projects.copy_it.api.http.pages.oauth.RequestToken;
import net.mms_projects.copy_it.api.http.pages.thirdpartyauth.GooglePlusAuth;
import net.mms_projects.copy_it.api.http.pages.thirdpartyauth.PersonaAuth;
import net.mms_projects.copy_it.api.http.pages.v1.ClipboardGet;
import net.mms_projects.copy_it.api.http.pages.v1.ClipboardUpdate;
import net.mms_projects.copy_it.api.http.pages.v1.CoffeePlease;
import net.mms_projects.copy_it.server.Messages;
import net.mms_projects.copy_it.server.database.Database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Page {
    private final static class Pages {
        private static final HashMap<String, AuthPage> oauth_pages = new HashMap<String, AuthPage>();
        private static final HashMap<String, Page> noauth_pages = new HashMap<String, Page>();

        /**
         * Register all your page handlers here with simple /uris/, don't bother with doing config checks yourself,
         * bottom of this method automatically filters them. Put all the page handlers using AuthPage in oauth_pages
         * all the others in noauth_pages.
         */
        private static void init() {
            oauth_pages.put("/1/clipboard/update", new ClipboardUpdate());
            oauth_pages.put("/1/clipboard/get", new ClipboardGet());
            oauth_pages.put("/1/android/register", new RegisterGCM());
            oauth_pages.put("/1/android/unregister", new UnRegisterGCM());
            noauth_pages.put("/oauth/request_token", new RequestToken());
            noauth_pages.put("/oauth/authorize", new Authorize());
            noauth_pages.put("/oauth/access_token", new AccessToken());
            noauth_pages.put("/1/coffee/please", new CoffeePlease());
            noauth_pages.put("/auth/persona", new PersonaAuth());
            noauth_pages.put("/auth/googleplus", new GooglePlusAuth());
            Iterator<Map.Entry<String, AuthPage>> iter = oauth_pages.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, AuthPage> page = iter.next();
                if (!page.getValue().checkConfig()) {
                    Messages.printWarning("Could not load " + page.getKey() + " because of a misconfiguration.");
                    iter.remove();
                }
            }
            Iterator<Map.Entry<String, Page>> iterno = noauth_pages.entrySet().iterator();
            while (iterno.hasNext()) {
                Map.Entry<String, Page> page = iterno.next();
                if (!page.getValue().checkConfig()) {
                    Messages.printWarning("Could not load " + page.getKey() + " because of a misconfiguration.");
                    iterno.remove();
                }
            }
        }

        private Pages() {
        }
    }

    public static void initPages() {
        Pages.init();
    }

    public static Page getNoAuthPage(final String uri) {
        return Pages.noauth_pages.get(uri);
    }

    public static AuthPage getAuthPage(final String uri) {
        return Pages.oauth_pages.get(uri);
    }

    public final static class ContentTypes {
        public static final String JSON_TYPE = "application/json";
        public static final String PLAIN_TEXT = "plain/text";
        public static final String PLAIN_HTML = "text/html";
    }

    /**
     * Abstract method for get requests
     *
     * @param request  The netty HttpRequest object for this request
     * @param database A database from the database pool for you to use
     * @return
     * @throws Exception
     */
    public abstract FullHttpResponse onGetRequest(final HttpRequest request
            , final Database database) throws Exception;

    /**
     * Abstract method for post requests
     *
     * @param request            The netty HttpRequest object for this request
     * @param postRequestDecoder A postRequestDecoder to have direct access to all the posted fields
     * @param database           A database for you to use
     * @return
     * @throws Exception
     */
    public abstract FullHttpResponse onPostRequest(final HttpRequest request
            , final HttpPostRequestDecoder postRequestDecoder
            , final Database database) throws Exception;

    /**
     * @return Specify the Content-Type header to use in the http response, application/json by default
     */
    public String GetContentType() {
        return ContentTypes.JSON_TYPE;
    }

    /**
     * Override this to do any config checks
     *
     * @return true if you want to still use this page after your config checks, false will unload it
     */
    public boolean checkConfig() {
        return true;
    }

    /**
     * @param runnable This runnable will be executed sometime in the near future after we already replied to the http
     *                 request
     */
    protected void postProcess(final Runnable runnable) {
        if (runnable != null)
            EXECUTOR_SERVICE.submit(runnable);
    }

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
}
