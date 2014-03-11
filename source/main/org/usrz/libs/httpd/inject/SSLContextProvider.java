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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.ProviderException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.usrz.libs.httpd.configurations.Configurations;
import org.usrz.libs.logging.Log;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SSLContextProvider implements Provider<SSLContext> {

    private static final Log log = new Log();

    private final Configurations configurations;
    private Initializer initializer;

    @Inject
    protected SSLContextProvider(Configurations configurations) {
        this.configurations = configurations.strip("ssl");
    }

    @Inject(optional=true)
    protected void setSSLPasswordCallback(SSLPasswordCallback passwordCallback) {
        final Initializer initializer = new Initializer();
        passwordCallback.setSSLContextInitializer(initializer);
        this.initializer = initializer;
    }

    @Override
    public SSLContext get() {
        try {
            if (initializer != null) return initializer.getContext();
            throw new ProviderException("SSLPasswordCallback not bound");
        } catch (InterruptedException exception) {
            throw new ProviderException("Interrupted waiting for context", exception);
        }
    }

    private final class Initializer implements SSLContextInitializer {

        private final Semaphore semaphore = new Semaphore(-1);
        private final AtomicReference<SSLContext> sslContext = new AtomicReference<>();

        private final SSLContext getContext()
        throws InterruptedException {
            SSLContext context = null;
            while ((context = sslContext.get()) == null) {
                log.info("Waiting for SSL password callback initialization");
                semaphore.acquire();
                semaphore.release();
            }
            return context;
        }

        @Override
        public final void init(char[] keyPassword, char[] keyStorePassword, char[] trustStorePassword)
        throws IOException, GeneralSecurityException {
            if (sslContext.get() != null) throw new GeneralSecurityException("Context already initialized");

            final SSLContext sslContext = SSLContext.getInstance("TLS");

            /* We must have a keyStore if we're being called */
            final File keyStoreFile = configurations.getFile("keyStore.file");
            if (keyStoreFile == null) throw new IllegalStateException("KeyStore file not configured");

            /* Load our key manager key store */
            final KeyStore keyStore = getKeyStore(configurations.get("keyStore.type"),
                                                  configurations.get("keyStore.provider"),
                                                  configurations.getFile("keyStore.file"),
                                                  keyStorePassword);

            /* Instantiate a key manager factory */
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword);

            /* See if we *also* need a trust manager */
            final TrustManagerFactory trustManagerFactory;
            if (configurations.containsKey("trustStore.file")) {
                final KeyStore trustStore = getKeyStore(configurations.get("trustStore.type"),
                                                        configurations.get("trustStore.provider"),
                                                        configurations.getFile("trustStore.file"),
                                                        trustStorePassword);
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
            } else {
                trustManagerFactory = null;
            }

            /* Finally initialize our context */
            sslContext.init(keyManagerFactory.getKeyManagers(),
                            trustManagerFactory == null ? null :
                                trustManagerFactory.getTrustManagers(),
                            null);

            /* Save the initialized SSL context, and release the semaphore */
            this.sslContext.set(sslContext);
            semaphore.release(Integer.MAX_VALUE);
        }

        private KeyStore getKeyStore(String type, String provider, File file, char[] password)
        throws IOException, GeneralSecurityException {

            if (type == null) type = KeyStore.getDefaultType();
            final KeyStore keyStore = provider == null ?
                                          KeyStore.getInstance(type) :
                                          KeyStore.getInstance(type, provider);

            keyStore.load(new FileInputStream(file.getAbsolutePath()), password);
            return keyStore;
        }
    }
}
