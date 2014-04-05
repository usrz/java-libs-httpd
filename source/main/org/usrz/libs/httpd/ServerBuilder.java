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

import static org.usrz.libs.httpd.inject.HttpHandlerProvider.handlerPath;
import static org.usrz.libs.utils.Check.notNull;

import java.io.File;

import javax.inject.Named;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.accesslog.AccessLogProbe;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.AccessLogProvider;
import org.usrz.libs.httpd.inject.FileHttpHandlerProvider;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.httpd.inject.HttpHandlerProvider;
import org.usrz.libs.httpd.inject.HttpServerProvider;
import org.usrz.libs.httpd.inject.NetworkListenerProvider;
import org.usrz.libs.httpd.rest.ObjectMapperProvider;
import org.usrz.libs.inject.Binder;
import org.usrz.libs.inject.TypeLiteral;
import org.usrz.libs.inject.utils.Names;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerBuilder {

    private final Binder binder;

    protected ServerBuilder(Binder binder) {
        this.binder = notNull(binder, "Null binder").isolate();
        binder.bind(HttpServer.class).toProvider(HttpServerProvider.class);
        binder.expose(HttpServer.class);
    }

    /* ====================================================================== */

    public void configure(Configurations configurations) {

        /* Bind our configs in this isolate */
        binder.configure(configurations);

        /* Add our access log if configured */
        final Configurations accessLog = configurations.strip("accessLog");
        if (! accessLog.isEmpty()) addAccessLog(accessLog);

        /* Add our singleton listener configured */
        final Configurations listener = configurations.strip("listener");
        if (! listener.isEmpty()) addListener(listener);

        /* Add any other listener grouped by "listeners" */
        configurations.group("listeners").values().forEach((configuration) ->
            addListener(configuration));

        /* And finally remember our JSON configurations */
        final Configurations json = configurations.strip("json");
        if (! json.isEmpty()) withObjectMapperDefaults(json);
    }

    /* ====================================================================== */

    public void withObjectMapperDefaults(Configurations configurations) {
        binder.bind(ObjectMapper.class).toProvider(new ObjectMapperProvider(configurations));
    }

    /* ====================================================================== */

    public void withErrorPageGenerator(ErrorPageGenerator generator) {
        binder.bind(ErrorPageGenerator.class).toInstance(generator);
    }

    /* ====================================================================== */

    public void addAccessLog(Configurations configurations) {
        binder.bind(AccessLogProbe.class)
                   .with(Names.unique())
                   .toProvider(new AccessLogProvider(configurations));
    }

    /* ====================================================================== */

    public void addListener(Configurations configurations) {
        final Named unique = Names.unique();
        binder.bind(NetworkListener.class)
              .with(unique)
              .toProvider(new NetworkListenerProvider(configurations, unique))
              .withEagerInjection();
    }

    /* ====================================================================== */

    private void addHandler(HttpHandlerPath path, HttpHandlerProvider provider) {
        binder.bind(HttpHandler.class)
              .with(path)
              .toProvider(provider)
              .withEagerInjection();
    }

    /* ---------------------------------------------------------------------- */

    public void addHandler(String path, TypeLiteral<? extends HttpHandler> handler) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new HttpHandlerProvider(handler, at));
    }

    public void addHandler(String path, HttpHandler handler) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new HttpHandlerProvider(handler, at));
    }

    public void addHandler(String path, Class<? extends HttpHandler> handler) {
        this.addHandler(path, TypeLiteral.of(handler));
    }

    /* ---------------------------------------------------------------------- */

    public void serveFiles(String path, File documentRoot) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new FileHttpHandlerProvider(documentRoot, at));
    }

    public void serveFiles(String path, String documentRoot) {
        this.serveFiles(path, new File(documentRoot));
    }

}
