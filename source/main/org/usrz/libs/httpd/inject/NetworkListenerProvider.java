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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.crypto.utils.KeyStoreBuilder;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;

@Singleton
public class NetworkListenerProvider implements Provider<NetworkListener> {

    private final Log log = new Log();

    private final Configurations configurations;
    private final String name;
    private final String host;
    private final int port;

    private NetworkListener listener;

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
    private void setup(Injector injector, HttpServer server) {

        /* Start by creating our listener */
        listener = new NetworkListener(name, host, port);
        listener.setUriEncoding("UTF-8");

        final boolean secure = configurations.get("secure", false);
        if (secure) {

            final boolean wantClientAuth = configurations.get("want_client_auth", false);
            final boolean needClientAuth = configurations.get("need_client_auth", false);

            final SSLContext sslContext;
            final Configurations keyStoreConfig = configurations.strip("keystore");

            if (keyStoreConfig.isEmpty()) {

                /* If we don't have a keystore configuration, we MUST have a SSL context */
                sslContext = injector.getInstance(SSLContext.class);

            } else try {

                /* Start building a KeyStore */
                final KeyStore keyStore = new KeyStoreBuilder().withConfiguration(keyStoreConfig).build();

                /* We can't use a callback handler (for key store or key passwords), as KeyManagerFactory wants a password */
                final String password = keyStoreConfig.getString("key_password", keyStoreConfig.getString("password", null));

                /* Build up our key manager factory */
                final KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyFactory.init(keyStore, password == null ? null : password.toCharArray());

                /* Build up our trust manager factory */
                final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(keyStore);

                /* Initialize the SSL context */
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyFactory.getKeyManagers(), null, null);

            } catch (GeneralSecurityException exception) {
                throw new ProvisionException("Security error reading from keystore " + keyStoreConfig.get("file", "(no file specified)"), exception);
            } catch (IOException exception) {
                throw new ProvisionException("I/O error reading from keystore " + keyStoreConfig.get("file", "(no file specified)"), exception);
            } catch (Exception exception) {
                throw new ProvisionException("Error provisioning keystore " + keyStoreConfig.get("file", "(no file specified)"), exception);
            }

            final SSLEngineConfigurator sslConfigurator = new SSLEngineConfigurator(sslContext);

            sslConfigurator.setClientMode(false);
            sslConfigurator.setWantClientAuth(wantClientAuth);
            sslConfigurator.setNeedClientAuth(needClientAuth);

            listener.setSSLEngineConfig(sslConfigurator);

            listener.setSecure(true);
        }

        final String name = server.getServerConfiguration().getName();
        server.addListener(listener);

        log.info("Added listener \"%s\" bound to %s:%d (secure=%b) to server \"%s\"", name, host, port, secure, name);
    }

    @Override
    public NetworkListener get() {
        return listener;
    }


}
