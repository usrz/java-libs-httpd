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
package org.usrz.libs.httpd;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.usrz.libs.httpd.Handlers.At;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;

public class HttpHandlersProvider implements Provider<Map<String, HttpHandler>> {

    private static final TypeLiteral<HttpHandler> HTTP_HANDLER_TYPE_LITERAL = TypeLiteral.get(HttpHandler.class);
    private static final Log log = new Log();

    private final Injector injector;

    @Inject
    protected HttpHandlersProvider(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Map<String, HttpHandler> get() {
        final Map<String, HttpHandler> handlers = new HashMap<>();
        final Set<Key<?>> keys = injector.getAllBindings().keySet();
        for (final Key<?> key: keys) {
            if (HTTP_HANDLER_TYPE_LITERAL.equals(key.getTypeLiteral())) {
                final Annotation annotation = key.getAnnotation();
                final String path = annotation instanceof At ? ((At)annotation).path() : "/*";
                final HttpHandler handler = (HttpHandler) injector.getInstance(key);
                if (handlers.containsKey(path))
                    throw new ProvisionException("Multiple applications mapped to " + path);
                handlers.put(path, handler);
                log.debug("Mapped handler \"%s[%s]\" under context path \"%s\"", handler.getClass().getName(), handler.getName(), path);
            }
        }
        return handlers;
    }

}
