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

import static org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_HOST;
import static org.glassfish.grizzly.http.server.NetworkListener.DEFAULT_NETWORK_PORT;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.configurations.Configuration;
import org.usrz.libs.utils.configurations.Configurations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;

@Singleton
@SuppressWarnings("restriction")
public class NetworkListenersProvider implements Provider<Collection<NetworkListener>> {

    private static final Log log = new Log();
    private final Map<String, Configurations> listenersConfigurations = new HashMap<>();
    private SSLContext sslContext = null;

    @Inject
    protected NetworkListenersProvider(@Configuration(HttpServerProvider.class) Configurations configurations) {
        if (configurations == null) throw new NullPointerException("Null configurations");

        /* We might have mappings like "listeners.[name].[key] = ..." */
        listenersConfigurations.putAll(configurations.group("listeners"));

        /* We might have a single mapping "listeners.[key] = ..." */
        final Configurations listenerConfiguration = configurations.strip("listener");
        if (!listenerConfiguration.isEmpty()) {
            listenersConfigurations.put("!default", listenerConfiguration);
        }
    }


    @Inject(optional = true)
    private void setSSLContextFactory(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public Collection<NetworkListener> get() {
        final List<NetworkListener> listeners = new ArrayList<>();

        for (final Entry<String, Configurations> entry: listenersConfigurations.entrySet()) {
            listeners.add(create(entry.getKey(), entry.getValue()));
        }

        return listeners;
    }

    /* ====================================================================== */

    private NetworkListener create(String name, Configurations configurations) {
        final String  listenerName =   configurations.get("name", name);
        final String  host =           configurations.get("host", DEFAULT_NETWORK_HOST);
        final int     port =           configurations.get("port", DEFAULT_NETWORK_PORT);
        final boolean secure =         configurations.get("secure", false);
        final boolean wantClientAuth = configurations.get("wantClientAuth", false);
        final boolean needClientAuth = configurations.get("needClientAuth", false);

        final NetworkListener listener = new NetworkListener(listenerName, host, port);
        listener.setUriEncoding("UTF-8");

        if (secure) try {
            if (sslContext == null) sslContext = SSLContext.getDefault();
            final SSLEngineConfigurator sslConfigurator = new SSLEngineConfigurator(sslContext);
            sslConfigurator.setClientMode(false);
            sslConfigurator.setWantClientAuth(wantClientAuth);
            sslConfigurator.setNeedClientAuth(needClientAuth);
            listener.setSSLEngineConfig(sslConfigurator);
            listener.setSecure(true);
        } catch (NoSuchAlgorithmException exception) {
            throw new ProvisionException("Unable to create default SSL context", exception);
        }

        log.debug("Created \"%s\" listener bound to %s on port %d (secure=%b)", listenerName, host, port, secure);
        return listener;
    }


}
