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

import javax.net.ssl.SSLContext;

import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.usrz.libs.httpd.configurations.Configurations;
import org.usrz.libs.logging.Log;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class NetworkListenerFactory {

    private static final Log log = new Log();

    private final Injector injector;

    @Inject
    protected NetworkListenerFactory(Injector injector) {
        this.injector = injector;
    }

    public NetworkListener create(String name, Configurations configurations) {
        final String  listenerName =   configurations.get("name", name);
        final String  host =           configurations.get("host", DEFAULT_NETWORK_HOST);
        final int     port =           configurations.get("port", DEFAULT_NETWORK_PORT);
        final boolean secure =         configurations.get("secure", false);
        final boolean wantClientAuth = configurations.get("wantClientAuth", false);
        final boolean needClientAuth = configurations.get("needClientAuth", false);

        final NetworkListener listener = new NetworkListener(listenerName, host, port);
        listener.setUriEncoding("UTF-8");

        if (secure) {
            final SSLContext sslContext = injector.getInstance(SSLContext.class);
            final SSLEngineConfigurator sslConfigurator = new SSLEngineConfigurator(sslContext);
            sslConfigurator.setClientMode(false);
            sslConfigurator.setWantClientAuth(wantClientAuth);
            sslConfigurator.setNeedClientAuth(needClientAuth);
            listener.setSSLEngineConfig(sslConfigurator);
            listener.setSecure(true);
        }

        log.debug("Created \"%s\" listener bound to %s on port %d (secure=%b)", listenerName, host, port, secure);
        return listener;
    }

}
