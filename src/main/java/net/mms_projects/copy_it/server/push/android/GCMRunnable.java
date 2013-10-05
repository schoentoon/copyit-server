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

package net.mms_projects.copy_it.server.push.android;

import net.mms_projects.copy_it.api.http.Page;
import net.mms_projects.copy_it.server.config.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class GCMRunnable implements Runnable {
    private static final String REGISTRATION_IDS = "registration_ids";
    private static final String DATA = "data";

    public GCMRunnable() {
        full = new JSONObject();
        data = new JSONObject();
        ids = new JSONArray();
        try {
            full.put(DATA, data);
            full.put(REGISTRATION_IDS, ids);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addRegistrationId(final String id) {
        ids.put(id);
    }

    private static final String DRY_RUN = "dry_run";

    public void setDryRun() {
        try {
            full.put(DRY_RUN, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setData(final String key, final int integer) {
        try {
            data.put(key, integer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setData(final String key, final long ln) {
        try {
            data.put(key, ln);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setData(final String key, final boolean bool) {
        try {
            data.put(key, bool);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setData(final String key, final String string) {
        try {
            data.put(key, string);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setData(final String key, final double dbl) {
        try {
            data.put(key, dbl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static final String GCM_URL = "https://android.googleapis.com/gcm/send";
    private static final String POST = "POST";
    private static final String KEY = "key=" + Config.getStringSafe(Config.Keys.GCM_TOKEN, null);

    public void run() {
        try {
            URL url = new URL(GCM_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod(POST);
            conn.setRequestProperty(CONTENT_TYPE, Page.ContentTypes.JSON_TYPE);
            conn.setRequestProperty(AUTHORIZATION, KEY);
            final String output_json = full.toString();
            System.err.println("Input json: " + output_json);
            conn.setRequestProperty(CONTENT_LENGTH, String.valueOf(output_json.length()));
            conn.setDoOutput(true);
            conn.setDoInput(true);
            DataOutputStream outputstream = new DataOutputStream(conn.getOutputStream());
            outputstream.writeBytes(output_json);
            outputstream.close();
            DataInputStream input = new DataInputStream(conn.getInputStream());
            StringBuilder builder = new StringBuilder(input.available());
            for(int c = input.read(); c != -1; c = input.read())
                builder.append((char) c);
            input.close();
            output = new JSONObject(builder.toString());
            System.err.println("Output json: " + output.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject send() {
        run();
        return output;
    }

    private final JSONObject data;
    private final JSONObject full;
    private final JSONArray ids;
    private JSONObject output;
}
