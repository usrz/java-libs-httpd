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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.crypto.pem.PEMProvider;
import org.usrz.libs.logging.Log;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;

@Singleton
public class NetworkListenerProvider implements Provider<NetworkListener> {

    private static final Key<CallbackHandler> CBH_KEY = Key.get(CallbackHandler.class);

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
            final File keyStoreFile = configurations.getFile("keystore_file");
            if (keyStoreFile == null) {

                /* If we don't have a keystore file, we MUST have a SSL context */
                sslContext = injector.getInstance(SSLContext.class);

            } else try {

                /* What kind of keystore are we using here? Add PEM if needed! */
                final String keyStoreType = configurations.getString("keystore_type", "PEM");
                if ("PEM".equalsIgnoreCase(keyStoreType)) Security.addProvider(new PEMProvider());

                /* Let's start trying to get a password here */
                final String keyStorePassword = configurations.getString("keystore_password");
                final char keyStoreSecret[];

                /* How to decrypt our key store */
                if (keyStorePassword != null) {

                    /* If we have a password, use it as our secret */
                    keyStoreSecret = keyStorePassword.toCharArray();

                } else if (injector.getAllBindings().containsKey(CBH_KEY)) {

                    /* If we can find a call back handler, ask for a password */
                    final CallbackHandler callbackHandler = injector.getInstance(CBH_KEY);
                    final PasswordCallback callback = new PasswordCallback("Enter password for key store " + keyStoreFile, false);
                    callbackHandler.handle(new Callback[] { callback });
                    keyStoreSecret = callback.getPassword();
                    callback.clearPassword();

                } else {

                    /* There is no password, and no call back handler... Null it */
                    keyStoreSecret = null;

                }

                final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(new FileInputStream(keyStoreFile), keyStoreSecret);

                final Enumeration<String> e = keyStore.aliases();
                while (e.hasMoreElements()) System.err.println("Found " + e.nextElement());

                final KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyFactory.init(keyStore, keyStoreSecret);

                final TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(keyStore);

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyFactory.getKeyManagers(), null, null); //trustFactory.getTrustManagers(), null);

            } catch (UnsupportedCallbackException exception) {
                throw new ProvisionException("Callback unable to get password for keystore " + keyStoreFile, exception);
            } catch (GeneralSecurityException exception) {
                throw new ProvisionException("Security error reading from keystore " + keyStoreFile, exception);
            } catch (IOException exception) {
                throw new ProvisionException("I/O error reading from keystore " + keyStoreFile, exception);
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
