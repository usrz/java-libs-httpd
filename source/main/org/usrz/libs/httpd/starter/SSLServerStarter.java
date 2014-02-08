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
package org.usrz.libs.httpd.starter;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.usrz.libs.httpd.configurations.Configurations;
import org.usrz.libs.httpd.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.inject.HttpServerProvider;
import org.usrz.libs.httpd.inject.NetworkListenerFactory;
import org.usrz.libs.httpd.inject.SSLPasswordCallback;
import org.usrz.libs.httpd.inject.SelfSignedSSLContextProvider;
import org.usrz.libs.logging.Logging;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SSLServerStarter {

    static { Logging.init(); }

    @Inject
    protected SSLServerStarter(final Configurations configurations,
                               final SSLPasswordCallback callback)
    throws IOException {
        Guice.createInjector(new AbstractModule() {
            final Configurations sslStarterConfigurations = configurations.strip("sslstarter")
                    .merge(new ConfigurationsBuilder()
                        .put("name",            "ssl-starter")
                        .put("listener.name",   "main")
                        .put("listener.secure", true)
                    .build());

            @Override
            protected void configure() {
                /* Extract our sub-configurations */
                this.bind(Configurations.class).toInstance(sslStarterConfigurations);
                /* Remember our SSL password callback (we'll "setPassword" on it) */
                this.bind(SSLPasswordCallback.class).toInstance(callback);

                /* Our password application (bound to root) */
                this.bind(HttpHandler.class).to(SSLServerStarterHandler.class);

                /* We need an HTTP server to start, a network listener, and a self-signed cert */
                this.bind(SSLContext.class).toProvider(SelfSignedSSLContextProvider.class);
                this.bind(HttpServer.class).toProvider(HttpServerProvider.class);
                this.bind(NetworkListenerFactory.class);

            }

        }).getInstance(HttpServer.class).start();
    }

}
