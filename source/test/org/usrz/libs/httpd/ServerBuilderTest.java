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

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class ServerBuilderTest extends AbstractTest {

    @Test
    public void testServerBuilder()
    throws Exception {
        final File accessLog = IO.makeTempFile();
        final File documentRoot = IO.makeTempDir();
        final File keystoreFile1 = IO.copyTempFile("certificate1.pem"); // password "qwer"
        final File keystoreFile2 = IO.copyTempFile("certificate2.pem"); // password "asdf"
        IO.copy("index.html", new File(documentRoot, "index.html"));

        final int port1 = NET.serverPort();
        final int port2 = NET.serverPort();
        final int port3 = NET.serverPort();

        final Configurations configurations = new ConfigurationsBuilder()
            .put("server.name", "myServer123")

            .put("server.listener.host", "127.0.0.1")
            .put("server.listener.port", port1)
            .put("server.listener.secure", false)

            .put("server.listeners.0.host", "127.0.0.1")
            .put("server.listeners.0.port", port2)
            .put("server.listeners.0.secure", true)
            .put("server.listeners.0.keystore.file", keystoreFile1)
            .put("server.listeners.0.keystore.password", "qwer")

            .put("server.listeners.1.host", "127.0.0.1")
            .put("server.listeners.1.port", port3)
            .put("server.listeners.1.secure", true)
            .put("server.listeners.1.keystore.file", keystoreFile2)
            .put("server.listeners.1.keystore.password", "asdf")

            .put("server.access_log.file", accessLog)
            .put("server.access_log.synchronous", true) // for tests, easy
            .put("server.access_log.format", "%A:%p %r") // only GET /foo HTTP/1.1
            .put("server.document_root", documentRoot)
            .put("server.json.use_timestamps", true)
            .put("server.json.field_naming", "underscores")
            .put("server.json.order_keys", true) // sanity for tests belok
            .build();

        final Configurations json2 = new ConfigurationsBuilder()
            .put("use_timestamps", false)
            .put("field_naming", "camel_case")
            .put("order_keys", true)
            .build();

        final Configurations json3 = new ConfigurationsBuilder()
            .put("indent", true)
            .put("use_timestamps", false)
            .put("field_naming", "pascal")
            .put("order_keys", true)
            .build();

        final Map<String, Integer> dependency = new HashMap<String, Integer>();
        dependency.put("foo", 123);
        dependency.put("bar", 321);

        final ServerStarter starter = new ServerStarter().start((builder) -> {

            /* Configure with defaults above */
            builder.configure(configurations.strip("server"));

            /* Serve /rest1 with undescores & timestamps */
            builder.serveApp("/rest1", (config) -> {
                config.setApplicationName("testApp-1");
                config.register(TestResource.class);
            }).withAppConfigurations(new ConfigurationsBuilder().put("conf", "config1").build());

            /* Serve /rest2 with camel case and dates */
            builder.serveApp("/rest2", (config) -> {
                config.register(TestResource.class);
            }).withAppConfigurations(new ConfigurationsBuilder().put("conf", "config2").build())
              .withObjectMapperConfigurations(json2);


            /* Serve /rest3 with camel case dates and indent! */
            builder.serveApp("/rest3", new TestApplication())
              .withAppConfigurations(new ConfigurationsBuilder().put("conf", "config3").build())
              .withObjectMapperConfigurations(json3);

            /* Remember to inject our dependency for TestResource */
            builder.install((binder) ->
                binder.bind(new TypeLiteral<Map<String, Integer>>(){})
                      .annotatedWith(Names.named("foobar"))
                      .toInstance(dependency));
        });

        /* Hairy code, trust ourselves for SSL certificate validation */
        final KeyStore keyStore = KeyStore.getInstance("PEM");
        keyStore.load(IO.resource("certificate1.pem"), "qwer".toCharArray());
        final Certificate certificate = keyStore.getCertificate("F7A4FD46266A272B145B4F09F6D14CC7A458268B");
        assertNotNull(certificate, "Unable to find our own certificate???");

        /* Trust manager trusting only ourselves... */
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    throws CertificateException {
                        throw new CertificateException("NO");
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    throws CertificateException {
                        if (certificate.equals(certs[0])) return;
                        throw new CertificateException("Invalid server cert: " + certs[0].getSubjectDN());
                    }
                }
            }, new SecureRandom());

        /* Install the security context with our trust manager */
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        /* Verify host names */
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                try {
                    if (certificate.equals(session.getPeerCertificates()[0])) {
                        if ("127.0.0.1".equals(hostname)) return true;

                        log.error("Wrong hostname: expected \"127.0.0.1\", got \"%s\"", hostname);
                        return false;
                    } else {
                        log.error("Wrong certificate: got \"%s\"", session.getPeerPrincipal());
                        return false;
                    }
                } catch (Exception exception) {
                    log.error("Exception validating host", exception);
                    return false;
                }
            }
        });

        /* Connect to the client and run tests */
        try {
            /* Check our server name */
            assertEquals(starter.server().getServerConfiguration().getName(), "myServer123");

            /* Read our INDEX.HTML and compare for equality */
            byte[] index = IO.read("index.html");
            log.info("Executing 300 static requests");
            for (int x = 0; x < 100; x ++) {
                byte[] index1 = IO.read(new URL("http://127.0.0.1:" + port1 + "/index.html"));
                byte[] index2 = IO.read(new URL("https://127.0.0.1:" + port2 + "/index.html"));
                byte[] index3 = IO.read(new URL("https://127.0.0.1:" + port3 + "/index.html"));
                assertEquals(index1, index, "Wrong data received from port " + port1);
                assertEquals(index2, index, "Wrong data received from port " + port2);
                assertEquals(index3, index, "Wrong data received from SSL port " + port3);
            }

            /* Reaad our REST values and check them */
            log.info("Executing 300 Jersey requests");
            for (int x = 0; x < 100; x ++) {
                final String string1 = new String(IO.read(new URL("http://127.0.0.1:" + port1 + "/rest1/")));
                final String string2 = new String(IO.read(new URL("https://127.0.0.1:" + port2 + "/rest2/")));
                final String string3 = new String(IO.read(new URL("https://127.0.0.1:" + port3 + "/rest3/")));
                assertEquals(string1, "{\"config\":\"config1\",\"depended_map\":{\"bar\":321,\"foo\":123},\"epoch_date\":0}");
                assertEquals(string2, "{\"config\":\"config2\",\"dependedMap\":{\"bar\":321,\"foo\":123},\"epochDate\":\"1970-01-01T00:00:00.000+0000\"}");
                assertEquals(string3, "{\n  \"Config\" : \"config3\",\n  \"DependedMap\" : {\n    \"bar\" : 321,\n    \"foo\" : 123\n  },\n  \"EpochDate\" : \"1970-01-01T00:00:00.000+0000\"\n}");
            }
        } finally {
            /* Stop the server */
            starter.stop();
        }

        /* Check the access log after a second... */
        Thread.sleep(1000);
        final String access = new String(IO.read(accessLog));

        /*
         * We *might* have entries not in the correct order, as we're too
         * fast making them and those are written by Jersey's threads (which
         * are all over the place). Sort'em!
         */
        final Set<String> parsed = new TreeSet<>();
        final StringTokenizer tokenizer = new StringTokenizer(access, "\r\n");
        while (tokenizer.hasMoreTokens()) parsed.add(tokenizer.nextToken());

        final Set<String> expected = new TreeSet<>();
        expected.add("127.0.0.1:" + port1 + " GET /index.html HTTP/1.1");
        expected.add("127.0.0.1:" + port2 + " GET /index.html HTTP/1.1");
        expected.add("127.0.0.1:" + port3 + " GET /index.html HTTP/1.1");
        expected.add("127.0.0.1:" + port1 + " GET /rest1/ HTTP/1.1");
        expected.add("127.0.0.1:" + port2 + " GET /rest2/ HTTP/1.1");
        expected.add("127.0.0.1:" + port3 + " GET /rest3/ HTTP/1.1");

        assertEquals(parsed.size(), 6);
        assertEquals(parsed, expected);

    }
}
