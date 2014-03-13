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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

import org.usrz.libs.crypto.cert.X500PrincipalBuilder;
import org.usrz.libs.crypto.cert.X509CertificateBuilder;
import org.usrz.libs.crypto.cert.X509CertificateBuilder.Mode;
import org.usrz.libs.crypto.utils.KeyPairBuilder;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.configurations.Configurations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SelfSignedSSLContextProvider implements Provider<SSLContext> {

    private final SSLContext sslContext;

    @Inject
    protected SelfSignedSSLContextProvider(Configurations configurations)
    throws IOException, GeneralSecurityException {
        final String country             = configurations.get("certificate.country",            "JP");
        final String state               = configurations.get("certificate.state",              "Tokyo");
        final String locality            = configurations.get("certificate.locality",           "Shinjuku");
        final String organisation        = configurations.get("certificate.organisation",       "USRZ.org");
        final String organisationalUnit  = configurations.get("certificate.organisationalUnit", "Self-Signed Certificates");
        final String commonName          = configurations.get("certificate.commonName");   //shall not be null
        final String emailAddress        = configurations.get("certificate.emailAddress"); //can be null

        /* Verify that we have *at least* the Common Name */
        if (commonName == null) throw new IllegalStateException("Self-signed certificate's common name not set");

        /* Build our X.500 principal */
        final X500PrincipalBuilder principalBuilder = new X500PrincipalBuilder()
                                            .country(country)
                                            .state(state)
                                            .locality(locality)
                                            .organisation(organisation)
                                            .organisationalUnit(organisationalUnit)
                                            .commonName(commonName);
        if (emailAddress != null) principalBuilder.emailAddress(emailAddress);
        final X500Principal principal = principalBuilder.build();

        /* What's in our certificate */
        final KeyPair keyPair = new KeyPairBuilder().keySize(2048).build();
        final X509CertificateBuilder certificateBuilder = new X509CertificateBuilder()
                                            .selfSigned(principal, keyPair)
                                            .mode(Mode.SERVER);

        /* Alternative names */
        for (Configurations alternativeName: configurations.group("certificate.alternativeName.dns").values())
            certificateBuilder.alternativeNameDNS(alternativeName.get(null));
        for (Configurations alternativeName: configurations.group("certificate.alternativeName.ip").values())
            certificateBuilder.alternativeNameDNS(alternativeName.get(null));
        for (Configurations alternativeName: configurations.group("certificate.alternativeName.email").values())
            certificateBuilder.alternativeNameDNS(alternativeName.get(null));

        /* Build our certificate */
        final X509Certificate certificate = certificateBuilder.build();

        /* Create a random 128-characters (256-bytes) password */
        final SecureRandom random = new SecureRandom();
        final char[] password = new char[128];
        for (int x = 0; x < password.length; x ++)
            password[x] = (char) random.nextInt(0x010000);

        /* Create a new JKS key store, and initialize it empty */
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, password);

        /* Put our key and certificate in the key store */
        keyStore.setKeyEntry("self-signed",
                             keyPair.getPrivate(),
                             password,
                             new Certificate[] { certificate });

        /* Create our key manager */
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        /* Finally create, initialize, and save our context */
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, random);
        this.sslContext = sslContext;

        /* Log what we've done */
        new Log().info("Created SSL context with self-signed certificate %s", principal);
    }

    @Override
    public SSLContext get() {
        return sslContext;
    }

}
