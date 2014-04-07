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
import java.util.function.Consumer;

import javax.inject.Provider;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.accesslog.AccessLogProbe;
import org.glassfish.jersey.server.ResourceConfig;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.AccessLogProvider;
import org.usrz.libs.httpd.inject.FileHandlerProvider;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.httpd.inject.HttpHandlerProvider;
import org.usrz.libs.httpd.inject.HttpServerProvider;
import org.usrz.libs.httpd.inject.NetworkListenerProvider;
import org.usrz.libs.httpd.rest.ObjectMapperProvider;
import org.usrz.libs.httpd.rest.RestHandlerProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class ServerBuilder {

    private final Binder binder;

    protected ServerBuilder(Binder binder) {
        this.binder = notNull(binder, "Null binder");

        /* Add the HttpServer in the child isolate as it might needs configs */
        this.binder.bind(HttpServer.class).toProvider(HttpServerProvider.class);
    }

    /* ====================================================================== */

    public void install(Consumer<Binder> consumer) {
        consumer.accept(binder);
    }

    public void install(Module... modules) {
        for (Module module: modules) binder.install(module);
    }

    /* ---------------------------------------------------------------------- */

    public void configure(Configurations configurations) {

        /* Add our access log if configured */
        final Configurations accessLog = configurations.strip("access_log");
        if (! accessLog.isEmpty()) addAccessLog(accessLog);

        /* Add our singleton listener configured */
        final Configurations listener = configurations.strip("listener");
        if (! listener.isEmpty()) addListener(listener);

        /* Add any other listener grouped by "listeners" */
        configurations.group("listeners").values().forEach((configuration) ->
            addListener(configuration));

        /* Any document root? */
        final File documentRoot = configurations.getFile("document_root");
        if (documentRoot != null) this.serveFiles("/", documentRoot);

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

    public void withErrorPageGenerator(Class<? extends ErrorPageGenerator> generator) {
        binder.bind(ErrorPageGenerator.class).to(generator);
    }

    public void withErrorPageGenerator(TypeLiteral<? extends ErrorPageGenerator> generator) {
        binder.bind(ErrorPageGenerator.class).to(generator);
    }

    /* ====================================================================== */

    public void addAccessLog(Configurations configurations) {
        final AccessLogProvider provider = new AccessLogProvider(configurations);
        binder.bind(AccessLogProbe.class)
              .annotatedWith(Names.named(provider.getName()))
              .toProvider(provider)
              .asEagerSingleton();
    }

    /* ====================================================================== */

    public void addListener(Configurations configurations) {
        final NetworkListenerProvider provider = new NetworkListenerProvider(configurations);
        binder.bind(NetworkListener.class)
              .annotatedWith(Names.named(provider.getName()))
              .toProvider(provider)
              .asEagerSingleton();
    }

    /* ====================================================================== */

    private void addHandler(HttpHandlerPath path, Provider<HttpHandler> provider) {
        binder.bind(HttpHandler.class)
              .annotatedWith(path)
              .toProvider(provider)
              .asEagerSingleton();
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
        this.addHandler(path, TypeLiteral.get(handler));
    }

    /* ---------------------------------------------------------------------- */

    public void serveFiles(String path, File documentRoot) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new FileHandlerProvider(documentRoot, at));
    }

    public void serveFiles(String path, String documentRoot) {
        this.serveFiles(path, new File(documentRoot));
    }

    /* ---------------------------------------------------------------------- */

    public void serveRest(String path, Consumer<ResourceConfig> consumer) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new RestHandlerProvider(consumer, at));
    }

    public void serveRest(String path, Configurations json, Consumer<ResourceConfig> consumer) {
        final HttpHandlerPath at = handlerPath(path);
        final RestHandlerProvider provider = new RestHandlerProvider(consumer, at);

        binder.bind(ObjectMapper.class).annotatedWith(at).toProvider(new ObjectMapperProvider(json));
        binder.bind(HttpHandler.class).annotatedWith(at).toProvider(provider).asEagerSingleton();
    }

}
