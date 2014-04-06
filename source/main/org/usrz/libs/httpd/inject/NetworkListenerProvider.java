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

import static org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_HOST;
import static org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_PORT;
import static org.usrz.libs.utils.Check.notNull;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.inject.Optional;
import org.usrz.libs.logging.Log;

@Singleton
public class NetworkListenerProvider implements Provider<NetworkListener> {

    private final Log log = new Log();

    private final Configurations configurations;
    private final String name;
    private final String host;
    private final int port;

    private SSLContext context;
    private HttpServer server;

    public NetworkListenerProvider(Configurations configurations) {
        this.configurations = notNull(configurations, "Null configurations");

        host = configurations.get("host", DEFAULT_NETWORK_HOST);
        port = configurations.get("port", DEFAULT_NETWORK_PORT);

        final String name = configurations.getString("name");
        this.name = name == null ? host + ":" + port : name;
    }

    public String getName() {
        return name;
    }

    @Inject
    private void setHttpServer(HttpServer server) {
        this.server = server;
    }

    @Inject @Optional
    private void setSSLContext(SSLContext context) {
        this.context = context;
    }

    @Override
    public NetworkListener get() {

        final NetworkListener listener = new NetworkListener(name, host, port);
        listener.setUriEncoding("UTF-8");

        final boolean secure = configurations.get("secure", false);
        if (secure) {

            final boolean wantClientAuth = configurations.get("wantClientAuth", false);
            final boolean needClientAuth = configurations.get("needClientAuth", false);

            final SSLContext sslContext = notNull(context, "SSL context not bound");
            final SSLEngineConfigurator sslConfigurator = new SSLEngineConfigurator(sslContext);

            sslConfigurator.setClientMode(false);
            sslConfigurator.setWantClientAuth(wantClientAuth);
            sslConfigurator.setNeedClientAuth(needClientAuth);

            listener.setSSLEngineConfig(sslConfigurator);
            listener.setSecure(true);
        }

        final HttpServer server = notNull(this.server, "HTTP server not bound");
        final String name = server.getServerConfiguration().getName();
        server.addListener(listener);

        log.info("Added listener \"%s\" bound to %s:%d (secure=%b) to server \"%s\"", name, host, port, secure, name);
        return listener;
    }


}
