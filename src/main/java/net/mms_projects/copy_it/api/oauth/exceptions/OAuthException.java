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

package net.mms_projects.copy_it.api.oauth.exceptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OAuthException extends Exception {
    private static final String ERROR = "error";
    private static final String ERROR_MESSAGES = "error_messages";

    public OAuthException(String message) {
        super();
        errors = new ArrayList<String>();
        errors.add(message);
    }

    public void addError(String message) {
        errors.add(message);
    }

    public String getMessage() {
        final StringBuilder output = new StringBuilder();
        for (int i = 0; i < errors.size(); i++)
            output.append(errors.get(i) + "\n");
        return output.toString();
    }

    public String toString() {
        return toJSON().toString();
    }

    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        try {
            json.put(ERROR, true);
            json.put(ERROR_MESSAGES, errors);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private final List<String> errors;
}
