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
package org.usrz.libs.httpd.inject;

import static org.usrz.libs.utils.Check.notNull;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.Key;

public class HttpHandlerProvider implements Provider<HttpHandler> {

    private final Log log = new Log();
    private final Key<? extends HttpHandler> key;
    private final HttpHandler handler;
    private final String path;


    public HttpHandlerProvider(Key<? extends HttpHandler> key, HttpHandlerPath path) {
        this.path = notNull(path, "Null path").value();
        this.key = notNull(key, "Null handler key");
        handler = null;
    }

    public HttpHandlerProvider(HttpHandler handler, HttpHandlerPath path) {
        this.handler = notNull(handler, "Null handler");
        this.path = notNull(path, "Null path").value();
        key = null;
    }

    @Inject
    private void setup(Injector injector, HttpServer server) {
        final HttpHandler httpHandler;
        if (handler == null) {
            httpHandler = injector.getInstance(key);
        } else {
            injector.injectMembers(handler);
            httpHandler = handler;
        }

        server.getServerConfiguration().addHttpHandler(httpHandler, path);
        log.info("Serving \"%s\" using handler %s", path, httpHandler.getClass().getName());
    }

    @Override
    public HttpHandler get() {
        return handler;
    }

    /* ====================================================================== */

    public static final HttpHandlerPath handlerPath(String path) {
        return new HttpHandlerPathImpl(notNull(path, "Null path"));
    }

    /* ---------------------------------------------------------------------- */

    @SuppressWarnings("all")
    private static class HttpHandlerPathImpl implements HttpHandlerPath {

        private final String path;

        private HttpHandlerPathImpl(String path) {
            path = ("/" + path).replaceAll("/+", "/");
            path += path.endsWith("/") ? "*" : "/*";
            this.path = path;
        }

        @Override
        public String value() {
            return path;
        }

        @Override
        public int hashCode() {
            // This is specified in java.lang.Annotation.
            return (127 * "value".hashCode()) ^ path.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            try {
                return path.equals(((HttpHandlerPath)object).value());
            } catch (ClassCastException exception) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "@" + HttpHandlerPath.class.getName() + "(value=" + path + ")";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return HttpHandlerPath.class;
        }
    }
}
