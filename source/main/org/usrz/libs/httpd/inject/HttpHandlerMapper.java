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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.Set;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.usrz.libs.logging.Log;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class HttpHandlerMapper {

    private static final TypeLiteral<HttpHandler> HTTP_HANDLER_TYPE_LITERAL = TypeLiteral.get(HttpHandler.class);
    private static final Log log = new Log();

    private final Injector injector;

    @Inject
    protected HttpHandlerMapper(Injector injector) {
        this.injector = injector;
    }

    public ServerConfiguration map(ServerConfiguration configuration) {
        final Set<Key<?>> keys = injector.getAllBindings().keySet();
        for (final Key<?> key: keys) {
            if (HTTP_HANDLER_TYPE_LITERAL.equals(key.getTypeLiteral())) {
                final Annotation annotation = key.getAnnotation();
                final String path = annotation instanceof At ? ((At)annotation).path() : "/";
                final HttpHandler handler = (HttpHandler) injector.getInstance(key);
                configuration.addHttpHandler(handler, path);
                log.debug("Mapped handler \"%s\" under context path \"%s\"", handler.getClass(), path);
            }
        }
        return configuration;
    }

    public static final At at(String path) {
        return new AtImpl(path);
    }

    /* ====================================================================== */
    /* MARKER ANNOTATION AND IMPLEMENTATION                                   */
    /* ====================================================================== */

    /**
     * {@link BindingAnnotation} to associate with {@link HttpHandler} instances
     * in order to mark the deployment path.
     *
     * @see HttpHandlerMapper#at(String)
     * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
     */
    @Retention(RUNTIME)
    @BindingAnnotation
    public static @interface At {

        /**
         * The context path where the associated {@link HttpHandler} will be
         * deployed under.
         */
        public String path();
    }

    /* ====================================================================== */

    @SuppressWarnings("all")
    private static class AtImpl implements At {

        private final String path;

        private AtImpl(String path) {
            if (path == null) throw new NullPointerException("Null path");
            path = ("/" + path).replaceAll("/+", "/");
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (path.isEmpty()) path = "/";
            this.path = path;
        }

        @Override
        public String path() {
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
                return path.equals(((At)object).path());
            } catch (ClassCastException exception) {
                return false;
            }
        }

        @Override
        public String toString() {
            return "@" + At.class.getName() + "(value=" + path + ")";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return At.class;
        }
    }
}
