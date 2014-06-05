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

import javax.inject.Singleton;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.inject.ConfigurableProvider;

@Singleton
public class HttpServerProvider extends ConfigurableProvider<HttpServer> {

    private final Log log = new Log();

    private ErrorPageGenerator defaultErrorPageGenerator;
    private HttpServer server;

    public HttpServerProvider() {
        super(HttpServerConfigurations.class);
    }

    @Override
    protected HttpServer get(Configurations configurations) {
        if (server != null) return server;

        final HttpServer server = new HttpServer();

        /* Get our configurations */
        final ServerConfiguration configuration = server.getServerConfiguration();
        configuration.setName(configurations.get("name", "default"));

        /* If we have a default error page generator ... */
        if (defaultErrorPageGenerator != null)
            configuration.setDefaultErrorPageGenerator(defaultErrorPageGenerator);

        /* Sensible defaults */
        configuration.setHttpServerName   (configurations.get("server",  "Grizzly"));
        configuration.setHttpServerVersion(configurations.get("version",  Grizzly.getDotedVersion()));
        configuration.setSendFileEnabled  (configurations.get("sendfile", true));

        /* Even more sensible defaults (non-configurable) */
        configuration.setPassTraceRequest(false);
        configuration.setTraceEnabled(false);

        /* Log something */
        log.info("Created server %s/%s with name \"%s\"",
                        configuration.getHttpServerName(),
                        configuration.getHttpServerVersion(),
                        configuration.getName());

        /* Done */
        return this.server = server;
    }
}
