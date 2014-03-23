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
import java.util.Collection;
import java.util.Map;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.usrz.libs.utils.configurations.Configurations;
import org.usrz.libs.utils.configurations.ConfigurationsModule;
import org.usrz.libs.utils.inject.BindingModuleSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

public abstract class ServerModule extends BindingModuleSupport {

    private final Configurations configurations;
    private Configurations jsonConfigurations = Configurations.EMPTY_CONFIGURATIONS;

    protected ServerModule(Configurations configurations) {
        if (configurations == null) throw new NullPointerException("Null configurations");
        this.configurations = configurations;
    }

    @Override
    public final void configure(Binder binder) {
        /* Configuration class is "HttpServerProvider" */
        binder.install(new ConfigurationsModule() {
            @Override
            public void configure() {
                this.configure(HttpServerProvider.class).with(configurations);
            }});

        /* Bind our handlers and listeners providers and our server instance */
        binder.bind(new TypeLiteral<Map<String, HttpHandler>>(){}).toProvider(HttpHandlersProvider.class);
        binder.bind(new TypeLiteral<Collection<NetworkListener>>(){}).toProvider(NetworkListenersProvider.class);
        binder.bind(HttpServer.class).toProvider(HttpServerProvider.class);

        /* Off configuring the rest */
        super.configure(binder);

        /* Finally bind our JSON configurations */
        binder.install(new ConfigurationsModule() {
            @Override
            public void configure() {
                this.configure(ObjectMapper.class).with(jsonConfigurations);
            }
        });
    }

    public final ServerModule withJSONConfigurations(Configurations configurations) {
        jsonConfigurations = configurations;
        return this;
    }

    public final HttpHandlerBuilder serve(String path) {
        return new HttpHandlerBuilder(Handlers.at(path));
    }

    /* ====================================================================== */

    public class HttpHandlerBuilder {

        private final Annotation annotation;

        private HttpHandlerBuilder(Annotation annotation) {
            if (annotation == null) throw new NullPointerException("Null annotation");
            this.annotation = annotation;
        }

        public void with(HttpHandler handler) {
            binder().bind(HttpHandler.class).annotatedWith(annotation).toInstance(handler);
        }

        public void withHandler(Class<? extends HttpHandler> handler) {
            binder().bind(HttpHandler.class).annotatedWith(annotation).to(handler);
        }

        public void with(Provider<HttpHandler> provider) {
            binder().bind(HttpHandler.class).annotatedWith(annotation).toProvider(provider);
        }

        public void with(Class<? extends Provider<? extends HttpHandler>> provider) {
            binder().bind(HttpHandler.class).annotatedWith(annotation).toProvider(provider);
        }

    }

}
