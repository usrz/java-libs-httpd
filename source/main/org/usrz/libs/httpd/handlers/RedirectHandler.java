/* ========================================================================== *
 * Copyright 2014 USRZ.com and Pier Paolo Fumagalli                           *
 * -------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *  http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * ========================================================================== */
package org.usrz.libs.httpd.handlers;

import static org.glassfish.grizzly.http.util.HttpStatus.MOVED_PERMANENTLY_301;
import static org.glassfish.grizzly.http.util.HttpStatus.NOT_FOUND_404;
import static org.glassfish.grizzly.http.util.HttpStatus.TEMPORARY_REDIRECT_307;
import static org.usrz.libs.utils.Check.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.logging.Log;

public class RedirectHandler extends HttpHandler {

    private static final Log log = new Log();

    private final List<Redirect> redirects = new ArrayList<>();
    private final String path;

    public RedirectHandler(HttpHandlerPath path) {
        this.path = path.value().substring(0, path.value().length() - 2);
    }

    @Override
    public void service(Request request, Response response)
    throws Exception {
        final String query = request.getQueryString();
        String path = request.getPathInfo();
        if (path == null) path = "/";

        log.info("Checking path \"%s\" against %d redirects", path, redirects.size());
        for (Redirect redirect: redirects) {
            final String location = redirect.apply(path, query);
            if (location != null) {
                response.setStatus(redirect.status);
                response.setHeader(Header.Location, location);
                response.finish();
                return;
            }
        }

        response.setStatus(NOT_FOUND_404);
        response.finish();
    }

    /* ====================================================================== */

    public void addRedirect(String match, String replacement, boolean matchEntireLine, boolean permanentRedirect, boolean preserveQueryString) {

        final HttpStatus status = permanentRedirect ? MOVED_PERMANENTLY_301 : TEMPORARY_REDIRECT_307;
        if (matchEntireLine) match = "^" + match + "$";

        log.info("Redirecting \"%s\" to \"%s\" with status %d (application path \"%s\", query string %s)",
                 match, replacement, status.getStatusCode(), path, preserveQueryString ? "preserved" : "ignored");
        redirects.add(new Redirect(match, replacement, status, preserveQueryString));
    }

    private static final class Redirect {

        private final HttpStatus status;
        private final Pattern pattern;
        private final String replace;
        private final boolean qsa;

        private Redirect(String match, String replacement, HttpStatus status, boolean qsa) {
            pattern = Pattern.compile(notNull(match, "Null match string"));
            replace = notNull(replacement, "Null replacement string");
            this.status = notNull(status, "Null status");
            this.qsa = qsa;
        }

        private String apply(String path, String query) {
            RedirectHandler.log.info("Checking path \"%s\" against \"%s\"", path, pattern);
            final Matcher matcher = pattern.matcher(path);
            if (! matcher.find()) return null;

            final String location = matcher.replaceAll(replace);
            return (qsa && (query != null)) ?
                location + (location.indexOf('?') < 0 ? '?' : '&') + query :
                location;
        }
    }

}
