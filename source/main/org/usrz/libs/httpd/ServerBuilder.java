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

import static org.usrz.libs.httpd.ServerBuilder.RedirectConfigurator.Flags.MATCH_ENTIRE_LINE;
import static org.usrz.libs.httpd.ServerBuilder.RedirectConfigurator.Flags.PERMANENT_REDIRECT;
import static org.usrz.libs.httpd.ServerBuilder.RedirectConfigurator.Flags.PRESERVE_QUERY_STRING;
import static org.usrz.libs.httpd.inject.HttpHandlerProvider.handlerPath;
import static org.usrz.libs.utils.Check.notNull;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Provider;
import javax.ws.rs.core.Application;

import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.accesslog.AccessLogProbe;
import org.glassfish.jersey.server.ResourceConfig;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.httpd.inject.AccessLogProvider;
import org.usrz.libs.httpd.inject.DefaultEPGProvider;
import org.usrz.libs.httpd.inject.FileHandlerProvider;
import org.usrz.libs.httpd.inject.HttpHandlerPath;
import org.usrz.libs.httpd.inject.HttpHandlerProvider;
import org.usrz.libs.httpd.inject.HttpServerProvider;
import org.usrz.libs.httpd.inject.NetworkListenerProvider;
import org.usrz.libs.httpd.inject.RedirectHandler;
import org.usrz.libs.httpd.rest.ObjectMapperProvider;
import org.usrz.libs.httpd.rest.RestHandlerProvider;
import org.usrz.libs.utils.inject.Injections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class ServerBuilder {

    private final Annotation unique = Injections.unique();
    private final Binder binder;

    protected ServerBuilder(Binder binder) {
        this.binder = notNull(binder, "Null binder");
        binder.skipSources(this.getClass());

        /* Add the HttpServer in the child isolate as it might needs configs */
        this.binder.bind(HttpServer.class).toProvider(new HttpServerProvider().with(unique));
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

        /* Bind the configurations for the server */
        binder.bind(Configurations.class).annotatedWith(unique).toInstance(configurations);

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
        binder.bind(ErrorPageGenerator.class).toProvider(new DefaultEPGProvider(generator));
    }

    public void withErrorPageGenerator(Class<? extends ErrorPageGenerator> generator) {
        binder.bind(ErrorPageGenerator.class).toProvider(new DefaultEPGProvider(Key.get(generator)));
    }

    public void withErrorPageGenerator(TypeLiteral<? extends ErrorPageGenerator> generator) {
        binder.bind(ErrorPageGenerator.class).toProvider(new DefaultEPGProvider(Key.get(generator)));
    }

    public void withErrorPageGenerator(Key<? extends ErrorPageGenerator> generator) {
        binder.bind(ErrorPageGenerator.class).toProvider(new DefaultEPGProvider(generator));
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

    public void addHandler(String path, HttpHandler handler) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new HttpHandlerProvider(handler, at));
    }

    public void addHandler(String path, Class<? extends HttpHandler> handler) {
        this.addHandler(path, Key.get(handler));
    }

    public void addHandler(String path, TypeLiteral<? extends HttpHandler> handler) {
        this.addHandler(path, Key.get(handler));
    }

    public void addHandler(String path, Key<? extends HttpHandler> handler) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new HttpHandlerProvider(handler, at));
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

    public RestConfigurator serveApp(String path, Application application) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new RestHandlerProvider(application, at));
        return new RestConfigurator(at);
    }

    public RestConfigurator serveApp(String path, Consumer<ResourceConfig> consumer) {
        final HttpHandlerPath at = handlerPath(path);
        this.addHandler(at, new RestHandlerProvider(consumer, at));
        return new RestConfigurator(at);
    }

    /* ---------------------------------------------------------------------- */

    public RedirectConfigurator serveRedirects(String path) {
        final HttpHandlerPath at = handlerPath(path);
        final RedirectHandler handler = new RedirectHandler(at);
        this.addHandler(at, new HttpHandlerProvider(handler, at));
        return new RedirectConfigurator(handler);
    }

    /* ---------------------------------------------------------------------- */

    public final class RestConfigurator {

        private final HttpHandlerPath at;

        private RestConfigurator(HttpHandlerPath at) {
            this.at = at;
        }

        public RestConfigurator withAppConfigurations(Configurations configurations) {
            binder.bind(Configurations.class)
                  .annotatedWith(at)
                  .toInstance(notNull(configurations, "Null application configurations"));
            return this;
        }

        public RestConfigurator withObjectMapperConfigurations(Configurations configurations) {
            binder.bind(ObjectMapper.class)
                  .annotatedWith(at)
                  .toProvider(new ObjectMapperProvider(configurations));
            return this;
        }

        public RestConfigurator withObjectMapper(ObjectMapper mapper) {
            binder.bind(ObjectMapper.class)
                  .annotatedWith(at)
                  .toInstance(notNull(mapper, "Null object mapper"));
            return this;
        }
    }

    /* ---------------------------------------------------------------------- */

    public static final class RedirectConfigurator {

        public enum Flags {
            PRESERVE_QUERY_STRING,
            MATCH_ENTIRE_LINE,
            PERMANENT_REDIRECT
        }

        private final RedirectHandler handler;

        private RedirectConfigurator(RedirectHandler handler) {
            this.handler = handler;
        }

        public RedirectConfigurator redirect(String location, String redirect) {
            return this.redirect(location, redirect, PRESERVE_QUERY_STRING, MATCH_ENTIRE_LINE);
        }

        public RedirectConfigurator redirect(String location, String redirect, Flags flag, Flags... flags) {
            final Set<Flags> set = flag == null ? Collections.emptySet() : EnumSet.of(flag, flags);
            boolean preserveQueryString = set.contains(PRESERVE_QUERY_STRING);
            boolean permanentRedirect = set.contains(PERMANENT_REDIRECT);
            boolean matchEntireLine = set.contains(MATCH_ENTIRE_LINE);
            handler.addRedirect(location, redirect, matchEntireLine, permanentRedirect, preserveQueryString);
            return this;
        }
    }
}
